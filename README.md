# Library Management System (Console Application)

**Revature – Project 0**  
**Author:** Benjamin Tuley  
**Language:** Java  
**Interface:** Console (CLI)

---

## Overview

This project is a **console-based Library Management System** written in Java.  
It allows a user to manage **books**, **members**, and **loans** through a menu-driven command-line interface.

The application demonstrates:
- Layered architecture (Controller → Service → DAO)
- JDBC database access
- Input validation and error handling
- Logging with Logback
- Unit testing of business logic

---

## Features

### Book Management
- Add a new book
- View all books
- Find a book by ID
- Update book details
- Delete a book (if not currently loaned)

### Member Management
- Add a new member
- View all members
- Find a member by ID
- Update member details
- Delete a member

### Loan Management
- Check out a book to a member
- Return a book
- View all loans
- View active loans
- View loans by member
- View overdue loans

### System Behavior
- Prevents checking out a book that is already loaned
- Validates all user input before processing
- Handles invalid menu selections gracefully
- Runs continuously until the user chooses to exit

---

## Technology Stack

- **Java:** OpenJDK 25
- **Build Tool:** Maven
- **Database:** PostgreSQL
- **Persistence:** JDBC with DAO pattern
- **Logging:** Logback
- **Testing:** JUnit 5, Mockito

---

## Project Structure

```
src/main/java
├── app
│   ├── Main.java
│   └── DebugMain.java
│
├── controller
│   ├── MainMenuController.java
│   ├── BookController.java
│   ├── MemberController.java
│   └── LoanController.java
│
├── service
│   ├── BookService.java
│   ├── MemberService.java
│   ├── LoanService.java
│   ├── ServiceInterface.java
│   └── models
│       ├── Book.java
│       ├── Member.java
│       └── Loan.java
│
├── repository
│   ├── DAO
│   │   ├── BaseDAO.java
│   │   ├── BookDAO.java
│   │   ├── MemberDAO.java
│   │   └── LoanDAO.java
│   └── entities
│       ├── BookEntity.java
│       ├── MemberEntity.java
│       └── LoanEntity.java
│
└── util
    ├── DbConnectionUtil.java
    ├── InputUtil.java
    └── validators
        ├── BookValidator.java
        ├── MemberValidator.java
        ├── LoanValidator.java
        └── ValidationUtil.java
```

---

## Database Setup

### Required Tables
- `books`
- `members`
- `loans`

Each book represents **one physical copy**.  
A book is considered unavailable if it has an active loan (`return_date IS NULL`).

### Configuration
Database connection settings are stored in:

```
src/main/resources/database.properties
```

Make sure PostgreSQL is running and the database schema has been created before starting the application.

---

## Running the Application

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd Project_0-Library_Management_System
```

### 2. Build the Project
```bash
mvn clean package
```

### 3. Run the Application
```bash
mvn exec:java
```

Entry point:
```java
app.Main
```

---

## Using the Console App

### Main Menu
```
=== LIBRARY MANAGEMENT SYSTEM ===
1. Book Services
2. Member Services
3. Loan Services
0. Exit
```

---

## Logging

- Console and rolling file logging
- Logs stored in `logs/`
- Configured via `src/main/resources/logback.xml`

---

## Testing

Run all tests with:
```bash
mvn test
```

---

## Future Enhancements

- Loan limits per member
- Late fee calculation
- Search by title or author
- Pagination
- Report exports
