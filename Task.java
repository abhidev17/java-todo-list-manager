public class Task {

    String description;
    String deadline;
    String priority;
    String category;
    String notes;
    boolean completed;

    public Task(String description, String deadline, String priority, String category, String notes, boolean completed) {
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.category = category;
        this.notes = notes;
        this.completed = completed;
    }

    // Backward-compatible constructor
    public Task(String description, String deadline, String priority, boolean completed) {
        this(description, deadline, priority, "General", "", completed);
    }

    public String toFileString() {
        return escapeCommas(description) + "|" +
               escapeCommas(deadline) + "|" +
               escapeCommas(priority) + "|" +
               escapeCommas(category) + "|" +
               escapeCommas(notes) + "|" +
               completed;
    }

    private String escapeCommas(String s) {
        return s == null ? "" : s.replace("|", "\\|");
    }

    @Override
    public String toString() {
        String status = completed ? "✓ Done" : "○ Pending";
        String priorityIcon = switch (priority) {
            case "High" -> "🔴";
            case "Medium" -> "🟡";
            case "Low" -> "🟢";
            default -> "⚪";
        };
        return priorityIcon + " [" + category + "] " + description +
               (deadline.isEmpty() ? "" : "  📅 " + deadline) +
               "  — " + status;
    }
}