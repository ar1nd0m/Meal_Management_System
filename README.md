# Meal Management System
 
A desktop Java application for managing shared meal expenses in a mess/hostel/shared-living setup. It tracks how many meals each member eats, records shared expenses and individual contributions, and automatically calculates who owes what — with printable PDF reports.
 
## Features
 
- **User login** with role support (`LoginFrame` → `MainFrame`)
- **Student management** — add and list mess members
- **Meal tracking** — record meal counts *before* and *after* the fact for each student/date (the higher of the two is used as the final count, to reconcile early estimates with actual counts)
- **Expense tracking** — log shared expenses (groceries, bazar cost, etc.) with date and description
- **Contribution tracking ("Given")** — record money each student has paid in
- **Automatic meal-rate calculation** — total expenses ÷ total meals for a given date range
- **Balance report** — per-student breakdown of meals eaten, amount owed, amount paid, and running balance (settled / owes / overpaid), plus a list of students who haven't contributed yet
- **PDF export** of reports via Apache PDFBox
- **Custom Swing UI** with background image support
## Tech Stack
 
| Component | Technology |
|---|---|
| Language | Java 11 |
| UI | Java Swing |
| Database | MySQL (via JDBC, `mysql-connector-j`) |
| PDF generation | Apache PDFBox 2.0.27 |
| Logging | SLF4J |
| Build tool | Maven |
 
## Project Structure
 
```
Meal_Management_System/
├── pom.xml
├── src/main/java/com/mealapp/
│   ├── app.java                  # Entry point
│   ├── dao/                      # Data access (JDBC) layer
│   │   ├── ExpenseDAO.java
│   │   ├── GivenDAO.java
│   │   ├── MealDAO.java
│   │   ├── StudentDAO.java
│   │   └── UserDAO.java
│   ├── model/                    # Plain data models
│   │   ├── AfterMeal.java
│   │   ├── BeforeMeal.java
│   │   ├── Expense.java
│   │   ├── Given.java
│   │   ├── Student.java
│   │   └── User.java
│   ├── service/
│   │   └── ReportService.java     # Builds the meal/expense report
│   ├── ui/
│   │   ├── LoginFrame.java
│   │   └── MainFrame.java         # Main tabbed interface
│   └── util/
│       └── DBConnection.java      # JDBC connection helper
└── src/main/resources/
    ├── db.properties              # DB connection config
    └── images/background.png
schema.sql                          # MySQL schema (reconstructed from the DAO queries)
```
 
## Database Schema
 
The repo doesn't ship a schema file, so one (`schema.sql`) has been reconstructed from the exact tables/columns referenced in the DAO classes:
 
| Table | Purpose |
|---|---|
| `users` (id, username, password, role) | Login accounts |
| `students` (id, name) | Mess members |
| `expenses` (id, student_id *nullable*, amount, expense_date, description) | Shared expenses (bazar cost etc.) |
| `given` (id, student_id, amount, given_date) | Money each student has contributed |
| `before_meal_call` (id, student_id, number_of_meal, meal_date) | Meal count declared in advance |
| `after_meal_call` (id, student_id, number_of_meal, meal_date) | Meal count confirmed afterward — `ReportService` takes `MAX(before, after)` per student per day |
 
Run `schema.sql` against your MySQL server to create the database and tables before starting the app.
 
## Prerequisites
 
- JDK 11+
- Maven
- MySQL Server (a database, e.g. `meal_db`, with `users`, `student`, `expense`, `given`, and meal tables matching the DAO queries)
## Setup
 
1. Clone the repo:
```bash
   git clone git@github.com:ar1nd0m/Meal_Management_System.git
   cd Meal_Management_System
```
2. Create the database and tables by running the included schema:
```bash
   mysql -u root -p < schema.sql
```
3. Update `src/main/resources/db.properties` with your own database URL, username, and password:
```properties
   db.url=jdbc:mysql://localhost:3306/meal_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   db.username=your_username
   db.password=your_password
```
   > ⚠️ **Security note:** don't commit real credentials to source control. Consider adding `db.properties` to `.gitignore` and providing a `db.properties.example` template instead.
4. Build and run:
> set up database
```bash
   git clone
   mvn compile
   mvn exec:java -Dexec.mainClass="com.mealapp.app"
```
   (or open/run the project in NetBeans/IntelliJ, since `nbactions.xml` indicates it was built as a NetBeans project.)
 
## Usage
 
1. Log in through the login screen.
2. Use the tabs to manage **Students**, **Expenses**, **Before Meal** / **After Meal** counts, and **Given** (contributions).
3. Go to the **Reports** tab, pick a date range, and generate the balance report.
4. Export the report to PDF if needed.
5. Check the **Not Given Students** tab to see who still owes a contribution.
## License
 
No license specified yet — add one (e.g. MIT) if you intend for others to reuse this code.
