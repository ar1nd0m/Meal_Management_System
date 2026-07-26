package com.mealapp.service;

import com.mealapp.dao.*;
import com.mealapp.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class ReportService {
    private final MealDAO mealDAO = new MealDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final GivenDAO givenDAO = new GivenDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 2; // for currency/meal-rate

    private static String center(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text;
        int leftPadding = (width - text.length()) / 2;
        return " ".repeat(leftPadding) + text;
    }

    /**
     * Generates the meal report text for the given date range.
     * @param from
     */
    public String generateReport(LocalDate from, LocalDate to) throws Exception {
        if (from == null || to == null) throw new IllegalArgumentException("From and To dates are required.");
        if (from.isAfter(to)) throw new IllegalArgumentException("From date cannot be after To date.");

        List<Student> students = studentDAO.findAll();
        if (students == null || students.isEmpty()) return "No students found.";

        // Sort students by name for consistent output
        students.sort(Comparator.comparing(Student::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

        // Map studentId -> index for arrays
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < students.size(); i++) idx.put(students.get(i).getId(), i);

        int n = students.size();
        int days = (int) ChronoUnit.DAYS.between(from, to) + 1;
        days = Math.max(days, 1);

        int[] totalPerStudent = new int[n];
        int totalMeals = 0;

        // Build date->student->count maps
        Map<LocalDate, Map<Integer, Integer>> beforeMap = new HashMap<>();
        Map<LocalDate, Map<Integer, Integer>> afterMap = new HashMap<>();

        for (BeforeMeal b : mealDAO.listBeforeBetween(from, to)) {
            beforeMap.computeIfAbsent(b.getMealDate(), d -> new HashMap<>())
                     .put(b.getStudentId(), b.getNumberOfMeal());
        }

        for (AfterMeal a : mealDAO.listAfterBetween(from, to)) {
            afterMap.computeIfAbsent(a.getMealDate(), d -> new HashMap<>())
                    .put(a.getStudentId(), a.getNumberOfMeal());
        }

        // compute final meals (max of before/after per day)
        for (int d = 0; d < days; d++) {
            LocalDate day = from.plusDays(d);
            Map<Integer, Integer> bmap = beforeMap.getOrDefault(day, Collections.emptyMap());
            Map<Integer, Integer> amap = afterMap.getOrDefault(day, Collections.emptyMap());

            for (Student s : students) {
                int sid = s.getId();
                int bcount = bmap.getOrDefault(sid, 0);
                int acount = amap.getOrDefault(sid, 0);
                int finalMeal = Math.max(bcount, acount);
                totalPerStudent[idx.get(sid)] += finalMeal;
                totalMeals += finalMeal;
            }
        }

        BigDecimal totalExpenses = Optional.ofNullable(expenseDAO.sumBetween(from, to)).orElse(BigDecimal.ZERO);
        Map<Integer, BigDecimal> givenPerStudent = Optional.ofNullable(givenDAO.sumByStudentBetween(from, to))
                                                           .orElse(Collections.emptyMap());

        BigDecimal mealRate = BigDecimal.ZERO;
        if (totalMeals > 0) {
            mealRate = totalExpenses.divide(BigDecimal.valueOf(totalMeals), SCALE, ROUNDING);
        }

        // formatting tools
        NumberFormat currencyFmt = NumberFormat.getCurrencyInstance(); // uses default locale
        currencyFmt.setMinimumFractionDigits(2);
        currencyFmt.setMaximumFractionDigits(2);

        NumberFormat numberFmt = NumberFormat.getNumberInstance();
        numberFmt.setMinimumFractionDigits(2);
        numberFmt.setMaximumFractionDigits(2);

        // prepare dynamic column widths
        int nameColWidth = Math.max(10, students.stream()
                .map(s -> s.getName() == null ? 0 : s.getName().length())
                .max(Integer::compareTo).orElse(10)) + 2;

        int width = Math.max(80, nameColWidth + 60); // overall width for centering header

        StringBuilder report = new StringBuilder();

        // Header
        report.append(center("=== Meal Report ===", width)).append("\n");
        report.append(center("From: " + from.format(DATE_FMT) + "    To: " + to.format(DATE_FMT), width)).append("\n");
        report.append(center("Total Expenses: " + currencyFmt.format(totalExpenses) +
                             "    Total Meals: " + totalMeals +
                             "    Meal Rate: " + currencyFmt.format(mealRate), width)).append("\n\n");

        // Table header
        String hdrFmt = String.format(" %%-%ds | %8s | %10s | %12s | %12s | %12s | %8s%n",
                nameColWidth, "Meals", "Rate", "Should Pay", "Paid", "Balance", "Avg/Day");
        String sep = "-".repeat(Math.min(width, nameColWidth + 80)) + "\n";

        report.append(sep);
        report.append(String.format(hdrFmt,
                "Student", " ", " ", " ", " ", " ", " "));
        report.append(sep);

        // Rows
        int grandMeals = 0;
        BigDecimal grandShould = BigDecimal.ZERO;
        BigDecimal grandPaid = BigDecimal.ZERO;

        for (int i = 0; i < n; i++) {
            Student s = students.get(i);
            String name = s.getName() == null ? ("ID " + s.getId()) : s.getName();
            int meals = totalPerStudent[i];
            grandMeals += meals;

            BigDecimal shouldPay = mealRate.multiply(BigDecimal.valueOf(meals)).setScale(SCALE, ROUNDING);
            BigDecimal paid = givenPerStudent.getOrDefault(s.getId(), BigDecimal.ZERO).setScale(SCALE, ROUNDING);
            BigDecimal balance = paid.subtract(shouldPay).setScale(SCALE, ROUNDING);
            BigDecimal avgPerDay = days > 0 ? BigDecimal.valueOf(meals).divide(BigDecimal.valueOf(days), 2, ROUNDING) : BigDecimal.ZERO;

            grandShould = grandShould.add(shouldPay);
            grandPaid = grandPaid.add(paid);

            report.append(String.format(" %-" + nameColWidth + "s | %8d | %10s | %12s | %12s | %12s | %8s%n",
                    name,
                    meals,
                    currencyFmt.format(mealRate),
                    currencyFmt.format(shouldPay),
                    currencyFmt.format(paid),
                    (balance.compareTo(BigDecimal.ZERO) == 0 ? "settled" : (balance.compareTo(BigDecimal.ZERO) > 0 ? ("+" + currencyFmt.format(balance)) : ("-" + currencyFmt.format(balance.abs())))),
                    numberFmt.format(avgPerDay)
            ));
        }

        // Totals line
        report.append(sep);
        report.append(String.format(" %-" + nameColWidth + "s | %8d | %10s | %12s | %12s | %12s | %8s%n",
                "TOTAL",
                grandMeals,
                currencyFmt.format(mealRate),
                currencyFmt.format(grandShould.setScale(SCALE, ROUNDING)),
                currencyFmt.format(grandPaid.setScale(SCALE, ROUNDING)),
                currencyFmt.format(grandPaid.subtract(grandShould).setScale(SCALE, ROUNDING)),
                numberFmt.format(grandMeals > 0 ? BigDecimal.valueOf(grandMeals).divide(BigDecimal.valueOf(days), 2, ROUNDING) : BigDecimal.ZERO)
        ));

        // non-contributors list (if any)
        List<Integer> nonContributors = givenDAO.studentsNotContributed(from, to);
        if (nonContributors != null && !nonContributors.isEmpty()) {
            report.append("\nStudents who didn't contribute:\n");
            for (int sid : nonContributors) {
                Student s = studentDAO.findById(sid);
                report.append(" - ").append(s != null ? s.getName() : ("ID " + sid)).append("\n");
            }
        }

        // summary note and footer
        report.append("\nReport generated on: ").append(LocalDate.now().format(DATE_FMT)).append("\n");

        return report.toString();
    }
}
