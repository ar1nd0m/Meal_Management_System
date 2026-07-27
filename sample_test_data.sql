-- Sample test data for the Meal Management System
-- Run this AFTER the schema script you already have.
-- Covers 2026-07-01 through 2026-07-05 so you can test the
-- Reports tab and the "Not Given Students" tab with a real date range.

USE meal_db;

-- ===== Students =====
INSERT INTO students (id, name) VALUES
    (1, 'Rahim'),
    (2, 'Karim'),
    (3, 'Habib'),
    (4, 'Anik'),
    (5, 'Tanvir');

-- ===== Contributions (given) =====
-- Everyone contributes on day 1; Tanvir intentionally skips the whole
-- period so he shows up under "Not Given Students" for 2026-07.
INSERT INTO given (student_id, amount, given_date) VALUES
    (1, 1000.00, '2026-07-01'),
    (2, 1000.00, '2026-07-01'),
    (3,  800.00, '2026-07-01'),
    (4, 1000.00, '2026-07-01');

-- ===== Expenses (bazar / shared costs) =====
-- Some rows tied to the student who did the bazar, some general
-- (student_id NULL) for the household as a whole.
INSERT INTO expenses (student_id, amount, expense_date, description) VALUES
    (1, 1500.00, '2026-07-01', 'Rice and vegetables'),
    (NULL, 800.50, '2026-07-02', 'Fish'),
    (3,  650.00, '2026-07-03', 'Spices and oil'),
    (2, 1200.00, '2026-07-04', 'Chicken'),
    (NULL, 300.00, '2026-07-05', 'Egg and bread');

-- ===== Before-meal calls (declared ahead of time) =====
INSERT INTO before_meal_call (student_id, number_of_meal, meal_date) VALUES
    (1, 2, '2026-07-01'),
    (2, 2, '2026-07-01'),
    (4, 3, '2026-07-01'),
    -- Habib and Tanvir did NOT call ahead on day 1

    (1, 2, '2026-07-02'),
    (2, 1, '2026-07-02'),
    (3, 2, '2026-07-02'),
    (4, 2, '2026-07-02'),
    (5, 2, '2026-07-02'),

    (1, 2, '2026-07-03'),
    (2, 2, '2026-07-03'),
    (3, 0, '2026-07-03'),
    (4, 2, '2026-07-03'),
    -- Tanvir skips calling on day 3

    (1, 2, '2026-07-04'),
    (2, 2, '2026-07-04'),
    (3, 2, '2026-07-04'),
    (4, 2, '2026-07-04'),
    (5, 3, '2026-07-04'),

    (1, 2, '2026-07-05'),
    (2, 2, '2026-07-05'),
    (3, 2, '2026-07-05'),
    (4, 2, '2026-07-05'),
    (5, 2, '2026-07-05');

-- ===== After-meal calls (actual count, recorded afterward) =====
-- Deliberately mismatched with "before" on a few rows so you can confirm
-- ReportService is taking MAX(before, after) per student per day.
INSERT INTO after_meal_call (student_id, number_of_meal, meal_date) VALUES
    (1, 2, '2026-07-01'),
    (2, 1, '2026-07-01'),   -- less than before (2) -> MAX should be 2
    (3, 2, '2026-07-01'),   -- no before entry -> MAX should be 2
    (5, 2, '2026-07-01'),   -- no before entry -> MAX should be 2
    -- Anik has no after entry on day 1 -> MAX should fall back to before (3)

    (1, 2, '2026-07-02'),
    (2, 2, '2026-07-02'),   -- more than before (1) -> MAX should be 2
    (3, 2, '2026-07-02'),
    (4, 2, '2026-07-02'),
    (5, 2, '2026-07-02'),

    (1, 2, '2026-07-03'),
    (2, 2, '2026-07-03'),
    (3, 2, '2026-07-03'),   -- more than before (0) -> MAX should be 2
    (4, 2, '2026-07-03'),
    (5, 2, '2026-07-03'),   -- no before entry -> MAX should be 2

    (1, 2, '2026-07-04'),
    (2, 2, '2026-07-04'),
    (3, 2, '2026-07-04'),
    (4, 2, '2026-07-04'),
    (5, 3, '2026-07-04'),

    (1, 2, '2026-07-05'),
    (2, 2, '2026-07-05'),
    (3, 2, '2026-07-05'),
    (4, 2, '2026-07-05'),
    (5, 2, '2026-07-05');

-- ===== Expected quick-check numbers for 2026-07-01 to 2026-07-05 =====
-- Total expenses  : 1500.00 + 800.50 + 650.00 + 1200.00 + 300.00 = 4450.50
-- Total given      : 1000.00 + 1000.00 + 800.00 + 1000.00 = 3800.00
-- Total meals (MAX(before, after) per student/day), summed across 5 students x 5 days:
--   Day 1: Rahim 2, Karim 2, Habib 2, Anik 3, Tanvir 2      = 11
--   Day 2: Rahim 2, Karim 2, Habib 2, Anik 2, Tanvir 2      = 10
--   Day 3: Rahim 2, Karim 2, Habib 2, Anik 2, Tanvir 2      = 10
--   Day 4: Rahim 2, Karim 2, Habib 2, Anik 2, Tanvir 3      = 11
--   Day 5: Rahim 2, Karim 2, Habib 2, Anik 2, Tanvir 2      = 10
--   Total meals = 52
-- Meal rate = Total expenses / Total meals = 4450.50 / 52 ≈ 85.59 per meal
--
-- "Not Given Students" for month 2026-07 should return: Tanvir (id 5)
