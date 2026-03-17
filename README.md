# Task Master Pro - Java GUI Todo Manager

A modern, elegant desktop task manager built with Java Swing featuring a clean light-themed interface.

## ✨ Features

### Core Functionality
- **Add Tasks** with detailed information (description, deadline, time, priority, category, notes)
- **Manage Tasks** - Create, edit, delete, and complete tasks
- **Task Status** - Three-level status tracking: To Do, In Progress, Done
- **Due Times** - Set both date and time for deadlines
- **Categories** - Organize tasks by 6 built-in categories with distinct colors
- **Notes** - Add detailed notes to any task
- **Priority Levels** - High, Medium, Low priority designation

### Organization & Filtering
- **Search** - Quickly find tasks by description, category, or notes
- **Filter** - View tasks by status (All, To Do, In Progress, Done, Overdue)
- **Sort** - Organize tasks by Date Added, Priority, Deadline, Category, or Status
- **Overdue Detection** - Automatically highlights overdue tasks

### Advanced Features
- **Undo/Redo** - Revert or redo up to 20 recent actions [Ctrl+Z] [Ctrl+Y]
- **Export to CSV** - Export all tasks for use in spreadsheet apps
- **Progress Tracking** - Real-time completion percentage and task statistics
- **Statistics Dashboard** - View total, done, urgent, and overdue task counts
- **Autosave** - Automatically save tasks when closing (optional)

### Design & UX
- **Light Color Theme** - Modern, easy-on-the-eyes light interface
- **Responsive Layout** - Three-column dashboard (sidebar, main area, details)
- **Keyboard Shortcuts**:
  - `Ctrl+S` - Save tasks
  - `Ctrl+Z` - Undo
  - `Ctrl+Y` - Redo
  - `Ctrl+N` - New task
  - `Ctrl+F` - Search
  - `C` - Mark complete
  - `E` - Edit task
  - `Del` - Delete task

### Data Persistence
- **File Storage** - Tasks saved in `tasks.txt` with pipe-delimited format
- **Backward Compatible** - Reads old task formats automatically
- **CSV Export** - Export tasks to spreadsheet format

## Technologies Used

- **Java** - Core language
- **Swing** - GUI framework
- **Java Time API** - Date/time handling
- **File I/O** - Data persistence
- **Object-Oriented Programming** - Clean architecture

## Getting Started

### Requirements
- Java 11 or higher

### Compilation
```bash
javac *.java
```

### Running
```bash
java TodoGUI
```

## File Structure

- `TodoGUI.java` - Main application and UI logic (1000+ lines)
- `Task.java` - Task data model with support for status, time, notes
- `FileHandler.java` - File reading/writing with format compatibility
- `tasks.txt` - Data file (auto-created)

## Usage

1. **Add a Task**: Fill in the sidebar form and click "Add Task" or press Enter
2. **Edit a Task**: Double-click a task or select it and click Edit
3. **Complete a Task**: Click the task and use "Done" button or press C
4. **Mark In Progress**: Click "In Progress" button while editing
5. **Search**: Type in the search box to filter tasks in real-time
6. **Filter & Sort**: Use sidebar dropdowns to organize your view
7. **Export**: Click the export button in the top-right to save as CSV
8. **Undo Actions**: Press Ctrl+Z to undo recent changes

## Color Palette

- **Accent**: Indigo (primary actions)
- **Secondary**: Cyan (secondary actions)
- **Text**: Dark Gray (main text)
- **Categories**: 6 distinct colors (Blue, Red, Green, Amber, Purple, Pink)
- **Status Indicators**: 
  - Green (Done)
  - Amber (In Progress)
  - Orange (Overdue)

## Future Enhancements

- Task recurring patterns
- Due date notifications
- Task categories management
- Data backup/restore
- Custom color themes
- Collaborative task sharing


## Run the Project

Compile:

```
javac *.java
```

Run:

```
java TodoGUI
```
