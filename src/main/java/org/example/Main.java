package org.example;

import org.example.Creations.Create;
import org.example.Creations.Export;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;


public class Main {
    // Used when the user presses Enter without typing a path
    private static final String DEFAULT_CONFIG = "db_config.json";

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        // There are 6 order types, so 12 or more orders covers every scenario
        System.out.println("Tip: set number_of_orders to 12 or more to cover all scenarios (there are 6 order types).");

        String config_path = args.length > 0 ? args[0] : ask(scanner, "Enter config file path [" + DEFAULT_CONFIG + "]: ");
        if (config_path.isEmpty()) {
            config_path = DEFAULT_CONFIG;
        }

        Path path = Path.of(config_path);
        if (!Files.exists(path)) {
            System.out.println("Config file not found: " + path.toAbsolutePath());
            return;
        }
        JSONObject config = new JSONObject(Files.readString(path, StandardCharsets.UTF_8));

        String ip = config.getString("app_ip");
        String port = String.valueOf(config.get("app_port"));   // accepts 8080 or "8080"
        Integer numberOfOrders = parse_positive_int(String.valueOf(config.get("number_of_orders")));
        if (numberOfOrders == null) {
            System.out.println("Invalid number_of_orders in " + config_path + ": " + config.get("number_of_orders"));
            return;
        }

        // Stops here, before anything is sent, if a user in users.xlsx has no user ID
        Export.check_user_ids();

        System.out.println("Sending " + numberOfOrders + " orders to " + ip + ":" + port);
        Create run = new Create(ip, port);
        try {
            ArrayList<String> workOrders = run.genrate_work_orders(numberOfOrders);
           // System.out.println("Done. Created " + workOrders.size() + " work orders.");
            run.export_work_orders(workOrders);
            System.out.println("Exported work orders to CSV files.");
            // export_work_orders has already waited for Partial Assign
            run.call_my_tasks_function();
        } finally {
            // Runs even if something above fails, so the quota never stays raised
            try {
                run.restore_quota();
            } finally {
                // The single database connection is closed only here, at the very end
                run.close_database();
            }
        }
    }

    /** Prints the prompt and returns the trimmed line the user types. */
    private static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Returns the number if the text is a whole number greater than 0, otherwise null. */
    static Integer parse_positive_int(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
