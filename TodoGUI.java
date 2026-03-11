import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class TodoGUI {

    // ── Palette ──────────────────────────────────────────────────────────────
    static final Color BG_ROOT    = new Color(0x0C, 0x0E, 0x16);
    static final Color BG_SIDEBAR = new Color(0x13, 0x15, 0x1F);
    static final Color BG_CARD    = new Color(0x1A, 0x1D, 0x2B);
    static final Color BG_INPUT   = new Color(0x20, 0x24, 0x36);
    static final Color ACCENT     = new Color(0x7C, 0x6F, 0xFF);
    static final Color ACCENT2    = new Color(0x00, 0xD4, 0xC8);
    static final Color TEXT_1     = new Color(0xF2, 0xF2, 0xFF);
    static final Color TEXT_2     = new Color(0x8A, 0x8E, 0xAB);
    static final Color TEXT_3     = new Color(0x50, 0x54, 0x70);
    static final Color BORDER     = new Color(0x28, 0x2C, 0x42);
    static final Color RED        = new Color(0xFF, 0x4F, 0x62);
    static final Color AMBER      = new Color(0xFF, 0xAD, 0x38);
    static final Color GREEN      = new Color(0x2E, 0xCC, 0x85);
    static final Color OVERDUE    = new Color(0xFF, 0x6B, 0x35);

    static final String[] CATEGORIES  = {"General","Work","Personal","Health","Shopping","Study"};
    static final String[] SORT_OPTIONS = {"Date Added","Priority","Deadline","Category","Status"};
    static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── State ─────────────────────────────────────────────────────────────────
    JFrame frame;
    JTextField taskField, deadlineField, searchField, notesField;
    JComboBox<String> priorityBox, categoryBox, filterStatus, sortBox;
    DefaultListModel<String> listModel;
    JList<String> taskList;
    JLabel statsLabel, progressLabel;
    JProgressBar progressBar;
    JLabel detailTitle, detailMeta, detailStatus, detailNotes;
    JButton detailCompleteBtn, detailDeleteBtn;
    JPanel statTilesRef;
    ArrayList<Task> tasks;
    ArrayList<Task> filteredTasks = new ArrayList<>();
    boolean autosave = true;

    // ─────────────────────────────────────────────────────────────────────────
    public TodoGUI() {
        tasks = FileHandler.loadTasks();
        applyLAF();

        frame = new JFrame("Task Master");
        frame.setSize(920, 680);
        frame.setMinimumSize(new Dimension(740, 540));
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { onClose(); }
        });
        frame.getContentPane().setBackground(BG_ROOT);
        frame.setLayout(new BorderLayout());

        frame.add(buildTopBar(),    BorderLayout.NORTH);
        frame.add(buildMain(),      BorderLayout.CENTER);

        setupKeyboard();
        refreshList();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    void applyLAF() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {}
        UIManager.put("OptionPane.background",        BG_CARD);
        UIManager.put("Panel.background",             BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_1);
    }

    // ── Top Bar ───────────────────────────────────────────────────────────────
    JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_SIDEBAR);
        bar.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,BORDER),
            new EmptyBorder(12,20,12,20)));

        // Left
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        left.setOpaque(false);
        JLabel dot = new JLabel("◆");
        dot.setFont(font(20));
        dot.setForeground(ACCENT);
        JPanel lbls = new JPanel();
        lbls.setLayout(new BoxLayout(lbls,BoxLayout.Y_AXIS));
        lbls.setOpaque(false);
        JLabel title = new JLabel("Task Master");
        title.setFont(font(16).deriveFont(Font.BOLD));
        title.setForeground(TEXT_1);
        statsLabel = new JLabel("Loading…");
        statsLabel.setFont(font(11));
        statsLabel.setForeground(TEXT_2);
        lbls.add(title); lbls.add(statsLabel);
        left.add(dot); left.add(lbls);

        // Right: animated progress bar
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        right.setOpaque(false);
        progressLabel = new JLabel("0%");
        progressLabel.setFont(font(11).deriveFont(Font.BOLD));
        progressLabel.setForeground(ACCENT);
        progressBar = new JProgressBar(0,100) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_INPUT);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());
                int w = (int)(getWidth()*getValue()/100.0);
                if (w > 0) {
                    g2.setPaint(new GradientPaint(0,0,ACCENT,w,0,ACCENT2));
                    g2.fillRoundRect(0,0,w,getHeight(),getHeight(),getHeight());
                }
                g2.dispose();
            }
        };
        progressBar.setBorderPainted(false);
        progressBar.setOpaque(false);
        progressBar.setPreferredSize(new Dimension(150,8));
        right.add(progressLabel); right.add(progressBar);

        bar.add(left,BorderLayout.WEST);
        bar.add(right,BorderLayout.EAST);
        return bar;
    }

    // ── Main 3-column layout ──────────────────────────────────────────────────
    JPanel buildMain() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_ROOT);
        p.add(buildSidebar(),      BorderLayout.WEST);
        p.add(buildTaskArea(),     BorderLayout.CENTER);
        p.add(buildDetailPanel(),  BorderLayout.EAST);
        return p;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    JPanel buildSidebar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBackground(BG_SIDEBAR);
        p.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0,0,0,1,BORDER),
            new EmptyBorder(20,16,20,16)));
        p.setPreferredSize(new Dimension(248,0));

        p.add(secLabel("NEW TASK"));    p.add(vgap(12));
        taskField     = styledField("Task description…");
        deadlineField = styledField("Deadline  yyyy-MM-dd");
        notesField    = styledField("Notes…");
        priorityBox   = styledCombo(new String[]{"High","Medium","Low"});
        categoryBox   = styledCombo(CATEGORIES);
        taskField.addActionListener(e -> addTask());

        p.add(fg("Task",     taskField));     p.add(vgap(8));
        p.add(fg("Deadline", deadlineField)); p.add(vgap(8));
        p.add(fg("Priority", priorityBox));   p.add(vgap(8));
        p.add(fg("Category", categoryBox));   p.add(vgap(8));
        p.add(fg("Notes",    notesField));    p.add(vgap(14));

        JButton addBtn = pill("＋ Add Task", ACCENT, Color.WHITE);
        addBtn.addActionListener(e -> addTask());
        p.add(addBtn);
        p.add(vgap(22)); p.add(divider()); p.add(vgap(16));

        p.add(secLabel("FILTER & SORT"));    p.add(vgap(10));
        filterStatus = styledCombo(new String[]{"All","Pending","Completed","Overdue"});
        sortBox      = styledCombo(SORT_OPTIONS);
        filterStatus.addActionListener(e -> refreshList());
        sortBox.addActionListener(e -> refreshList());
        p.add(fg("Status",  filterStatus)); p.add(vgap(8));
        p.add(fg("Sort by", sortBox));
        p.add(vgap(22)); p.add(divider()); p.add(vgap(16));

        p.add(secLabel("OVERVIEW"));         p.add(vgap(10));
        statTilesRef = new JPanel(new GridLayout(2,2,8,8));
        statTilesRef.setOpaque(false);
        statTilesRef.setAlignmentX(Component.LEFT_ALIGNMENT);
        statTilesRef.setMaximumSize(new Dimension(Integer.MAX_VALUE,94));
        p.add(statTilesRef);
        p.add(Box.createVerticalGlue());
        p.add(vgap(16)); p.add(divider()); p.add(vgap(10));

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

    // ── Task Area ─────────────────────────────────────────────────────────────
    JPanel buildTaskArea() {
        JPanel p = new JPanel(new BorderLayout(0,10));
        p.setBackground(BG_ROOT);
        p.setBorder(new EmptyBorder(16,16,10,8));

        // Search + action buttons
        JPanel top = new JPanel(new BorderLayout(10,0));
        top.setOpaque(false);
        searchField = styledField("Search tasks…");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e)  { refreshList(); }
            @Override
            public void removeUpdate(DocumentEvent e)  { refreshList(); }
            @Override
            public void changedUpdate(DocumentEvent e) { refreshList(); }
        });
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
        btns.setOpaque(false);
        JButton doneBtn   = iconBtn("✓", new Color(0x1E,0x7A,0x52), "Mark complete  [C]");
        JButton editBtn   = iconBtn("✎", new Color(0x2B,0x6B,0xB0), "Edit  [E]");
        JButton deleteBtn = iconBtn("✕", new Color(0x8B,0x1F,0x2C), "Delete  [Del]");
        doneBtn.addActionListener(e -> markCompleted());
        editBtn.addActionListener(e -> editTask());
        deleteBtn.addActionListener(e -> deleteTask());
        btns.add(doneBtn); btns.add(editBtn); btns.add(deleteBtn);
        top.add(searchField,BorderLayout.CENTER);
        top.add(btns,BorderLayout.EAST);
        p.add(top,BorderLayout.NORTH);

        // List
        listModel = new DefaultListModel<>();
        taskList  = new JList<>(listModel);
        taskList.setBackground(BG_ROOT);
        taskList.setSelectionBackground(new Color(ACCENT.getRed(),ACCENT.getGreen(),ACCENT.getBlue(),50));
        taskList.setFixedCellHeight(54);
        taskList.setCellRenderer(new TaskRenderer());
        taskList.addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) updateDetail(); });
        taskList.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){ if(e.getClickCount()==2) editTask(); }
        });

        JScrollPane scroll = new JScrollPane(taskList);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER,1));
        scroll.getViewport().setBackground(BG_ROOT);
        styleScrollbar(scroll.getVerticalScrollBar());
        p.add(scroll,BorderLayout.CENTER);

        // Bottom hint + buttons
        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.setBorder(new EmptyBorder(6,0,0,0));
        JLabel hint = new JLabel("↵ add  ·  double-click edit  ·  [C] done  [E] edit  [Del] remove  [Ctrl+S] save  [Ctrl+F] search");
        hint.setFont(font(10));
        hint.setForeground(TEXT_3);
        JPanel botR = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        botR.setOpaque(false);
        JButton clearBtn = ghostBtn("Clear Completed");
        JButton saveBtn  = pill("💾 Save", new Color(0x22,0x22,0x44), ACCENT);
        clearBtn.addActionListener(e -> clearCompleted());
        saveBtn.addActionListener(e -> saveTasks());
        botR.add(clearBtn); botR.add(saveBtn);
        bot.add(hint,BorderLayout.WEST);
        bot.add(botR,BorderLayout.EAST);
        p.add(bot,BorderLayout.SOUTH);
        return p;
    }

    // ── Detail Panel ─────────────────────────────────────────────────────────
    JPanel buildDetailPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBackground(BG_SIDEBAR);
        p.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0,1,0,0,BORDER),
            new EmptyBorder(20,16,20,16)));
        p.setPreferredSize(new Dimension(210,0));

        p.add(secLabel("TASK DETAILS")); p.add(vgap(14));

        detailTitle = new JLabel("<html><i>Select a task…</i></html>");
        detailTitle.setFont(font(13).deriveFont(Font.BOLD));
        detailTitle.setForeground(TEXT_1);
        detailTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailTitle.setBorder(new EmptyBorder(0,0,10,0));

        detailMeta = new JLabel(" ");
        detailMeta.setFont(font(11));
        detailMeta.setForeground(TEXT_2);
        detailMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailMeta.setBorder(new EmptyBorder(0,0,8,0));

        detailStatus = new JLabel(" ");
        detailStatus.setFont(font(12).deriveFont(Font.BOLD));
        detailStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailStatus.setBorder(new EmptyBorder(0,0,8,0));

        detailNotes = new JLabel(" ");
        detailNotes.setFont(font(11));
        detailNotes.setForeground(TEXT_2);
        detailNotes.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailNotes.setBorder(new EmptyBorder(0,0,16,0));

        p.add(detailTitle); p.add(detailMeta); p.add(detailStatus); p.add(detailNotes);
        p.add(divider());   p.add(vgap(14));

        detailCompleteBtn = pill("✓  Complete", new Color(0x1A,0x6B,0x47), new Color(0x7F,0xFF,0xC0));
        detailDeleteBtn   = pill("✕  Delete",   new Color(0x5A,0x1A,0x1A), new Color(0xFF,0xAA,0xAA));
        detailCompleteBtn.addActionListener(e -> markCompleted());
        detailDeleteBtn.addActionListener(e -> deleteTask());
        detailCompleteBtn.setEnabled(false);
        detailDeleteBtn.setEnabled(false);

        p.add(detailCompleteBtn); p.add(vgap(8)); p.add(detailDeleteBtn);
        p.add(Box.createVerticalGlue());
        return p;
    }

    void updateDetail() {
        int idx = taskList.getSelectedIndex();
        if (idx < 0 || idx >= filteredTasks.size()) {
            detailTitle.setText("<html><i style='color:#505470'>Select a task…</i></html>");
            detailMeta.setText(" "); detailStatus.setText(" "); detailNotes.setText(" ");
            detailCompleteBtn.setEnabled(false); detailDeleteBtn.setEnabled(false);
            return;
        }
        Task t = filteredTasks.get(idx);
        detailTitle.setText("<html><b>" + esc(t.description) + "</b></html>");

        String meta = "<html>" + t.priority + " · " + t.category;
        if (!t.deadline.isEmpty()) {
            meta += "<br>📅 " + t.deadline;
            if (isOverdue(t)) meta += " <font color='#FF6B35'> ⚠ Overdue</font>";
        }
        meta += "</html>";
        detailMeta.setText(meta);

        if (t.completed) {
            detailStatus.setText("✓  Completed");
            detailStatus.setForeground(GREEN);
        } else if (isOverdue(t)) {
            detailStatus.setText("⚠  Overdue");
            detailStatus.setForeground(OVERDUE);
        } else {
            detailStatus.setText("○  Pending");
            detailStatus.setForeground(AMBER);
        }

        detailNotes.setText(t.notes.isEmpty()
            ? "<html><i style='color:#505470'>No notes</i></html>"
            : "<html>" + esc(t.notes) + "</html>");

        detailCompleteBtn.setEnabled(!t.completed);
        detailDeleteBtn.setEnabled(true);
    }

    // ── Keyboard Shortcuts ────────────────────────────────────────────────────
    void setupKeyboard() {
        JRootPane r = frame.getRootPane();
        kb(r,"C",     KeyEvent.VK_C, 0,                            this::markCompleted);
        kb(r,"E",     KeyEvent.VK_E, 0,                            this::editTask);
        kb(r,"DEL",   KeyEvent.VK_DELETE, 0,                       this::deleteTask);
        kb(r,"SAVE",  KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK,    this::saveTasks);
        kb(r,"NEW",   KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK,    ()->taskField.requestFocus());
        kb(r,"SEARCH",KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK,    ()->searchField.requestFocus());
    }
    void kb(JRootPane r, String n, int k, int m, Runnable fn) {
        r.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(k,m),n);
        r.getActionMap().put(n, new AbstractAction(){ @Override public void actionPerformed(ActionEvent e){ fn.run(); } });
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    void addTask() {
        String desc = taskField.getText().trim();
        if (desc.isEmpty()) { shake(taskField); return; }
        String dl   = deadlineField.getText().trim();
        String note = notesField.getText().trim();
        if (dl.equals("Deadline  yyyy-MM-dd")) dl = "";
        if (note.equals("Notes…")) note = "";
        tasks.add(new Task(desc, dl,
            (String)priorityBox.getSelectedItem(),
            (String)categoryBox.getSelectedItem(),
            note, false));
        taskField.setText(""); deadlineField.setText(""); notesField.setText("");
        refreshList();
        taskField.requestFocus();
    }

    void deleteTask() {
        int idx = taskList.getSelectedIndex();
        if (idx < 0 || idx >= filteredTasks.size()) return;
        Task t = filteredTasks.get(idx);
        if (JOptionPane.showConfirmDialog(frame,
                "Delete  \""+t.description+"\" ?","Confirm Delete",
                JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            tasks.remove(t);
            refreshList();
        }
    }

    void markCompleted() {
        int idx = taskList.getSelectedIndex();
        if (idx >= 0 && idx < filteredTasks.size()) {
            filteredTasks.get(idx).completed = true;
            refreshList();
        }
    }

    void editTask() {
        int idx = taskList.getSelectedIndex();
        if (idx < 0 || idx >= filteredTasks.size()) return;
        Task t = filteredTasks.get(idx);

        JDialog dlg = new JDialog(frame,"Edit Task",true);
        dlg.setSize(430,370);
        dlg.setLocationRelativeTo(frame);
        dlg.getContentPane().setBackground(BG_CARD);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner,BoxLayout.Y_AXIS));
        inner.setBackground(BG_CARD);
        inner.setBorder(new EmptyBorder(24,24,24,24));

        JTextField dF  = styledField(t.description); dF.setText(t.description);
        JTextField dlF = styledField(t.deadline);    dlF.setText(t.deadline);
        JTextField nF  = styledField(t.notes);       nF.setText(t.notes);
        JComboBox<String> pB = styledCombo(new String[]{"High","Medium","Low"});
        pB.setSelectedItem(t.priority);
        JComboBox<String> cB = styledCombo(CATEGORIES);
        cB.setSelectedItem(t.category);

        inner.add(fg("Task",     dF));  inner.add(vgap(10));
        inner.add(fg("Deadline", dlF)); inner.add(vgap(10));
        inner.add(fg("Priority", pB));  inner.add(vgap(10));
        inner.add(fg("Category", cB));  inner.add(vgap(10));
        inner.add(fg("Notes",    nF));  inner.add(vgap(20));

        JButton save = pill("Save Changes", ACCENT, Color.WHITE);
        save.setAlignmentX(Component.CENTER_ALIGNMENT);
        save.addActionListener(ev -> {
            t.description = dF.getText().trim();
            t.deadline    = dlF.getText().trim();
            t.priority    = (String)pB.getSelectedItem();
            t.category    = (String)cB.getSelectedItem();
            t.notes       = nF.getText().trim();
            refreshList(); dlg.dispose();
        });
        inner.add(save);
        dlg.add(inner);
        dlg.setVisible(true);
    }

    void clearCompleted() {
        long n = tasks.stream().filter(t->t.completed).count();
        if (n==0){ JOptionPane.showMessageDialog(frame,"No completed tasks.","Info",JOptionPane.INFORMATION_MESSAGE); return; }
        if (JOptionPane.showConfirmDialog(frame,"Remove "+n+" completed task"+(n>1?"s":"")+"?",
                "Clear Completed",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
            tasks.removeIf(t->t.completed);
            refreshList();
        }
    }

    void saveTasks() {
        FileHandler.saveTasks(tasks);
        String old = statsLabel.getText();
        statsLabel.setText(old + "  ✓ Saved");
        javax.swing.Timer t = new javax.swing.Timer(2000, e -> refreshStats());
        t.setRepeats(false); t.start();
    }

    void onClose() {
        if (autosave) FileHandler.saveTasks(tasks);
        System.exit(0);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    void refreshList() {
        String q  = searchField   != null ? searchField.getText().toLowerCase()   : "";
        String st = filterStatus  != null ? (String)filterStatus.getSelectedItem() : "All";
        String so = sortBox       != null ? (String)sortBox.getSelectedItem()      : "Date Added";

        filteredTasks = tasks.stream()
            .filter(t -> q.isEmpty() ||
                t.description.toLowerCase().contains(q) ||
                t.category.toLowerCase().contains(q)    ||
                t.notes.toLowerCase().contains(q))
            .filter(t -> switch(st) {
                case "Pending"   -> !t.completed;
                case "Completed" -> t.completed;
                case "Overdue"   -> isOverdue(t);
                default          -> true;
            })
            .sorted(sortComp(so))
            .collect(Collectors.toCollection(ArrayList::new));

        int sel = taskList != null ? taskList.getSelectedIndex() : -1;
        listModel.clear();
        for (Task t : filteredTasks) listModel.addElement(t.description);
        if (taskList!=null && sel>=0 && sel<listModel.size()) taskList.setSelectedIndex(sel);

        refreshStats();
        rebuildTiles();
        updateDetail();
    }

    void refreshStats() {
        long total = tasks.size(), done = tasks.stream().filter(t->t.completed).count();
        int pct = total==0 ? 0 : (int)(done*100/total);
        if (statsLabel   != null) statsLabel.setText(total+" tasks · "+(total-done)+" pending · "+done+" done");
        if (progressBar  != null) progressBar.setValue(pct);
        if (progressLabel!= null) progressLabel.setText(pct+"%");
    }

    void rebuildTiles() {
        if (statTilesRef==null) return;
        statTilesRef.removeAll();
        long tot  = tasks.size();
        long done = tasks.stream().filter(t->t.completed).count();
        long hi   = tasks.stream().filter(t->t.priority.equals("High")&&!t.completed).count();
        long ov   = tasks.stream().filter(this::isOverdue).count();
        statTilesRef.add(tile("Total",  ""+tot,  TEXT_2));
        statTilesRef.add(tile("Done",   ""+done, GREEN));
        statTilesRef.add(tile("Urgent", ""+hi,   RED));
        statTilesRef.add(tile("Overdue",""+ov,   OVERDUE));
        statTilesRef.revalidate(); statTilesRef.repaint();
    }

    JPanel tile(String label, String val, Color vc) {
        JPanel t = new JPanel();
        t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));
        t.setBackground(BG_CARD);
        t.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER,1),new EmptyBorder(7,9,7,9)));
        JLabel v = new JLabel(val);
        v.setFont(font(20).deriveFont(Font.BOLD)); v.setForeground(vc); v.setAlignmentX(0);
        JLabel l = new JLabel(label);
        l.setFont(font(9)); l.setForeground(TEXT_3); l.setAlignmentX(0);
        t.add(v); t.add(l);
        return t;
    }

    Comparator<Task> sortComp(String s) {
        return switch(s) {
            case "Priority" -> Comparator.comparingInt(t -> priRank(t.priority));
            case "Deadline" -> Comparator.comparing(t -> t.deadline.isEmpty()?"9999":t.deadline);
            case "Category" -> Comparator.comparing(t -> t.category);
            case "Status"   -> Comparator.comparingInt(t -> t.completed?1:0);
            default         -> (a,b) -> 0;
        };
    }

    int priRank(String p) { return switch(p){ case "High"->0; case "Medium"->1; default->2; }; }

    boolean isOverdue(Task t) {
        if (t.completed || t.deadline==null || t.deadline.isEmpty()) return false;
        try { return LocalDate.parse(t.deadline,DT_FMT).isBefore(LocalDate.now()); }
        catch (Exception e) { return false; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    JPanel fg(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0,4));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,56));
        JLabel l = new JLabel(label);
        l.setFont(font(9).deriveFont(Font.BOLD)); l.setForeground(TEXT_3);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
        p.add(l,BorderLayout.NORTH); p.add(comp,BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    JTextField styledField(String ph) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2=(Graphics2D)g; g2.setColor(TEXT_3);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    g2.drawString(ph,9,getHeight()/2+5);
                }
            }
        };
        f.setBackground(BG_INPUT); f.setForeground(TEXT_1); f.setCaretColor(ACCENT);
        f.setFont(font(12));
        f.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER,1),new EmptyBorder(4,8,4,8)));
        f.addFocusListener(new FocusAdapter(){
            @Override
            public void focusGained(FocusEvent e){ f.setBorder(new CompoundBorder(BorderFactory.createLineBorder(ACCENT,1),new EmptyBorder(4,8,4,8))); }
            @Override
            public void focusLost(FocusEvent e)  { f.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER,1),new EmptyBorder(4,8,4,8))); }
        });
        return f;
    }

    <T> JComboBox<T> styledCombo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setBackground(BG_INPUT); c.setForeground(TEXT_1); c.setFont(font(12));
        c.setBorder(BorderFactory.createLineBorder(BORDER,1));
        c.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list,Object v,int i,boolean s,boolean f){
                super.getListCellRendererComponent(list,v,i,s,f);
                setBackground(s?ACCENT:BG_INPUT); setForeground(TEXT_1);
                setBorder(new EmptyBorder(4,8,4,8)); return this;
            }
        });
        return c;
    }

    JButton pill(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            boolean h=false;
            { addMouseListener(new MouseAdapter(){
                @Override
                public void mouseEntered(MouseEvent e){h=true; repaint();}
                @Override
                public void mouseExited(MouseEvent e) {h=false;repaint();}
            });}
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = !isEnabled() ? bg.darker() : h ? bg.brighter() : bg;
                g2.setColor(c);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(isEnabled()?fg:fg.darker()); g2.setFont(getFont());
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        b.setFont(font(12).deriveFont(Font.BOLD));
        b.setPreferredSize(new Dimension(130,34));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JButton iconBtn(String text, Color bg, String tip) {
        JButton b = pill(text, bg, Color.WHITE);
        b.setPreferredSize(new Dimension(36,32));
        b.setMaximumSize(new Dimension(36,32));
        b.setToolTipText(tip);
        return b;
    }

    JButton ghostBtn(String text) {
        JButton b = pill(text, BG_INPUT, TEXT_2);
        b.setPreferredSize(new Dimension(130,30));
        return b;
    }

    JLabel secLabel(String s) {
        JLabel l = new JLabel(s);
        l.setFont(font(9).deriveFont(Font.BOLD));
        l.setForeground(TEXT_3);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    JSeparator divider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    Component vgap(int h){ return Box.createRigidArea(new Dimension(0,h)); }

    void styleScrollbar(JScrollBar sb) {
        sb.setBackground(BG_SIDEBAR);
        sb.setPreferredSize(new Dimension(6,0));
        sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors(){ thumbColor=new Color(0x38,0x3C,0x58); trackColor=BG_SIDEBAR; }
            @Override
            protected JButton createDecreaseButton(int o){ return zBtn(); }
            @Override
            protected JButton createIncreaseButton(int o){ return zBtn(); }
            JButton zBtn(){ JButton b=new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        });
    }

    void shake(Component c) {
        Point o = c.getLocation();
        int[] dx={-7,7,-5,5,-3,3,0}; int[] i={0};
        javax.swing.Timer t = new javax.swing.Timer(28,null);
        t.addActionListener(e->{
            if(i[0]>=dx.length){t.stop();c.setLocation(o);return;}
            c.setLocation(o.x+dx[i[0]++],o.y);
        });
        t.start();
    }

    Font font(float sz){ return new Font("Segoe UI",Font.PLAIN,(int)sz); }
    String esc(String s){ return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }

    // ── Cell Renderer ─────────────────────────────────────────────────────────
    class TaskRenderer extends JPanel implements ListCellRenderer<String> {
        JPanel   bar;
        JLabel   main, sub, badge;

        TaskRenderer() {
            setLayout(new BorderLayout(0,0));
            setBorder(new EmptyBorder(0,0,0,0));

            bar = new JPanel();
            bar.setPreferredSize(new Dimension(4,0));

            JPanel content = new JPanel(new BorderLayout(12,0));
            content.setOpaque(false);
            content.setBorder(new EmptyBorder(7,12,7,12));

            JPanel text = new JPanel(new GridLayout(2,1,0,3));
            text.setOpaque(false);
            main = new JLabel(); main.setFont(font(13).deriveFont(Font.BOLD));
            sub  = new JLabel(); sub.setFont(font(10));
            text.add(main); text.add(sub);

            badge = new JLabel();
            badge.setFont(font(11).deriveFont(Font.BOLD));
            badge.setHorizontalAlignment(SwingConstants.RIGHT);
            badge.setPreferredSize(new Dimension(28,28));

            content.add(text,  BorderLayout.CENTER);
            content.add(badge, BorderLayout.EAST);
            add(bar,     BorderLayout.WEST);
            add(content, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends String> list, String value,
                int idx, boolean sel, boolean foc) {

            if (idx >= 0 && idx < filteredTasks.size()) {
                Task t = filteredTasks.get(idx);
                boolean ov = isOverdue(t);

                Color barColor = t.completed ? TEXT_3
                    : ov ? OVERDUE
                    : switch(t.priority){ case "High"->RED; case "Medium"->AMBER; default->GREEN; };
                bar.setBackground(barColor);

                main.setText(t.completed
                    ? "<html><strike style='color:#50547090'>"+esc(t.description)+"</strike></html>"
                    : t.description);
                main.setForeground(t.completed ? TEXT_3 : TEXT_1);

                String s = t.category;
                if (!t.deadline.isEmpty()) { s += "  ·  📅 "+t.deadline; if(ov) s+=" ⚠"; }
                if (!t.notes.isEmpty())      s += "  ·  "+t.notes;
                sub.setText(s);
                sub.setForeground(ov ? OVERDUE : TEXT_2);

                badge.setText(t.completed ? "✓" : "");
                badge.setForeground(GREEN);
            }

            Color bg = sel
                ? new Color(ACCENT.getRed(),ACCENT.getGreen(),ACCENT.getBlue(),50)
                : idx%2==0 ? BG_ROOT : new Color(0x10,0x12,0x1C);
            setBackground(bg); bar.setOpaque(true); setOpaque(true);
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TodoGUI::new);
    }
}