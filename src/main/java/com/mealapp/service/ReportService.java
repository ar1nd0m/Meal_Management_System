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

        // Map studentId -> index for arrays, and studentId -> Student for O(1) lookups
        // later (avoids re-querying the DB for names we already have in memory).
        Map<Integer, Integer> idx = new HashMap<>();
        Map<Integer, Student> studentById = new HashMap<>();
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            idx.put(s.getId(), i);
            studentById.put(s.getId(), s);
        }

        int n = students.size();
        int days = (int) ChronoUnit.DAYS.between(from, to) + 1;
        days = Math.max(days, 1);

        int[] totalPerStudent = new int[n];
        int totalMeals = 0;

        // ===== Optimized calculation =====
        // Instead of Map<LocalDate, Map<Integer, Integer>> (a hash map of hash maps,
        // with every count boxed as an Integer), we key each day to a plain int[]
        // sized to the student count and slot values in directly via the
        // precomputed idx map. This avoids per-student hashing/boxing entirely
        // while keeping the same sparse-by-day memory footprint as before (a day
        // with no recorded meals still costs nothing).
        Map<LocalDate, int[]> beforeMap = new HashMap<>();
        Map<LocalDate, int[]> afterMap = new HashMap<>();

        for (BeforeMeal b : mealDAO.listBeforeBetween(from, to)) {
            Integer sIdx = idx.get(b.getStudentId());
            if (sIdx == null) continue; // defensive: ignore rows for students no longer in the list
            beforeMap.computeIfAbsent(b.getMealDate(), d -> new int[n])[sIdx] = b.getNumberOfMeal();
        }

        for (AfterMeal a : mealDAO.listAfterBetween(from, to)) {
            Integer sIdx = idx.get(a.getStudentId());
            if (sIdx == null) continue;
            afterMap.computeIfAbsent(a.getMealDate(), d -> new int[n])[sIdx] = a.getNumberOfMeal();
        }

        // compute final meals (max of before/after per day) — same result as
        // before, just reading from int[] instead of unboxing HashMap<Integer,Integer>.
        for (int d = 0; d < days; d++) {
            LocalDate day = from.plusDays(d);
            int[] barr = beforeMap.get(day);
            int[] aarr = afterMap.get(day);
            if (barr == null && aarr == null) continue; // nothing recorded this day at all

            for (int i = 0; i < n; i++) {
                int bcount = barr != null ? barr[i] : 0;
                int acount = aarr != null ? aarr[i] : 0;
                int finalMeal = Math.max(bcount, acount);
                totalPerStudent[i] += finalMeal;
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
        // Optimized: reuse the Student objects we already loaded via studentById
        // instead of calling studentDAO.findById(sid) once per non-contributor
        // (that was an N+1 query — one extra round trip to the DB per student).
        List<Integer> nonContributors = givenDAO.studentsNotContributed(from, to);
        if (nonContributors != null && !nonContributors.isEmpty()) {
            List<String> names = new ArrayList<>(nonContributors.size());
            for (int sid : nonContributors) {
                Student s = studentById.get(sid);
                names.add(s != null ? s.getName() : ("ID " + sid));
            }
            names.sort(Comparator.nullsLast(String::compareToIgnoreCase));

            report.append("\nStudents who didn't contribute:\n");
            for (String name : names) {
                report.append(" - ").append(name).append("\n");
            }
        }

        // summary note and footer
        report.append("\nReport generated on: ").append(LocalDate.now().format(DATE_FMT)).append("\n");

        return report.toString();
    }
}
