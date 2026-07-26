-- Meal Management System — MySQL schema
-- Reconstructed from the SQL used in the DAO classes
-- (com.mealapp.dao.*), since the repo does not ship a schema file.

CREATE DATABASE IF NOT EXISTS meal_db;
USE meal_db;

-- Login accounts (LoginFrame / UserDAO)
CREATE TABLE IF NOT EXISTS users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL DEFAULT 'user'
);

-- Mess members (StudentDAO)
CREATE TABLE IF NOT EXISTS students (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- Shared expenses, e.g. bazar cost (ExpenseDAO)
-- student_id is nullable: NULL means a general/manager expense not tied to one student
CREATE TABLE IF NOT EXISTS expenses (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    student_id   INT NULL,
    amount       DECIMAL(10,2) NOT NULL,
    expense_date DATE NOT NULL,
    description  VARCHAR(255),
    CONSTRAINT fk_expenses_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE SET NULL
);

-- Money contributed by each student (GivenDAO)
CREATE TABLE IF NOT EXISTS given (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    given_date DATE NOT NULL,
    CONSTRAINT fk_given_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE CASCADE
);

-- Meal counts recorded ahead of time, e.g. "I'll eat 2 meals today" (MealDAO)
CREATE TABLE IF NOT EXISTS before_meal_call (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    student_id     INT NOT NULL,
    number_of_meal INT NOT NULL,
    meal_date      DATE NOT NULL,
    CONSTRAINT fk_before_meal_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE CASCADE
);

-- Actual meal counts recorded afterward (MealDAO)
-- ReportService takes MAX(before, after) per student per day as the final count
CREATE TABLE IF NOT EXISTS after_meal_call (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    student_id     INT NOT NULL,
    number_of_meal INT NOT NULL,
    meal_date      DATE NOT NULL,
    CONSTRAINT fk_after_meal_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE CASCADE
);

-- Helpful indexes for the date-range queries used in ReportService
CREATE INDEX idx_expenses_date       ON expenses(expense_date);
CREATE INDEX idx_given_date          ON given(given_date);
CREATE INDEX idx_before_meal_date    ON before_meal_call(meal_date);
CREATE INDEX idx_after_meal_date     ON after_meal_call(meal_date);

-- Optional: seed a default admin login
-- Replace the password with a real hash/value before using this in practice.
-- INSERT INTO users (username, password, role) VALUES ('admin', 'changeme', 'admin');
