package org.example.Creations;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Opens a database connection using the settings in db_config.json.
 * <p>
 * The file is read from the working directory (the same relative path as the exported CSV files)
 * and must contain: jdbc, ip, port, service_name, user, password.
 * The URL is built as: {jdbc}:@//{ip}:{port}/{service_name}
 * e.g. jdbc:oracle:thin:@//10.0.0.5:1521/ORCLPDB
 */
public class Database implements AutoCloseable
{
    private static final String CONFIG_FILE = "db_config.json";

    // Longest time to wait for the database to connect or answer one query
    private static final int TIMEOUT_SECONDS = 30;

    // Column in wf_work_order that holds the work order number returned when an order is created
    private static final String WO_ID_COLUMN = "work_order_id";

    private final String url;
    private final String user;
    private final String password;

    // The one connection the whole run shares; opened on first use and closed by close()
    private Connection connection;

    // Oracle error codes that mean the database has no free sessions/processes left
    private static final int[] TOO_MANY_CONNECTIONS_CODES = {18, 20, 12516, 12519, 12520};

    // The WO_STAGE and WO_STATUS a work order must have to count as scheduled
    private static final String WO_STAGE = "Schedule";
    private static final String WO_STATUS = "Scheduled";

    // The WO_STAGE and WO_STATUS an FTTHNewSubVS order reaches once it has taken its quota
    private static final String ASSIGNED_STAGE = "Assign";
    private static final String ASSIGNED_STATUS = "Partial Assign";

    // Database function called once for every My Tasks work order, e.g. "MY_PKG.MY_FUNCTION". Fill in before running.
    private static final String MY_TASKS_FUNCTION = "ASSIGN_TASK_MULTIPLE";

    // SELECT that returns the one function input that comes from the database (first column of the first row).
    // If it has a ?, the work order ID is put there, so it runs once per work order. No ";" at the end.
    private static final String MY_TASKS_SELECT = "select WORK_ID from wf_work_order_item where work_order_id = ? order by WORK_ID desc FETCH FIRST 1 ROW ONLY";

    // SELECT that returns already closed work order IDs for Closed_Page_work_orders.csv (first column). Fill in before running.
    // If it has a ?, the number of orders the file needs is put there, e.g. "... FETCH FIRST ? ROWS ONLY". No ";" at the end.
    private static final String CLOSED_SELECT = "select work_order_id from wf_work_order where WO_STAGE = 'Close' and org_role_name in ('MBKGZ','MR2GZ')and request_type in ('A/VReSplitReserve','ADSL Un-Splitting','ADSL/VDSL Splitting','Data down','FTTHDataandVoiceDown','FTTHMigrationSurvey','FTTHNewSubVS','FTTHWrongCardPort','Physical Instability')";

    // Fixed inputs of MY_TASKS_FUNCTION; the full order is set in call_my_tasks_function
    private static final String MY_TASKS_ASSIGNED_BY = "root";
    private static final String MY_TASKS_MODE = "MULTIPLE";

    // Tomorrow 16:00 quota for the MBKGZ zone of compound 676, read from the database in the constructor
    private String quota_id;
    private int quota;
    private int norm_quota;
    private int used_quota;
    private int used_normal_quota;

    // True while QUOTA / NORM_QUOTA / USED_QUOTA / USED_NORMAL_QUOTA hold the raised amount and still need to be put back
    private volatile boolean quota_raised = false;
    private boolean rollback_on_exit_added = false;

    public Database() throws IOException, SQLException { this(CONFIG_FILE); }

    public Database(String config_file) throws IOException, SQLException {
        Path path = Path.of(config_file);
        if (!Files.exists(path)) {
            throw new IOException("Database config not found: " + path.toAbsolutePath());
        }
        JSONObject config = new JSONObject(Files.readString(path, StandardCharsets.UTF_8));

        String jdbc = config.getString("jdbc");
        String ip = config.getString("ip");
        String port = String.valueOf(config.get("port"));   // accepts 1521 or "1521"
        String service_name = config.getString("service_name");

        this.url = jdbc + ":@//" + ip + ":" + port + "/" + service_name;
        this.user = config.getString("user");

        this.password = config.getString("password");

        //System.out.println("Connecting to the database and reading the zone quota...");
        select_quota();
    }

    /**
     * Reads ID, QUOTA, NORM_QUOTA, USED_QUOTA and USED_NORMAL_QUOTA of the 16:00 interval for the MBKGZ zone of compound 676
     * and saves them in quota_id, quota, norm_quota, used_quota and used_normal_quota.
     */
    private void select_quota() throws SQLException {
        // No ";" at the end: JDBC sends one statement and Oracle rejects a trailing semicolon
        String sql = "SELECT ID, QUOTA, NORM_QUOTA, USED_QUOTA, USED_NORMAL_QUOTA"
                + " FROM wf_org_role_interval_zone_quota"
                + " WHERE INTERVAL_TO = TRUNC(SYSDATE) + 1 + (16/24)"
                + "   AND zone_id IN ("
                + "       SELECT zone_id"
                + "       FROM lc_place_demographic"
                + "       WHERE nw_element_value = '676'"
                + "         AND NW_ELEMENT_TYPE = 'COMPOUND'"
                + "         AND ORG_ROLE_ID = 'MBKGZ'"
                   + "   )";
        try (PreparedStatement statement = prepare(connection(), sql, new ArrayList<>());
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("No quota found for tomorrow 16:00 (compound 676, MBKGZ)");
            }
            this.quota_id = result.getString("ID");
            this.quota = result.getInt("QUOTA");
            this.norm_quota = result.getInt("NORM_QUOTA");
            this.used_quota = result.getInt("USED_QUOTA");
            this.used_normal_quota = result.getInt("USED_NORMAL_QUOTA");
        }
    }

    public String get_quota_id() { return quota_id; }

    public int get_quota() { return quota; }

    public int get_norm_quota() { return norm_quota; }

    public int get_used_quota() { return used_quota; }

    public int get_used_normal_quota() { return used_normal_quota; }

    /**
     * Sets QUOTA and NORM_QUOTA of the saved quota_id to the given amount and resets USED_QUOTA and USED_NORMAL_QUOTA to 0.
     * The old amounts stay saved in quota, norm_quota, used_quota and used_normal_quota for rollback_quota().
     * If the program is closed (Ctrl+C, closing the window) before rollback_quota() runs,
     * the old amounts are still put back on exit.
     */
    public void raise_quota(int amount) throws SQLException {
        if (!rollback_on_exit_added) {
            rollback_on_exit_added = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    rollback_quota();
                } catch (SQLException e) {
                    System.out.println("Could not restore the zone quota (ID " + quota_id + ", QUOTA " + quota
                            + ", NORM_QUOTA " + norm_quota + ", USED_QUOTA " + used_quota
                            + ", USED_NORMAL_QUOTA " + used_normal_quota + "): " + e.getMessage());
                } finally {
                    close();
                }
            }));
        }
        quota_raised = true;
        set_quota(amount, amount, 0, 0);
    }

    /**
     * Puts QUOTA, NORM_QUOTA, USED_QUOTA and USED_NORMAL_QUOTA of the saved quota_id back to the old amounts read in the constructor.
     * Does nothing if the quota is not raised, so calling it twice is safe.
     */
    public synchronized void rollback_quota() throws SQLException {
        if (!quota_raised) {
            return;
        }
        set_quota(quota, norm_quota, used_quota, used_normal_quota);
        quota_raised = false;
    }

    private void set_quota(int quota_amount, int norm_quota_amount, int used_quota_amount, int used_normal_quota_amount) throws SQLException {
        String sql = "UPDATE wf_org_role_interval_zone_quota SET QUOTA = ?, NORM_QUOTA = ?, USED_QUOTA = ?, USED_NORMAL_QUOTA = ? WHERE ID = ?";
        ArrayList<Object> values = new ArrayList<>();
        values.add(quota_amount);
        values.add(norm_quota_amount);
        values.add(used_quota_amount);
        values.add(used_normal_quota_amount);
        values.add(quota_id);
        run_update(sql, values);
    }

    /**
     * Returns the shared connection, opening it the first time (or again if it was lost).
     * Do not close the returned connection; close() closes it once at the end of the run.
     */
    private synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(TIMEOUT_SECONDS)) {
            close();
            connection = connect();
        }
        return connection;
    }

    /** Opens a new connection, with a clear message if the database has no free sessions left. */
    private Connection connect() throws SQLException {
        DriverManager.setLoginTimeout(TIMEOUT_SECONDS);
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            if (is_too_many_connections(e)) {
                throw new SQLException("The database refused the connection because it has too many open sessions"
                        + " (ORA-" + String.format("%05d", e.getErrorCode()) + "). Close other sessions to " + url
                        + " (SQL Developer, other test runs) or ask the DBA to raise the limit, then try again.", e);
            }
            throw e;
        }
    }

    private static boolean is_too_many_connections(SQLException e) {
        for (int code : TOO_MANY_CONNECTIONS_CODES) {
            if (e.getErrorCode() == code) {
                return true;
            }
        }
        return false;
    }

    /** Closes the shared connection. Safe to call more than once. */
    @Override
    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println("Could not close the database connection: " + e.getMessage());
        }
        connection = null;
    }

    /**
     * Runs an UPDATE. If it waits longer than TIMEOUT_SECONDS it stops with a clear error instead of hanging:
     * that almost always means the rows are locked by another session (an UPDATE in SQL Developer without COMMIT).
     */
    private void run_update(String sql, List<Object> values) throws SQLException {
        try (PreparedStatement statement = prepare(connection(), sql, values)) {
            statement.executeUpdate();
        } catch (SQLTimeoutException e) {
            throw new SQLTimeoutException("UPDATE waited more than " + TIMEOUT_SECONDS + "s. The rows are probably locked by another session"
                    + " (e.g. an UPDATE in SQL Developer without COMMIT). Run COMMIT or ROLLBACK there and try again. SQL: " + sql, e);
        }
    }

    /**
     * Sets SCHEDULING_TYPE to MANUAL for the test request types in MBKGZ, and to AUTOMATIC for FTTHNewSubVS.
     */
    public void update() throws SQLException {
        String sql = "update bs_cfg_req_type_org set SCHEDULING_TYPE ="
                + " CASE WHEN REQUEST_TYPE = 'FTTHNewSubVS' THEN 'Automatic' ELSE 'MANUAL' END"
                + " WHERE REQUEST_TYPE IN ('A/VReSplitReserve','ADSL Un-Splitting','Physical Instability','Data down','FTTHMigrationSurvey','ADSL Change to VDSL','FTTHNewSubVS') AND wf_org_role = 'MBKGZ'";
        run_update(sql, new ArrayList<>());
    }

    /**
     * Keeps looping over the given work orders and only stops (breaks the loop) when all of them
     * have WO_STAGE = WO_STAGE and WO_STATUS = WO_STATUS. There is no time limit.
     * Shows an animated loading screen with how many are SCHEDULE so far.
     */
    public void wait_until_scheduled(ArrayList<String> work_orders) throws SQLException, InterruptedException {
        wait_until(work_orders, WO_STAGE, WO_STATUS, "SCHEDULE");
    }

    /**
     * Same as wait_until_scheduled, but waits for WO_STAGE = ASSIGNED_STAGE and WO_STATUS = ASSIGNED_STATUS,
     * which FTTHNewSubVS orders reach once they have taken their quota.
     */
    public void wait_until_assigned(ArrayList<String> work_orders) throws SQLException, InterruptedException {
        wait_until(work_orders, ASSIGNED_STAGE, ASSIGNED_STATUS, "Partial Assign");
    }

    /**
     * Keeps looping over the given work orders until all of them have the given WO_STAGE and WO_STATUS.
     * There is no time limit. Shows an animated loading screen with how many have reached it so far.
     */
    private void wait_until(ArrayList<String> work_orders, String stage, String status, String label) throws SQLException, InterruptedException {
        Loading loading = new Loading("Waiting for work orders to reach " + label, work_orders.size());
        loading.start();
        try {
            // Counts the matching rows instead of using not_exists(): NOT EXISTS is also true while
            // the work orders are not in wf_work_order yet, which ended the wait straight away
            int reached = count_in_state(work_orders, stage, status);
            while (reached < work_orders.size()) {
                loading.set_done(reached);
                // Check every 30 seconds so the loop does not flood the database with queries
                TimeUnit.SECONDS.sleep(30);
                reached = count_in_state(work_orders, stage, status);
            }
            loading.set_done(work_orders.size());
            loading.finish(true, "All " + work_orders.size() + " work orders are " + label);
        } catch (SQLException | InterruptedException e) {
            loading.finish(false, "Stopped: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Calls MY_TASKS_FUNCTION once for every given work order with the inputs
     * (the order's user ID, WORK_ID from MY_TASKS_SELECT, today's date, MY_TASKS_ASSIGNED_BY, MY_TASKS_MODE).
     * work_orders maps each work order ID to the user ID it is assigned to.
     * Stops at the first work order that fails, naming it in the error.
     */
    public void call_my_tasks_function(Map<String, String> work_orders) throws SQLException {
        if (MY_TASKS_FUNCTION.isEmpty() || MY_TASKS_SELECT.isEmpty()) {
            throw new SQLException("Set MY_TASKS_FUNCTION and MY_TASKS_SELECT in Database.java before running");
        }
        // Without a ? the SELECT does not depend on the work order, so it is run only once
        boolean per_work_order = MY_TASKS_SELECT.contains("?");
        Object shared_value = per_work_order ? null : select_one_value(MY_TASKS_SELECT, new ArrayList<>());

        Loading loading = new Loading("Calling " + MY_TASKS_FUNCTION + " on My Tasks work orders", work_orders.size());
        loading.start();
        int done = 0;
        try {
            for (Map.Entry<String, String> entry : work_orders.entrySet()) {
                String work_order = entry.getKey();
                Object work_id = per_work_order ? select_one_value(MY_TASKS_SELECT, List.of(work_order)) : shared_value;
                // Today's system date (no time), sent as a real DATE so the session's NLS_DATE_FORMAT does not matter
                java.sql.Date today = java.sql.Date.valueOf(java.time.LocalDate.now());
                Object[] params = {entry.getValue(), work_id, today, MY_TASKS_ASSIGNED_BY, MY_TASKS_MODE};
                try {
                    call_function(MY_TASKS_FUNCTION, params);
                } catch (SQLException e) {
                    throw new SQLException("Work order " + work_order + ": " + e.getMessage(), e);
                }
                loading.set_done(++done);
            }
            loading.finish(true, "Called " + MY_TASKS_FUNCTION + " on all " + work_orders.size() + " My Tasks work orders");
        } catch (SQLException e) {
            loading.finish(false, "Stopped after " + done + " work orders: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Returns count already closed work order IDs from CLOSED_SELECT (its first column).
     * Rows after the first count are ignored. If there are fewer than count, prints a tip with both numbers
     * and returns the ones found.
     */
    public ArrayList<String> select_closed_work_orders(int count) throws SQLException {
        ArrayList<String> work_orders = new ArrayList<>();
        if (count == 0) {
            return work_orders;
        }
        if (CLOSED_SELECT.isEmpty()) {
            throw new SQLException("Set CLOSED_SELECT in Database.java before running");
        }
        List<Object> values = CLOSED_SELECT.contains("?") ? List.of(count) : List.of();
        try (PreparedStatement statement = prepare(connection(), CLOSED_SELECT, values)) {
            statement.setMaxRows(count);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    work_orders.add(result.getString(1));
                }
            }
        }
        // Not enough closed orders is not an error: the run goes on and Closed Page just gets the ones found
        if (work_orders.size() < count) {
            System.out.println("Tip: closed work orders are less than expected. Expected " + count
                    + ", found " + work_orders.size() + " in the database.");
        }
        return work_orders;
    }

    /** Runs the SELECT and returns the first column of its first row. */
    private Object select_one_value(String sql, List<Object> values) throws SQLException {
        try (PreparedStatement statement = prepare(connection(), sql, values);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("The SELECT returned no rows (inputs " + values + "): " + sql);
            }
            return result.getObject(1);
        }
    }

    /** Calls the database function with the given inputs and returns its result as text. */
    private String call_function(String function_name, Object[] params) throws SQLException {
        String placeholders = String.join(", ", java.util.Collections.nCopies(params.length, "?"));
        try (CallableStatement statement = connection().prepareCall("{? = call " + function_name + "(" + placeholders + ")}")) {
            statement.setQueryTimeout(TIMEOUT_SECONDS);
            statement.registerOutParameter(1, java.sql.Types.VARCHAR);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 2, params[i]);
            }
            statement.execute();
            return statement.getString(1);
        }
    }

    /** How many of the given work orders have WO_STAGE = WO_STAGE and WO_STATUS = WO_STATUS. */
    public int count_scheduled(ArrayList<String> work_orders) throws SQLException {
        return count_in_state(work_orders, WO_STAGE, WO_STATUS);
    }

    // Oracle allows at most 1000 values in one IN (...) list (ORA-01795), so bigger lists are checked in parts
    private static final int MAX_IN_LIST = 1000;

    /** How many of the given work orders have the given WO_STAGE and WO_STATUS (exact, case-sensitive match). */
    public int count_in_state(ArrayList<String> work_orders, String stage, String status) throws SQLException {
        int total = 0;
        for (int from = 0; from < work_orders.size(); from += MAX_IN_LIST) {
            int to = Math.min(from + MAX_IN_LIST, work_orders.size());
            total += count_part_in_state(new ArrayList<>(work_orders.subList(from, to)), stage, status);
        }
        return total;
    }

    /** count_in_state for at most MAX_IN_LIST work orders, in one query. */
    private int count_part_in_state(ArrayList<String> work_orders, String stage, String status) throws SQLException {
        if (work_orders.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(work_orders.size(), "?"));
        String sql = "SELECT COUNT(*) FROM wf_work_order"
                   + " WHERE " + WO_ID_COLUMN + " IN (" + placeholders + ")"
                   + "   AND WO_STAGE = ?"
                   + "   AND WO_STATUS = ?";
        ArrayList<Object> values = new ArrayList<>(work_orders);
        values.add(stage);
        values.add(status);
        try (PreparedStatement statement = prepare(connection(), sql, values);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    /**
     * Returns true if none of the given work orders has a WO_STAGE or WO_STATUS
     * other than WO_STAGE / WO_STATUS (or empty), i.e. all of them are scheduled.
     */
    public boolean not_exists(ArrayList<String> work_orders) throws SQLException {
        if (work_orders.isEmpty()) {
            return true;
        }
        // One ? per work order: (?, ?, ?, ...)
        String placeholders = String.join(", ", java.util.Collections.nCopies(work_orders.size(), "?"));
        String sql = "SELECT CASE"
                   + "         WHEN NOT EXISTS ("
                   + "             SELECT 1"
                   + "             FROM wf_work_order"
                   + "             WHERE " + WO_ID_COLUMN + " IN (" + placeholders + ")"
                   + "               AND (   WO_STAGE <> ?"
                   + "                    OR WO_STAGE IS NULL"
                   + "                    OR WO_STATUS <> ?"
                   + "                    OR WO_STATUS IS NULL)"
                   + "         )"
                   + "         THEN 1"
                   + "         ELSE 0"
                   + "       END AS result"
                   + " FROM dual";
        ArrayList<Object> values = with_stage_and_status(work_orders);
        try (PreparedStatement statement = prepare(connection(), sql, values);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt("result") == 1;
        }
    }

    /** Creates the statement and fills each ? with the matching value, in order. */
    private static PreparedStatement prepare(Connection conn, String sql, List<Object> values) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setQueryTimeout(TIMEOUT_SECONDS);   // never wait forever, e.g. on a locked row
        for (int i = 0; i < values.size(); i++) {
            statement.setObject(i + 1, values.get(i));
        }
        return statement;
    }

    /** The work order IDs (for the IN list) followed by WO_STAGE and WO_STATUS, matching the ? order in the queries. */
    private ArrayList<Object> with_stage_and_status(ArrayList<String> work_orders) {
        ArrayList<Object> values = new ArrayList<>(work_orders);
        values.add(WO_STAGE);
        values.add(WO_STATUS);
        return values;
    }
}
