import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class TodoGUI {

     // ── LIGHT COLOR PALETTE ────────────────────────────────────────────────────
    static final Color BG_ROOT      = new Color(0xF8, 0xF9, 0xFA);
    static final Color BG_SIDEBAR   = new Color(0xFF, 0xFF, 0xFF);
    static final Color BG_CARD      = new Color(0xFF, 0xFF, 0xFF);
    static final Color BG_INPUT     = new Color(0xFF, 0xFF, 0xFF);
    static final Color ACCENT       = new Color(0x4F, 0x46, 0xE5);
    static final Color ACCENT2      = new Color(0x06, 0xB6, 0xD4);
    static final Color TEXT_1       = new Color(0x1F, 0x2E, 0x3E);
    static final Color TEXT_2       = new Color(0x64, 0x74, 0x8B);
    static final Color TEXT_3       = new Color(0xA0, 0xAE, 0xC0);
    static final Color BORDER       = new Color(0xE5, 0xE7, 0xEB);
    static final Color RED          = new Color(0xDC, 0x26, 0x26);
    static final Color AMBER        = new Color(0xEA, 0xB3, 0x08);
    static final Color GREEN        = new Color(0x16, 0xA3, 0x4A);
    static final Color OVERDUE      = new Color(0xEA, 0x58, 0x0C);

    static final Color[] CATEGORY_COLORS = {
        new Color(0x3B, 0x82, 0xF6), new Color(0xEF, 0x44, 0x44), new Color(0x10, 0xB9, 0x81),
        new Color(0xF5, 0x9E, 0x0B), new Color(0x8B, 0x5C, 0xF6), new Color(0xEC, 0x48, 0x99)
    };

    static final String[] CATEGORIES  = {"General","Work","Personal","Health","Shopping","Study"};
    static final String[] SORT_OPTIONS = {"Date Added","Priority","Deadline","Category","Status"};
    static final String[] STATUS_OPTIONS = {"To Do","In Progress","Done"};
    static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── STATE ──────────────────────────────────────────────────────────────────
    JFrame frame;
    JTextField taskField, deadlineField, deadlineTimeField, searchField, notesField;
    JComboBox<String> priorityBox, categoryBox, filterStatus, sortBox, statusBox;
    DefaultListModel<String> listModel;
    JList<String> taskList;
    JLabel statsLabel, progressLabel;
    JProgressBar progressBar;
    JLabel detailTitle, detailMeta, detailStatus, detailNotes;
    JButton detailCompleteBtn, detailProgressBtn, detailDeleteBtn;
    JPanel statTilesRef;
    ArrayList<Task> tasks;
    ArrayList<Task> filteredTasks = new ArrayList<>();
    ArrayList<ArrayList<Task>> undoStack = new ArrayList<>();
    ArrayList<ArrayList<Task>> redoStack = new ArrayList<>();
    boolean autosave = true;

    public TodoGUI() {
        tasks = FileHandler.loadTasks();
        applyLAF();

        frame = new JFrame("Task Master Pro");
        frame.setSize(1050, 740);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { onClose(); }
        });
        frame.getContentPane().setBackground(BG_ROOT);
        frame.setLayout(new BorderLayout());

        frame.add(buildTopBar(), BorderLayout.NORTH);
        frame.add(buildMain(), BorderLayout.CENTER);

        setupKeyboard();
        refreshList();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    void applyLAF() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("OptionPane.background", BG_CARD);
        UIManager.put("Panel.background", BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_1);
    }

    private Color getCategoryColor(int idx) {
        return CATEGORY_COLORS[idx % CATEGORY_COLORS.length];
    }

    // ── TOP BAR ────────────────────────────────────────────────────────────────
    JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_SIDEBAR);
        bar.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER), new EmptyBorder(14,20,14,20)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel dot = new JLabel("★");
        dot.setFont(font(20));
        dot.setForeground(ACCENT);
        JPanel lbls = new JPanel();
        lbls.setLayout(new BoxLayout(lbls, BoxLayout.Y_AXIS));
        lbls.setOpaque(false);
        JLabel title = new JLabel("Task Master Pro");
        title.setFont(font(16).deriveFont(Font.BOLD));
        title.setForeground(TEXT_1);
        statsLabel = new JLabel("Loading…");
        statsLabel.setFont(font(11));
        statsLabel.setForeground(TEXT_2);
        lbls.add(title);
        lbls.add(statsLabel);
        left.add(dot);
        left.add(lbls);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        progressLabel = new JLabel("0%");
        progressLabel.setFont(font(11).deriveFont(Font.BOLD));
        progressLabel.setForeground(ACCENT);
        progressBar = new JProgressBar(0, 100) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                int w = (int) (getWidth() * getValue() / 100.0);
                if (w > 0) {
                    g2.setPaint(new GradientPaint(0, 0, ACCENT, w, 0, ACCENT2));
                    g2.fillRoundRect(0, 0, w, getHeight(), getHeight(), getHeight());
                }
                g2.dispose();
            }
        };
        progressBar.setBorderPainted(false);
        progressBar.setOpaque(false);
        progressBar.setPreferredSize(new Dimension(160, 7));

        JButton undoBtn = iconBtn("↶", TEXT_2, "Undo  [Ctrl+Z]");
        JButton redoBtn = iconBtn("↷", TEXT_2, "Redo  [Ctrl+Y]");
        JButton exportBtn = iconBtn("⬇", ACCENT2, "Export to CSV");
        undoBtn.addActionListener(e -> undo());
        redoBtn.addActionListener(e -> redo());
        exportBtn.addActionListener(e -> exportToCSV());

        right.add(progressLabel);
        right.add(progressBar);
        right.add(Box.createRigidArea(new Dimension(20, 0)));
        right.add(undoBtn);
        right.add(redoBtn);
        right.add(exportBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
     }

    JPanel buildMain() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_ROOT);
        p.add(buildSidebar(), BorderLayout.WEST);
        p.add(buildTaskArea(), BorderLayout.CENTER);
        p.add(buildDetailPanel(), BorderLayout.EAST);
        return p;
    }

    // ── SIDEBAR ────────────────────────────────────────────────────────────────
    JPanel buildSidebar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_SIDEBAR);
        p.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(0,0,0,1,BORDER), new EmptyBorder(20,16,20,16)));
        p.setPreferredSize(new Dimension(280, 0));

        p.add(secLabel("NEW TASK"));
        p.add(vgap(12));
        taskField = styledField("Task description…");
        deadlineField = styledField("Deadline (yyyy-MM-dd)");
        deadlineTimeField = styledField("Time (HH:mm)");
        notesField = styledField("Notes…");
        priorityBox = styledCombo(new String[]{"High", "Medium", "Low"});
        categoryBox = styledCombo(CATEGORIES);
        statusBox = styledCombo(STATUS_OPTIONS);
        taskField.addActionListener(e -> addTask());

        p.add(fg("Task", taskField));
        p.add(vgap(8));
        p.add(fg("Deadline", deadlineField));
        p.add(vgap(8));
        p.add(fg("Time", deadlineTimeField));
        p.add(vgap(8));
        p.add(fg("Priority", priorityBox));
        p.add(vgap(8));
        p.add(fg("Category", categoryBox));
        p.add(vgap(8));
        p.add(fg("Status", statusBox));
        p.add(vgap(8));
        p.add(fg("Notes", notesField));
        p.add(vgap(14));

        JButton addBtn = pill("+ Add Task", ACCENT, Color.WHITE);
        addBtn.addActionListener(e -> addTask());
        p.add(addBtn);
        p.add(vgap(22));
        p.add(divider());
        p.add(vgap(16));

        p.add(secLabel("FILTER & SORT"));
        p.add(vgap(10));
        filterStatus = styledCombo(new String[]{"All", "To Do", "In Progress", "Done", "Overdue"});
        sortBox = styledCombo(SORT_OPTIONS);
        filterStatus.addActionListener(e -> refreshList());
        sortBox.addActionListener(e -> refreshList());
        p.add(fg("Status", filterStatus));
        p.add(vgap(8));
        p.add(fg("Sort by", sortBox));
        p.add(vgap(22));
        p.add(divider());
        p.add(vgap(16));

        p.add(secLabel("OVERVIEW"));
        p.add(vgap(10));
        statTilesRef = new JPanel(new GridLayout(2, 2, 8, 8));
        statTilesRef.setOpaque(false);
        statTilesRef.setAlignmentX(Component.LEFT_ALIGNMENT);
        statTilesRef.setMaximumSize(new Dimension(Integer.MAX_VALUE, 94));
        p.add(statTilesRef);
        p.add(Box.createVerticalGlue());
        p.add(vgap(16));
        p.add(divider());
        p.add(vgap(10));

        JCheckBox autoCheck = new JCheckBox("Autosave on close");
        autoCheck.setSelected(true);
        autoCheck.setOpaque(false);
        autoCheck.setForeground(TEXT_2);
        autoCheck.setFont(font(11));
        autoCheck.addActionListener(e -> autosave = autoCheck.isSelected());
        autoCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(autoCheck);
        return p;
    }

    // ── TASK AREA ──────────────────────────────────────────────────────────────
    JPanel buildTaskArea() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(BG_ROOT);
        p.setBorder(new EmptyBorder(16, 16, 10, 8));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        searchField = styledField("Search tasks…");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshList(); }
            public void removeUpdate(DocumentEvent e) { refreshList(); }
            public void changedUpdate(DocumentEvent e) { refreshList(); }
        });
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        JButton doneBtn = iconBtn("✓", new Color(0x1A, 0x7A, 0x3E), "Mark complete  [C]");
        JButton editBtn = iconBtn("✎", new Color(0x2B, 0x6B, 0xB0), "Edit  [E]");
        JButton deleteBtn = iconBtn("✕", new Color(0xA6, 0x1E, 0x24), "Delete  [Del]");
        doneBtn.addActionListener(e -> markCompleted());
        editBtn.addActionListener(e -> editTask());
        deleteBtn.addActionListener(e -> deleteTask());
        btns.add(doneBtn);
        btns.add(editBtn);
        btns.add(deleteBtn);
        top.add(searchField, BorderLayout.CENTER);
        top.add(btns, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        taskList = new JList<>(listModel);
        taskList.setBackground(BG_ROOT);
        taskList.setSelectionBackground(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 50));
        taskList.setFixedCellHeight(60);
        taskList.setCellRenderer(new TaskRenderer());
        taskList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateDetail(); });
        taskList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) editTask(); }
        });

        JScrollPane scroll = new JScrollPane(taskList);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scroll.getViewport().setBackground(BG_ROOT);
        styleScrollbar(scroll.getVerticalScrollBar());
        p.add(scroll, BorderLayout.CENTER);

        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.setBorder(new EmptyBorder(6, 0, 0, 0));
        JLabel hint = new JLabel("↵ add  ·  double-click edit  ·  [C] done  [E] edit  [Del] remove  [Ctrl+S] save");
        hint.setFont(font(9));
        hint.setForeground(TEXT_3);
        JPanel botR = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botR.setOpaque(false);
        JButton clearBtn = ghostBtn("Clear Done");
        JButton saveBtn = pill("💾 Save", new Color(0xE8, 0xF5, 0xE9), new Color(0x16, 0xA3, 0x4A));
        clearBtn.addActionListener(e -> clearCompleted());
        saveBtn.addActionListener(e -> saveTasks());
        botR.add(clearBtn);
        botR.add(saveBtn);
        bot.add(hint, BorderLayout.WEST);
        bot.add(botR, BorderLayout.EAST);
        p.add(bot, BorderLayout.SOUTH);
        return p;
    }

    // ── DETAIL PANEL ───────────────────────────────────────────────────────────
    JPanel buildDetailPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_SIDEBAR);
        p.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(0,1,0,0,BORDER), new EmptyBorder(20,16,20,16)));
        p.setPreferredSize(new Dimension(250, 0));

        p.add(secLabel("TASK DETAILS"));
        p.add(vgap(14));

        detailTitle = new JLabel("<html><i>Select a task…</i></html>");
        detailTitle.setFont(font(13).deriveFont(Font.BOLD));
        detailTitle.setForeground(TEXT_1);
        detailTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        detailMeta = new JLabel(" ");
        detailMeta.setFont(font(10));
        detailMeta.setForeground(TEXT_2);
        detailMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailMeta.setBorder(new EmptyBorder(0, 0, 8, 0));

        detailStatus = new JLabel(" ");
        detailStatus.setFont(font(12).deriveFont(Font.BOLD));
        detailStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailStatus.setBorder(new EmptyBorder(0, 0, 8, 0));

        detailNotes = new JLabel(" ");
        detailNotes.setFont(font(11));
        detailNotes.setForeground(TEXT_2);
        detailNotes.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailNotes.setBorder(new EmptyBorder(0, 0, 16, 0));

        p.add(detailTitle);
        p.add(detailMeta);
        p.add(detailStatus);
        p.add(detailNotes);
        p.add(divider());
        p.add(vgap(14));

        detailCompleteBtn = pill("✓ Done", new Color(0x1A, 0x7A, 0x3E), Color.WHITE);
        detailProgressBtn = pill("⟳ In Progress", new Color(0x2B, 0x6B, 0xB0), Color.WHITE);
        detailDeleteBtn = pill("✕ Delete", new Color(0xA6, 0x1E, 0x24), Color.WHITE);
        detailCompleteBtn.addActionListener(e -> updateTaskStatus("Done"));
        detailProgressBtn.addActionListener(e -> updateTaskStatus("In Progress"));
        detailDeleteBtn.addActionListener(e -> deleteTask());
        detailCompleteBtn.setEnabled(false);
        detailProgressBtn.setEnabled(false);
        detailDeleteBtn.setEnabled(false);

        p.add(detailCompleteBtn);
        p.add(vgap(8));
        p.add(detailProgressBtn);
        p.add(vgap(8));
        p.add(detailDeleteBtn);
        p.add(Box.createVerticalGlue());
        return p;
    }

    void updateDetail() {
        int idx = taskList.getSelectedIndex();
        if (idx < 0 || idx >= filteredTasks.size()) {
            detailTitle.setText("<html><i style='color:#A0AEC0'>Select a task…</i></html>");
            detailMeta.setText(" ");
            detailStatus.setText(" ");
            detailNotes.setText(" ");
            detailCompleteBtn.setEnabled(false);
            detailProgressBtn.setEnabled(false);
            detailDeleteBtn.setEnabled(false);
            return;
        }
        Task t = filteredTasks.get(idx);
        detailTitle.setText("<html><b>" + esc(t.description) + "</b></html>");

        String meta = "<html>" + t.priority + " · " + t.category;
        if (!t.deadline.isEmpty()) {
            meta += "<br>📅 " + t.deadline;
            if (!t.deadlineTime.isEmpty()) meta += " " + t.deadlineTime;
            if (isOverdue(t)) meta += " <font color='#EA580C'> ⚠ Overdue</font>";
        }
        meta += "</html>";
        detailMeta.setText(meta);

        if (t.status.equals("Done")) {
            detailStatus.setText("✓ Done");
            detailStatus.setForeground(GREEN);
        } else if (isOverdue(t)) {
            detailStatus.setText("⚠ Overdue");
            detailStatus.setForeground(OVERDUE);
        } else if (t.status.equals("In Progress")) {
            detailStatus.setText("⟳ In Progress");
            detailStatus.setForeground(AMBER);
        } else {
            detailStatus.setText("○ To Do");
            detailStatus.setForeground(TEXT_2);
        }

        detailNotes.setText(t.notes.isEmpty() ? "<html><i style='color:#A0AEC0'>No notes</i></html>" : "<html>" + esc(t.notes) + "</html>");

        detailCompleteBtn.setEnabled(!t.status.equals("Done"));
        detailProgressBtn.setEnabled(!t.status.equals("In Progress") && !t.status.equals("Done"));
        detailDeleteBtn.setEnabled(true);
    }

    void setupKeyboard() {
        JRootPane r = frame.getRootPane();
        kb(r, "C", KeyEvent.VK_C, 0, this::markCompleted);
        kb(r, "E", KeyEvent.VK_E, 0, this::editTask);
        kb(r, "DEL", KeyEvent.VK_DELETE, 0, this::deleteTask);
        kb(r, "SAVE", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, this::saveTasks);
        kb(r, "UNDO", KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, this::undo);
        kb(r, "REDO", KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK, this::redo);
        kb(r, "NEW", KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK, () -> taskField.requestFocus());
        kb(r, "SEARCH", KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, () -> searchField.requestFocus());
    }

    void kb(JRootPane r, String n, int k, int m, Runnable fn) {
        r.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(k, m), n);
        r.getActionMap().put(n, new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { fn.run(); } });
    }

    // ── ACTIONS ────────────────────────────────────────────────────────────────
    void saveState() {
        undoStack.add(deepCopy(tasks));
        redoStack.clear();
        if (undoStack.size() > 20) undoStack.remove(0);
    }

    void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.add(deepCopy(tasks));
        tasks = undoStack.remove(undoStack.size() - 1);
        refreshList();
    }

    void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.add(deepCopy(tasks));
        tasks = redoStack.remove(redoStack.size() - 1);
        refreshList();
    }

    void addTask() {
        String desc = taskField.getText().trim();
        if (desc.isEmpty()) { shake(taskField); return; }
        String dl = deadlineField.getText().trim();
        String tm = deadlineTimeField.getText().trim();
        String note = notesField.getText().trim();

        if (dl.equals("Deadline (yyyy-MM-dd)")) dl = "";
        if (tm.equals("Time (HH:mm)")) tm = "";
        if (note.equals("Notes…")) note = "";

        saveState();
        tasks.add(new Task(desc, dl, tm, (String) priorityBox.getSelectedItem(), (String) categoryBox.getSelectedItem(), note, (String) statusBox.getSelectedItem()));

        taskField.setText("");
        deadlineField.setText("");
        deadlineTimeField.setText("");
        notesField.setText("");
        statusBox.setSelectedIndex(0);
        refreshList();
        taskField.requestFocus();
    }

    void deleteTask() {
        int idx = taskList.getSelectedIndex();
        if (idx < 0 || idx >= filteredTasks.size()) return;
        Task t = filteredTasks.get(idx);
        if (JOptionPane.showConfirmDialog(frame, "Delete  \"" + t.description + "\" ?", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            saveState();
            tasks.remove(t);
            refreshList();
        }
    }

    void markCompleted() {
        int idx = taskList.getSelectedIndex();
        if (idx >= 0 && idx < filteredTasks.size()) {
            updateTaskStatus("Done");
        }
    }

    void updateTaskStatus(String status) {
        int idx = taskList.getSelectedIndex();
        if (idx >= 0 && idx < filteredTasks.size()) {
            saveState();
            filteredTasks.get(idx).status = status;
            filteredTasks.get(idx).completed = status.equals("Done");
            refreshList();
        }
    }

    void editTask() {
        int idx = taskList.getSelectedIndex();
        if (idx < 0 || idx >= filteredTasks.size()) return;
        Task t = filteredTasks.get(idx);

        JDialog dlg = new JDialog(frame, "Edit Task", true);
        dlg.setSize(450, 480);
        dlg.setLocationRelativeTo(frame);
        dlg.getContentPane().setBackground(BG_CARD);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(BG_CARD);
        inner.setBorder(new EmptyBorder(24, 24, 24, 24));

        JTextField dF = styledField(t.description);
        dF.setText(t.description);
        JTextField dlF = styledField(t.deadline);
        dlF.setText(t.deadline);
        JTextField tmF = styledField(t.deadlineTime);
        tmF.setText(t.deadlineTime);
        JTextField nF = styledField(t.notes);
        nF.setText(t.notes);
        JComboBox<String> pB = styledCombo(new String[]{"High", "Medium", "Low"});
        pB.setSelectedItem(t.priority);
        JComboBox<String> cB = styledCombo(CATEGORIES);
        cB.setSelectedItem(t.category);
        JComboBox<String> sB = styledCombo(STATUS_OPTIONS);
        sB.setSelectedItem(t.status);

        inner.add(fg("Task", dF));
        inner.add(vgap(10));
        inner.add(fg("Deadline", dlF));
        inner.add(vgap(10));
        inner.add(fg("Time", tmF));
        inner.add(vgap(10));
        inner.add(fg("Priority", pB));
        inner.add(vgap(10));
        inner.add(fg("Category", cB));
        inner.add(vgap(10));
        inner.add(fg("Status", sB));
        inner.add(vgap(10));
        inner.add(fg("Notes", nF));
        inner.add(vgap(20));

        JButton save = pill("Save Changes", ACCENT, Color.WHITE);
        save.setAlignmentX(Component.CENTER_ALIGNMENT);
        save.addActionListener(ev -> {
            saveState();
            t.description = dF.getText().trim();
            t.deadline = dlF.getText().trim();
            t.deadlineTime = tmF.getText().trim();
            t.priority = (String) pB.getSelectedItem();
            t.category = (String) cB.getSelectedItem();
            t.status = (String) sB.getSelectedItem();
            t.completed = t.status.equals("Done");
            t.notes = nF.getText().trim();
            refreshList();
            dlg.dispose();
        });
        inner.add(save);
        dlg.add(inner);
        dlg.setVisible(true);
    }

    void clearCompleted() {
        long n = tasks.stream().filter(t -> t.status.equals("Done")).count();
        if (n == 0) { JOptionPane.showMessageDialog(frame, "No completed tasks.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        if (JOptionPane.showConfirmDialog(frame, "Remove " + n + " completed task" + (n > 1 ? "s" : "") + "?", "Clear Completed", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            saveState();
            tasks.removeIf(t -> t.status.equals("Done"));
            refreshList();
        }
    }

    void exportToCSV() {
        try {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("tasks.csv"));
            int result = fc.showSaveDialog(frame);
            if (result != JFileChooser.APPROVE_OPTION) return;

            File file = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("Task,Priority,Category,Deadline,Time,Status,Notes");
                for (Task t : tasks) {
                    pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n", t.description, t.priority, t.category, t.deadline, t.deadlineTime, t.status, t.notes);
                }
            }
            JOptionPane.showMessageDialog(frame, "Tasks exported to CSV successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void saveTasks() {
        FileHandler.saveTasks(tasks);
        String old = statsLabel.getText();
        statsLabel.setText(old + "  ✓ Saved");
        javax.swing.Timer t = new javax.swing.Timer(2000, e -> refreshStats());
        t.setRepeats(false);
        t.start();
    }

    void onClose() {
        if (autosave) FileHandler.saveTasks(tasks);
        System.exit(0);
    }

    void refreshList() {
        String q = searchField != null ? searchField.getText().toLowerCase() : "";
        String st = filterStatus != null ? (String) filterStatus.getSelectedItem() : "All";
        String so = sortBox != null ? (String) sortBox.getSelectedItem() : "Date Added";

        filteredTasks = tasks.stream()
                .filter(t -> q.isEmpty() || t.description.toLowerCase().contains(q) || t.category.toLowerCase().contains(q) || t.notes.toLowerCase().contains(q))
                .filter(t -> switch (st) {
                    case "To Do" -> t.status.equals("To Do");
                    case "In Progress" -> t.status.equals("In Progress");
                    case "Done" -> t.status.equals("Done");
                    case "Overdue" -> isOverdue(t) && !t.status.equals("Done");
                    default -> true;
                })
                .sorted(sortComp(so))
                .collect(Collectors.toCollection(ArrayList::new));

        int sel = taskList != null ? taskList.getSelectedIndex() : -1;
        listModel.clear();
        for (Task t : filteredTasks) listModel.addElement(t.description);
        if (taskList != null && sel >= 0 && sel < listModel.size()) taskList.setSelectedIndex(sel);

        refreshStats();
        rebuildTiles();
        updateDetail();
    }

    void refreshStats() {
        long total = tasks.size(), done = tasks.stream().filter(t -> t.status.equals("Done")).count();
        int pct = total == 0 ? 0 : (int) (done * 100 / total);
        if (statsLabel != null) statsLabel.setText(total + " tasks · " + (total - done) + " pending · " + done + " done");
        if (progressBar != null) progressBar.setValue(pct);
        if (progressLabel != null) progressLabel.setText(pct + "%");
    }

    void rebuildTiles() {
        if (statTilesRef == null) return;
        statTilesRef.removeAll();
        long tot = tasks.size();
        long done = tasks.stream().filter(t -> t.status.equals("Done")).count();
        long hi = tasks.stream().filter(t -> t.priority.equals("High") && !t.status.equals("Done")).count();
        long ov = tasks.stream().filter(this::isOverdue).count();
        statTilesRef.add(tile("Total", "" + tot, TEXT_2));
        statTilesRef.add(tile("Done", "" + done, GREEN));
        statTilesRef.add(tile("Urgent", "" + hi, RED));
        statTilesRef.add(tile("Overdue", "" + ov, OVERDUE));
        statTilesRef.revalidate();
        statTilesRef.repaint();
    }

    JPanel tile(String label, String val, Color vc) {
        JPanel t = new JPanel();
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setBackground(BG_CARD);
        t.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(9, 11, 9, 11)));
        JLabel v = new JLabel(val);
        v.setFont(font(20).deriveFont(Font.BOLD));
        v.setForeground(vc);
        v.setAlignmentX(0);
        JLabel l = new JLabel(label);
        l.setFont(font(9));
        l.setForeground(TEXT_3);
        l.setAlignmentX(0);
        t.add(v);
        t.add(l);
        return t;
    }

    Comparator<Task> sortComp(String s) {
        return switch (s) {
            case "Priority" -> Comparator.comparingInt(t -> priRank(t.priority));
            case "Deadline" -> Comparator.comparing(t -> t.deadline.isEmpty() ? "9999" : t.deadline);
            case "Category" -> Comparator.comparing(t -> t.category);
            case "Status" -> Comparator.comparing(t -> t.status);
            default -> (a, b) -> 0;
        };
    }

    int priRank(String p) { return switch (p) { case "High" -> 0; case "Medium" -> 1; default -> 2; }; }

    boolean isOverdue(Task t) {
        if (t.status.equals("Done") || t.deadline == null || t.deadline.isEmpty()) return false;
        try { return LocalDate.parse(t.deadline, DT_FMT).isBefore(LocalDate.now()); }
        catch (Exception e) { return false; }
    }

    // ── HELPERS ────────────────────────────────────────────────────────────────
    JPanel fg(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        JLabel l = new JLabel(label);
        l.setFont(font(9).deriveFont(Font.BOLD));
        l.setForeground(TEXT_3);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    JTextField styledField(String ph) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(TEXT_3);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    g2.drawString(ph, 9, getHeight() / 2 + 5);
                }
            }
        };
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_1);
        f.setCaretColor(ACCENT);
        f.setFont(font(12));
        f.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(6, 8, 6, 8)));
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) { f.setBorder(new CompoundBorder(BorderFactory.createLineBorder(ACCENT, 1), new EmptyBorder(6, 8, 6, 8))); }
            @Override
            public void focusLost(FocusEvent e) { f.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(6, 8, 6, 8))); }
        });
        return f;
    }

    <T> JComboBox<T> styledCombo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setBackground(BG_INPUT);
        c.setForeground(TEXT_1);
        c.setFont(font(12));
        c.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        c.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(list, v, i, s, f);
                setBackground(s ? ACCENT : BG_INPUT);
                setForeground(s ? Color.WHITE : TEXT_1);
                setBorder(new EmptyBorder(6, 8, 6, 8));
                return this;
            }
        });
        return c;
    }

    JButton pill(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            boolean h = false;
            { addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { h = true; repaint(); }
                @Override
                public void mouseExited(MouseEvent e) { h = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = !isEnabled() ? new Color(200, 200, 200) : h ? bg.brighter() : bg;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isEnabled() ? (fg.getRed() + fg.getGreen() + fg.getBlue() > 382 ? TEXT_1 : fg) : new Color(150, 150, 150));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(font(12).deriveFont(Font.BOLD));
        b.setPreferredSize(new Dimension(140, 36));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JButton iconBtn(String text, Color bg, String tip) {
        JButton b = new JButton(text) {
            boolean h = false;
            { addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { h = true; repaint(); }
                @Override
                public void mouseExited(MouseEvent e) { h = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = h ? new Color(240, 240, 240) : new Color(250, 250, 250);
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(isEnabled() ? bg : TEXT_3);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(font(14).deriveFont(Font.BOLD));
        b.setPreferredSize(new Dimension(32, 32));
        b.setMaximumSize(new Dimension(32, 32));
        b.setToolTipText(tip);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JButton ghostBtn(String text) {
        JButton b = pill(text, new Color(0xF0, 0xFD, 0xFA), TEXT_2);
        b.setPreferredSize(new Dimension(130, 32));
        return b;
    }

    JLabel secLabel(String s) {
        JLabel l = new JLabel(s);
        l.setFont(font(10).deriveFont(Font.BOLD));
        l.setForeground(TEXT_3);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    JSeparator divider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    Component vgap(int h) { return Box.createRigidArea(new Dimension(0, h)); }

    void styleScrollbar(JScrollBar sb) {
        sb.setBackground(BG_SIDEBAR);
        sb.setPreferredSize(new Dimension(7, 0));
        sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() { thumbColor = BORDER; trackColor = BG_SIDEBAR; }
            @Override
            protected JButton createDecreaseButton(int o) { return zBtn(); }
            @Override
            protected JButton createIncreaseButton(int o) { return zBtn(); }
            JButton zBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    void shake(Component c) {
        Point o = c.getLocation();
        int[] dx = {-5, 5, -3, 3, 0};
        int[] i = {0};
        javax.swing.Timer t = new javax.swing.Timer(30, null);
        t.addActionListener(e -> {
            if (i[0] >= dx.length) { t.stop(); c.setLocation(o); return; }
            c.setLocation(o.x + dx[i[0]++], o.y);
        });
        t.start();
    }

    Font font(float sz) { return new Font("Segoe UI", Font.PLAIN, (int) sz); }
    String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    ArrayList<Task> deepCopy(ArrayList<Task> list) {
        ArrayList<Task> copy = new ArrayList<>();
        for (Task t : list) {
            copy.add(new Task(t.description, t.deadline, t.deadlineTime, t.priority, t.category, t.notes, t.status));
        }
        return copy;
    }

    // ── CELL RENDERER ──────────────────────────────────────────────────────────
    class TaskRenderer extends JPanel implements ListCellRenderer<String> {
        JPanel bar;
        JLabel main, sub, badge;

        TaskRenderer() {
            setLayout(new BorderLayout(0, 0));
            setBorder(new EmptyBorder(0, 0, 0, 0));

            bar = new JPanel();
            bar.setPreferredSize(new Dimension(4, 0));

            JPanel content = new JPanel(new BorderLayout(12, 0));
            content.setOpaque(false);
            content.setBorder(new EmptyBorder(8, 12, 8, 12));

            JPanel text = new JPanel(new GridLayout(2, 1, 0, 3));
            text.setOpaque(false);
            main = new JLabel();
            main.setFont(font(13).deriveFont(Font.BOLD));
            sub = new JLabel();
            sub.setFont(font(9));
            text.add(main);
            text.add(sub);

            badge = new JLabel();
            badge.setFont(font(11).deriveFont(Font.BOLD));
            badge.setHorizontalAlignment(SwingConstants.RIGHT);
            badge.setPreferredSize(new Dimension(32, 28));

            content.add(text, BorderLayout.CENTER);
            content.add(badge, BorderLayout.EAST);
            add(bar, BorderLayout.WEST);
            add(content, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int idx, boolean sel, boolean foc) {

            if (idx >= 0 && idx < filteredTasks.size()) {
                Task t = filteredTasks.get(idx);
                boolean ov = isOverdue(t);

                int catIdx = java.util.Arrays.asList(CATEGORIES).indexOf(t.category);
                Color barColor = t.status.equals("Done") ? TEXT_3 : ov ? OVERDUE : getCategoryColor(catIdx);
                bar.setBackground(barColor);

                main.setText(t.status.equals("Done") ? "<html><strike style='color:#A0AEC0'>" + esc(t.description) + "</strike></html>" : t.description);
                main.setForeground(t.status.equals("Done") ? TEXT_3 : TEXT_1);

                String s = t.category;
                if (!t.deadline.isEmpty()) {
                    s += "  ·  📅 " + t.deadline;
                    if (!t.deadlineTime.isEmpty()) s += " " + t.deadlineTime;
                    if (ov) s += " ⚠";
                }
                if (!t.notes.isEmpty()) s += "  ·  " + t.notes;
                sub.setText(s);
                sub.setForeground(ov ? OVERDUE : TEXT_2);

                String badgeText = switch (t.status) {
                    case "Done" -> "✓";
                    case "In Progress" -> "⟳";
                    default -> "";
                };
                badge.setText(badgeText);
                badge.setForeground(t.status.equals("Done") ? GREEN : AMBER);
            }

            Color bg = sel ? new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 20) : idx % 2 == 0 ? BG_ROOT : new Color(0xFF, 0xFF, 0xFF);
            setBackground(bg);
            bar.setOpaque(true);
            setOpaque(true);
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TodoGUI::new);
    }
}
