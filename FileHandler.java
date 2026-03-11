import java.io.*;
import java.util.*;

public class FileHandler {

    static final String FILE_NAME = "tasks.txt";

    public static void saveTasks(ArrayList<Task> tasks) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Task t : tasks) {
                writer.println(t.toFileString());
            }
        } catch (Exception e) {
            System.out.println("Error saving tasks: " + e.getMessage());
        }
    }

    public static ArrayList<Task> loadTasks() {
        ArrayList<Task> tasks = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return tasks;

        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                // New pipe-delimited format
                if (line.contains("|")) {
                    String[] parts = line.split("\\|(?!\\\\)", -1);
                    if (parts.length >= 6) {
                        tasks.add(new Task(
                            unescape(parts[0]),
                            unescape(parts[1]),
                            unescape(parts[2]),
                            unescape(parts[3]),
                            unescape(parts[4]),
                            Boolean.parseBoolean(parts[5])
                        ));
                    }
                } else {
                    // Legacy comma format
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 4) {
                        tasks.add(new Task(
                            parts[0], parts[1], parts[2],
                            Boolean.parseBoolean(parts[3])
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }
        return tasks;
    }

    private static String unescape(String s) {
        return s == null ? "" : s.replace("\\|", "|").trim();
    }
}