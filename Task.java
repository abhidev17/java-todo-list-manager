public class Task {

    String description;
    String deadline;
    String deadlineTime;  // HH:mm format
    String priority;
    String category;
    String notes;
    String status;  // "To Do", "In Progress", "Done"
    boolean completed;  // Legacy field for compatibility

    public Task(String description, String deadline, String deadlineTime, String priority, String category, String notes, String status)     {
        this.description = description;
        this.deadline = deadline;
        this.deadlineTime = deadlineTime != null ? deadlineTime : "";
        this.priority = priority;
        this.category = category;
        this.notes = notes;
        this.status = status != null ? status : "To Do";
        this.completed = status.equals("Done");
    }

    // Backward-compatible constructors
    public Task(String description, String deadline, String priority, String category, String notes, boolean completed)   {
        this.description = description;
        this.deadline = deadline;
        this.deadlineTime = "";
        this.priority = priority;
        this.category = category;
        this.notes = notes;
        this.completed = completed;
        this.status = completed ? "Done" : "To Do";
    }

    public Task(String description, String deadline, String priority, boolean completed) {
        this(description, deadline, priority, "General", "", completed);
    }

    public String toFileString() {
        return escapeCommas(description) + "|" +
               escapeCommas(deadline) + "|" +
               escapeCommas(deadlineTime) + "|" +
               escapeCommas(priority) + "|" +
               escapeCommas(category) + "|" +
               escapeCommas(notes) + "|" +
               escapeCommas(status);
    }

    private String escapeCommas(String s) {
        return s == null ? "" : s.replace("|", "\\|");
    }

    @Override
    public String toString() {
        String icon = switch (status) {
            case "Done" -> "✓";
            case "In Progress" -> "⟳";
            default -> "○";
        };
         String priorityIcon = switch (priority) {
            case "High" -> "🔴";
            case "Medium" -> "🟡";
            case "Low" -> "🟢";
            default -> "⚪";
        };
        String result = priorityIcon + " [" + category + "] " + description;
        if (!deadline.isEmpty()) {
            result += "  📅 " + deadline;
            if (!deadlineTime.isEmpty()) result += " " + deadlineTime;
        }
        result += "  — " + icon + " " + status;
        return result;
    }
}
