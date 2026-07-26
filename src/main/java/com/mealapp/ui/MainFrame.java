package com.mealapp.ui;

import com.mealapp.dao.*;
import com.mealapp.model.*;
import com.mealapp.service.ReportService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.plaf.FontUIResource;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;

public class MainFrame extends JFrame {

    // UI container that supports background image painting
    private final BackgroundPanel content = new BackgroundPanel();

    private final JTabbedPane tabs = new JTabbedPane();

    // Students
    private JTable tblStudents;
    private DefaultTableModel studentModel;
    private final JTextField txtStudentName = new JTextField(15);

    // Expenses
    private JTable tblExpenses;
    private DefaultTableModel expenseModel;
    private final JTextField txtAmount = new JTextField(8);
    private final JTextField txtDate = new JTextField(10);
    private final JTextField txtDesc = new JTextField(15);

    // Meals
    private JTable tblBefore, tblAfter;
    private DefaultTableModel beforeModel, afterModel;
    private JComboBox<Student> cmbStudentBefore, cmbStudentAfter;
    private JTextField txtBeforeMeal, txtAfterMeal;
    private JTextField txtBeforeDate, txtAfterDate;

    // Given
    private JTable tblGiven;
    private DefaultTableModel givenModel;
    private JComboBox<Student> cmbStudentGiven;
    private JTextField txtGivenAmount, txtGivenDate;

    // Reports
    private final ReportService reportService = new ReportService();

    public MainFrame(User user) {
        super("Meal Management System");

        // Increase global UI font sizes (applies to most Swing controls)
        setGlobalFont(new Font("SansSerif", Font.PLAIN, 14));

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screen);
        setLocation(0, 0);

        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // use our background panel as content pane
        setContentPane(content);
        content.setLayout(new BorderLayout());
        content.add(tabs, BorderLayout.CENTER);

        // Initialize tabs
        initStudentsTab();
        initExpensesTab();
        initBeforeMealTab();
        initAfterMealTab();
        initGivenTab();
        initReportsTab();
        initNotGivenTab();

        // Try to auto-load background.jpg from working dir if exists
   // in MainFrame.java (or whatever class)



        // Clean shutdown on close (if you need to release DB resources)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // if any cleanup required, do here.
                dispose();
            }
        });
    }

    // ===== Helper: set global font =====
    private static void setGlobalFont(Font font) {
        FontUIResource fr = new FontUIResource(font);
        for (Object k : UIManager.getDefaults().keySet()) {
            if (k != null && k.toString().toLowerCase().contains("font")) {
                UIManager.put(k, fr);
            }
        }
    }

    // ===== STUDENTS TAB =====
    private void initStudentsTab() {
        JPanel panel = transparentPanel(new BorderLayout());
        studentModel = new DefaultTableModel(new Object[]{"ID", "Name"}, 0);
        tblStudents = new JTable(studentModel);
        JScrollPane sp = new JScrollPane(tblStudents);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);

        JButton btnAdd = new JButton("Add");
        JButton btnDelete = new JButton("Delete");

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.add(new JLabel("Name:"));
        top.add(txtStudentName);
        top.add(btnAdd);
        top.add(btnDelete);

        panel.add(top, BorderLayout.NORTH);
        tabs.addTab("Students", panel);

        btnAdd.addActionListener(e -> addStudent());
        btnDelete.addActionListener(e -> deleteStudent());

        loadStudents();
    }

    private void loadStudents() {
        try {
            studentModel.setRowCount(0);
            StudentDAO dao = new StudentDAO();
            for (Student s : dao.findAll()) {
                studentModel.addRow(new Object[]{s.getId(), s.getName()});
            }
            // refresh comboboxes safely
            safeRefreshMealStudentCombos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addStudent() {
        try {
            String name = txtStudentName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a student name.");
                return;
            }
            new StudentDAO().create(new Student(0, name));
            txtStudentName.setText("");
            loadStudents();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteStudent() {
        int row = tblStudents.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a student to delete.");
            return;
        }
        int id = (int) studentModel.getValueAt(row, 0);
        try {
            new StudentDAO().delete(id);
            loadStudents();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Refreshes all student combo boxes but handles cases where combos are not yet created.
     */
    private void safeRefreshMealStudentCombos() {
        try {
            List<Student> all = new StudentDAO().findAll();
            if (cmbStudentBefore != null) {
                cmbStudentBefore.removeAllItems();
                for (Student s : all) cmbStudentBefore.addItem(s);
            }
            if (cmbStudentAfter != null) {
                cmbStudentAfter.removeAllItems();
                for (Student s : all) cmbStudentAfter.addItem(s);
            }
            if (cmbStudentGiven != null) {
                cmbStudentGiven.removeAllItems();
                for (Student s : all) cmbStudentGiven.addItem(s);
            }
        } catch (Exception e) {
            // Non-fatal: show message but continue
            System.err.println("Warning: could not refresh student combos: " + e.getMessage());
        }
    }

    // ===== EXPENSES TAB =====
    private void initExpensesTab() {
        JPanel panel = transparentPanel(new BorderLayout());
        expenseModel = new DefaultTableModel(new Object[]{"ID", "Amount", "Date", "Description"}, 0);
        tblExpenses = new JTable(expenseModel);
        JScrollPane sp = new JScrollPane(tblExpenses);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);

        JButton btnAdd = new JButton("Add");
        JButton btnDelete = new JButton("Delete");

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.add(new JLabel("Amount:"));
        top.add(txtAmount);
        top.add(new JLabel("Date (YYYY-MM-DD):"));
        top.add(txtDate);
        top.add(new JLabel("Desc:"));
        top.add(txtDesc);
        top.add(btnAdd);
        top.add(btnDelete);

        panel.add(top, BorderLayout.NORTH);
        tabs.addTab("Expenses", panel);

        btnAdd.addActionListener(e -> addExpense());
        btnDelete.addActionListener(e -> deleteExpense());

        loadExpenses();
    }

    private void loadExpenses() {
        try {
            expenseModel.setRowCount(0);
            ExpenseDAO dao = new ExpenseDAO();
            for (Expense ex : dao.findAll()) {
                expenseModel.addRow(new Object[]{
                        ex.getId(),
                        ex.getAmount(),
                        ex.getExpenseDate(),
                        ex.getDescription()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addExpense() {
        try {
            if (txtAmount.getText().trim().isEmpty() || txtDate.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter amount and date.");
                return;
            }
            Expense e = new Expense();
            e.setAmount(new BigDecimal(txtAmount.getText().trim()));
            e.setExpenseDate(LocalDate.parse(txtDate.getText().trim()));
            e.setDescription(txtDesc.getText().trim());

            new ExpenseDAO().insert(e);
            loadExpenses();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid amount.");
        } catch (java.time.format.DateTimeParseException dtpe) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteExpense() {
        int row = tblExpenses.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an expense to delete.");
            return;
        }
        int id = (int) expenseModel.getValueAt(row, 0);
        try {
            new ExpenseDAO().delete(id);
            loadExpenses();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== BEFORE MEAL TAB =====
    private void initBeforeMealTab() {
        JPanel panel = transparentPanel(new BorderLayout());
        beforeModel = new DefaultTableModel(new Object[]{"ID", "Student", "Meals", "Date"}, 0);
        tblBefore = new JTable(beforeModel);
        JScrollPane sp = new JScrollPane(tblBefore);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);

        cmbStudentBefore = new JComboBox<>();
        setStudentComboRenderer(cmbStudentBefore);

        try {
            for (Student s : new StudentDAO().findAll()) cmbStudentBefore.addItem(s);
        } catch (Exception e) {
            // ignore now — will be refreshed later
        }

        txtBeforeMeal = new JTextField(5);
        txtBeforeDate = new JTextField(10);

        JButton btnAdd = new JButton("Add");
        JButton btnDelete = new JButton("Delete");

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.add(new JLabel("Student:"));
        top.add(cmbStudentBefore);
        top.add(new JLabel("Meals:"));
        top.add(txtBeforeMeal);
        top.add(new JLabel("Date (YYYY-MM-DD):"));
        top.add(txtBeforeDate);
        top.add(btnAdd);
        top.add(btnDelete);

        panel.add(top, BorderLayout.NORTH);
        tabs.addTab("Before Meal", panel);

        btnAdd.addActionListener(e -> addBeforeMeal());
        btnDelete.addActionListener(e -> deleteBeforeMeal());

        loadBeforeMeals();
    }

    private void loadBeforeMeals() {
        try {
            beforeModel.setRowCount(0);
            MealDAO dao = new MealDAO();
            StudentDAO studentDAO = new StudentDAO(); // To get student names
            for (BeforeMeal m : dao.listBeforeAll()) {
                Student s = studentDAO.findById(m.getStudentId()); // fetch student
                beforeModel.addRow(new Object[]{
                        m.getId(),
                        s != null ? s.getName() : "Unknown",
                        m.getNumberOfMeal(),
                        m.getMealDate()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addBeforeMeal() {
        try {
            Student s = (Student) cmbStudentBefore.getSelectedItem();
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Select a student.");
                return;
            }
            int meals = Integer.parseInt(txtBeforeMeal.getText().trim());
            LocalDate d = LocalDate.parse(txtBeforeDate.getText().trim());
            new MealDAO().saveBefore(s.getId(), meals, d);
            loadBeforeMeals();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid meals number.");
        } catch (java.time.format.DateTimeParseException dtpe) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteBeforeMeal() {
        int row = tblBefore.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record to delete.");
            return;
        }
        int id = (int) beforeModel.getValueAt(row, 0);
        try {
            new MealDAO().deleteBefore(id);
            loadBeforeMeals();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== AFTER MEAL TAB =====
    private void initAfterMealTab() {
        JPanel panel = transparentPanel(new BorderLayout());
        afterModel = new DefaultTableModel(new Object[]{"ID", "Student", "Meals", "Date"}, 0);
        tblAfter = new JTable(afterModel);
        JScrollPane sp = new JScrollPane(tblAfter);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);

        cmbStudentAfter = new JComboBox<>();
        setStudentComboRenderer(cmbStudentAfter);

        try {
            for (Student s : new StudentDAO().findAll()) cmbStudentAfter.addItem(s);
        } catch (Exception e) {
            // ignore
        }

        txtAfterMeal = new JTextField(5);
        txtAfterDate = new JTextField(10);

        JButton btnAdd = new JButton("Add");
        JButton btnDelete = new JButton("Delete");

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.add(new JLabel("Student:"));
        top.add(cmbStudentAfter);
        top.add(new JLabel("Meals:"));
        top.add(txtAfterMeal);
        top.add(new JLabel("Date (YYYY-MM-DD):"));
        top.add(txtAfterDate);
        top.add(btnAdd);
        top.add(btnDelete);

        panel.add(top, BorderLayout.NORTH);
        tabs.addTab("After Meal", panel);

        btnAdd.addActionListener(e -> addAfterMeal());
        btnDelete.addActionListener(e -> deleteAfterMeal());

        loadAfterMeals();
    }

    private void loadAfterMeals() {
        try {
            afterModel.setRowCount(0);
            MealDAO dao = new MealDAO();
            StudentDAO studentDAO = new StudentDAO();
            for (AfterMeal m : dao.listAfterAll()) {
                Student s = studentDAO.findById(m.getStudentId());
                afterModel.addRow(new Object[]{
                        m.getId(),
                        s != null ? s.getName() : "Unknown",
                        m.getNumberOfMeal(),
                        m.getMealDate()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addAfterMeal() {
        try {
            Student s = (Student) cmbStudentAfter.getSelectedItem();
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Select a student.");
                return;
            }
            int meals = Integer.parseInt(txtAfterMeal.getText().trim());
            LocalDate d = LocalDate.parse(txtAfterDate.getText().trim());
            new MealDAO().saveAfter(s.getId(), meals, d);
            loadAfterMeals();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid meals number.");
        } catch (java.time.format.DateTimeParseException dtpe) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAfterMeal() {
        int row = tblAfter.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record to delete.");
            return;
        }
        int id = (int) afterModel.getValueAt(row, 0);
        try {
            new MealDAO().deleteAfter(id);
            loadAfterMeals();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== GIVEN TAB =====
    private void initGivenTab() {
        JPanel panel = transparentPanel(new BorderLayout());
        givenModel = new DefaultTableModel(new Object[]{"ID", "Student", "Amount", "Date"}, 0);
        tblGiven = new JTable(givenModel);
        JScrollPane sp = new JScrollPane(tblGiven);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);

        cmbStudentGiven = new JComboBox<>();
        setStudentComboRenderer(cmbStudentGiven);

        try {
            for (Student s : new StudentDAO().findAll()) cmbStudentGiven.addItem(s);
        } catch (Exception e) {
            // ignore
        }

        txtGivenAmount = new JTextField(8);
        txtGivenDate = new JTextField(10);

        JButton btnAdd = new JButton("Add");
        JButton btnDelete = new JButton("Delete");

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.add(new JLabel("Student:"));
        top.add(cmbStudentGiven);
        top.add(new JLabel("Amount:"));
        top.add(txtGivenAmount);
        top.add(new JLabel("Date (YYYY-MM-DD):"));
        top.add(txtGivenDate);
        top.add(btnAdd);
        top.add(btnDelete);

        panel.add(top, BorderLayout.NORTH);
        tabs.addTab("Given", panel);

        btnAdd.addActionListener(e -> addGiven());
        btnDelete.addActionListener(e -> deleteGiven());

        loadGiven();
    }

    private void loadGiven() {
        try {
            givenModel.setRowCount(0);
            GivenDAO dao = new GivenDAO();
            StudentDAO studentDAO = new StudentDAO();
            for (Given g : dao.listBetween(LocalDate.of(2023,1,1), LocalDate.now())) {
                Student s = studentDAO.findById(g.getStudentId());
                givenModel.addRow(new Object[]{
                        g.getId(),
                        s != null ? s.getName() : "Unknown",
                        g.getAmount(),
                        g.getGivenDate()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addGiven() {
        try {
            Student s = (Student) cmbStudentGiven.getSelectedItem();
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Select a student.");
                return;
            }
            Given g = new Given();
            g.setStudentId(s.getId());
            g.setAmount(new BigDecimal(txtGivenAmount.getText().trim()));
            g.setGivenDate(LocalDate.parse(txtGivenDate.getText().trim()));
            new GivenDAO().insert(g);
            loadGiven();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid amount.");
        } catch (java.time.format.DateTimeParseException dtpe) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteGiven() {
        int row = tblGiven.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record to delete.");
            return;
        }
        int id = (int) givenModel.getValueAt(row, 0);
        try {
            new GivenDAO().delete(id);
            loadGiven();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== REPORTS TAB =====
    private void initReportsTab() {
        JPanel panel = transparentPanel(new BorderLayout());

        JTextArea txtReport = new JTextArea();
        txtReport.setEditable(false);
        JScrollPane sp = new JScrollPane(txtReport);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);

        JPanel top = new JPanel();
        top.setOpaque(false);

        top.add(new JLabel("From Date (YYYY-MM-DD):"));
        JTextField txtFromDate = new JTextField(10);
        top.add(txtFromDate);

        top.add(new JLabel("To Date (YYYY-MM-DD):"));
        JTextField txtToDate = new JTextField(10);
        top.add(txtToDate);

        JButton btnGenerate = new JButton("Generate Report");
        JButton btnExportPdf = new JButton("Export PDF");

        top.add(btnGenerate);
        top.add(btnExportPdf);

        panel.add(top, BorderLayout.NORTH);
        tabs.addTab("Reports", panel);

        // ===== Generate Report =====
        btnGenerate.addActionListener(e -> {
            try {
                String fromStr = txtFromDate.getText().trim();
                String toStr = txtToDate.getText().trim();

                if (fromStr.isEmpty() || toStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter both From and To dates.");
                    return;
                }

                LocalDate from = LocalDate.parse(fromStr);
                LocalDate to = LocalDate.parse(toStr);

                String report = reportService.generateReport(from, to);
                txtReport.setText(report);

            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error generating report: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // ===== Export PDF =====
        btnExportPdf.addActionListener(e -> {
            try {
                if (txtReport.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Generate report first.");
                    return;
                }

                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));

                if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if (!file.getName().toLowerCase().endsWith(".pdf")) {
                        file = new File(file.getAbsolutePath() + ".pdf");
                    }

                    generatePdfReport(txtReport.getText(), file);
                    JOptionPane.showMessageDialog(this, "PDF saved successfully.");
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "PDF Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
    }

    // ===== NOT GIVEN STUDENTS TAB =====
    private void initNotGivenTab() {
        JPanel panel = transparentPanel(new BorderLayout());

        // Table to display students
        DefaultTableModel notGivenModel = new DefaultTableModel(new Object[]{"ID", "Name"}, 0);
        JTable tblNotGiven = new JTable(notGivenModel);
        JScrollPane sp = new JScrollPane(tblNotGiven);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);

        // Top panel: input month
        JPanel top = new JPanel();
        top.setOpaque(false);
        JTextField txtMonth = new JTextField(7); // format YYYY-MM
        JButton btnShow = new JButton("Show");
        top.add(new JLabel("Month (YYYY-MM):"));
        top.add(txtMonth);
        top.add(btnShow);

        panel.add(top, BorderLayout.NORTH);
        tabs.addTab("Not Given Students", panel);

        // Button action
        btnShow.addActionListener(e -> {
            try {
                String monthStr = txtMonth.getText().trim();
                if (!monthStr.matches("\\d{4}-\\d{2}")) {
                    JOptionPane.showMessageDialog(this, "Invalid month format. Use YYYY-MM.");
                    return;
                }

                String[] parts = monthStr.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                LocalDate from = LocalDate.of(year, month, 1);
                LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

                GivenDAO givenDAO = new GivenDAO();
                StudentDAO studentDAO = new StudentDAO();

                List<Integer> nonContributors = givenDAO.studentsNotContributed(from, to);

                notGivenModel.setRowCount(0);
                for (int sid : nonContributors) {
                    Student s = studentDAO.findById(sid);
                    notGivenModel.addRow(new Object[]{s.getId(), s.getName()});
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ===== PDF helper =====
    private void generatePdfReport(String content, File file) throws Exception {
        // sanitize control characters (some fonts/encodings don't support them)
        String safeContent = content.replace("\t", "    ").replace("\r", "");

        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDFont font = PDType1Font.HELVETICA;
        float fontSize = 12; // slightly larger in PDF
        float leading = fontSize + 4;

        PDPageContentStream cs = new PDPageContentStream(document, page);

        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;
        float xStart = margin;

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(xStart, yStart);

        for (String line : safeContent.split("\n")) {
            // wrap long lines simply by splitting at 100 chars (naive but safe)
            String work = line;
            while (work.length() > 0) {
                int fit = estimateCharsFit(fontSize, font, page.getMediaBox().getWidth() - 2 * margin);
                String part;
                if (work.length() <= fit) {
                    part = work;
                    work = "";
                } else {
                    part = work.substring(0, fit);
                    // try to break at last space to avoid mid-word break
                    int lastSpace = part.lastIndexOf(' ');
                    if (lastSpace > fit / 2) {
                        part = part.substring(0, lastSpace);
                        work = work.substring(lastSpace + 1);
                    } else {
                        work = work.substring(fit);
                    }
                }

                // if we've run out of space on page, start new page
                if (yStart <= margin + leading) {
                    cs.endText();
                    cs.close();

                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    cs = new PDPageContentStream(document, page);
                    yStart = page.getMediaBox().getHeight() - margin;
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(xStart, yStart);
                }

                cs.showText(part);
                cs.newLineAtOffset(0, -leading);
                yStart -= leading;
            }
        }

        cs.endText();
        cs.close();

        document.save(file);
        document.close();
    }

    /**
     * Very rough estimate of characters that fit in the given width for the given font and fontSize.
     * This avoids complicated line breaking libraries — sufficient for typical reports.
     */
    private int estimateCharsFit(float fontSize, PDFont pdfFont, float availableWidth) {
        // approx width per char in points: fontSize * 0.5 (very rough)
        float approxCharWidth = fontSize * 0.5f;
        return Math.max(10, (int) (availableWidth / approxCharWidth));
    }

    // ===== Small helpers =====
    private JPanel transparentPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    private void setStudentComboRenderer(JComboBox<Student> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Student) {
                    Student s = (Student) value;
                    lbl.setText(s.getName());
                } else {
                    lbl.setText("");
                }
                return lbl;
            }
        });
    }

    /**
     * Expose a method to set background from a file at runtime.
     */
    public boolean setBackgroundFromFile(File imageFile) {
        try {
            BufferedImage img = ImageIO.read(imageFile);
            if (img == null) return false;
            content.setBackgroundImage(img);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===== BackgroundPanel class =====
    private static class BackgroundPanel extends JPanel {
        private Image background;

        public BackgroundPanel() {
            setLayout(new BorderLayout());
        }

        public void setBackgroundImage(Image img) {
            this.background = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (background != null) {
                int w = getWidth();
                int h = getHeight();
                // draw scaled to fill while keeping aspect ratio
                double imgW = background.getWidth(null);
                double imgH = background.getHeight(null);
                if (imgW <= 0 || imgH <= 0) {
                    g.drawImage(background, 0, 0, w, h, null);
                    return;
                }
                double scale = Math.max(w / imgW, h / imgH);
                int nw = (int) (imgW * scale);
                int nh = (int) (imgH * scale);
                int x = (w - nw) / 2;
                int y = (h - nh) / 2;
                g.drawImage(background, x, y, nw, nh, null);
            } else {
                // optional subtle tint to make text readable
                // keep default background
            }
        }
    }
}
