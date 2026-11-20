package org.example.Creations;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports work order IDs to CSV files, one order at a time.
 * <p>
 * Every file starts with "username" and "password" columns, taken in turn from USERS (read from users.xlsx)
 * (the first row of each file gets the first user, the next row the next user, and so on, starting over at the end).
 * Then comes a "wo_id" column with one ID per row. Some files have one extra column:
 * Opened Page has an empty "status" column,
 * Assign has an "organisation" column that is always "MBKGZ".
 * The first order written to a file in a run clears the file from the previous run,
 * so use one Export object per run.
 */
public class Export
{
    /** Excel file with the users, in the folder the program is run from (next to db_config.json). */
    static final String USERS_FILE = "users.xlsx";

    static {
        // POI logs through Log4j; without this it prints a "could not find a logging implementation" error on every run
        if (System.getProperty("log4j2.loggerContextFactory") == null) {
            System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
        }
    }

    // Users as {username, password, user ID}, read from the first sheet of USERS_FILE:
    // row 1 is the header (username, password, user_id), every row after it is one user.
    // Username and password are written to the files; My Tasks orders are assigned to the user ID of the user on their row.
    static final String[][] USERS = load_users(USERS_FILE);

    /** Reads the users from the first sheet of the Excel file, skipping the header row and empty rows. */
    static String[][] load_users(String file_name) {
        Path path = Path.of(file_name);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Users file not found: " + path.toAbsolutePath());
        }
        DataFormatter formatter = new DataFormatter();   // reads a number cell like 126337 as "126337", not "126337.0"
        List<String[]> users = new ArrayList<String[]>();
        try (InputStream in = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;   // header
                }
                String username = formatter.formatCellValue(row.getCell(0)).trim();
                String password = formatter.formatCellValue(row.getCell(1)).trim();
                String user_id = formatter.formatCellValue(row.getCell(2)).trim();
                if (username.isEmpty() && password.isEmpty() && user_id.isEmpty()) {
                    continue;   // empty row
                }
                users.add(new String[]{username, password, user_id});
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read users file " + path.toAbsolutePath() + ": " + e.getMessage(), e);
        }
        if (users.isEmpty()) {
            throw new IllegalStateException("No users found in " + path.toAbsolutePath());
        }
        return users.toArray(new String[0][]);
    }

    /** Stops the run before anything is sent if a user in USERS has no user ID. */
    public static void check_user_ids() {
        for (String[] user : USERS) {
            if (user.length < 3 || user[2].isBlank()) {
                throw new IllegalStateException("Fill in the user ID of " + user[0] + " in " + USERS_FILE + " before running");
            }
        }
    }

    private static final String HEADER = "username,password,wo_id";
    private static final String ORGANISATION = "MBKGZ";

    // Rows written so far to each file in this run; a file not in here has not been started yet
    private final Map<String, Integer> rows_written = new HashMap<String, Integer>();

    public void Zone_Tasks_work_orders(String work_order) throws IOException { write_order("Zone_Tasks_work_orders.csv", work_order); }
    public void Assign_work_orders(String work_order) throws IOException { write_order("Assign_work_orders.csv", work_order, "organisation", ORGANISATION); }
    public void Re_assigen_work_orders(String work_order) throws IOException { write_order("Re_assigen_work_orders.csv", work_order); }
    /** Returns the user ID of the user written on the order's row. */
    public String My_tasks_work_orders(String work_order) throws IOException { return write_order("My_tasks_work_orders.csv", work_order); }
    public void Opened_Page_work_orders(String work_order) throws IOException { write_order("Opened_Page_work_orders.csv", work_order, "status", ""); }
    public void Closed_Page_work_orders(String work_order) throws IOException { write_order("Closed_Page_work_orders.csv", work_order); }

    /** Adds one work order ID as a new line, with no extra column. Returns the user ID of the user written on the line. */
    private String write_order(String file_name, String work_order) throws IOException {
        return write_line(file_name, HEADER, work_order);
    }

    /** Adds one work order ID as a new line, followed by an extra column with the given value (can be empty). Returns the user ID of the user written on the line. */
    private String write_order(String file_name, String work_order, String column, String value) throws IOException {
        return write_line(file_name, HEADER + "," + column, work_order + "," + value);
    }

    /**
     * Adds one line to the file, starting with the next user's username and password.
     * On the first call for a file in this run, the old file is overwritten and the header written first.
     * Returns the user ID of the user written on the line.
     */
    private String write_line(String file_name, String header, String line) throws IOException {
        Path path = Path.of(file_name);
        int row = rows_written.getOrDefault(file_name, 0);
        rows_written.put(file_name, row + 1);
        String[] user = USERS[row % USERS.length];
        String full_line = user[0] + "," + user[1] + "," + line;
        if (row == 0) {
            Files.write(path, List.of(header, full_line), StandardCharsets.UTF_8);
        } else {
            Files.write(path, List.of(full_line), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
        return user[2];
    }
}
