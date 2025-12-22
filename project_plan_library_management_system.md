# Library Management System (Console, Java)

## 1. Project Overview

### Goal
Build a console-based **Library Management System** in Java that demonstrates:

- Clear layered architecture (Controller → Service → Repository/DAO)
- Persistence using a relational SQL database
- A many-to-many relationship between Books and Members via Loans
- Strong separation of concerns and validation
- Unit testing of business logic with mocked data-access layers

This project is designed to meet **Revature Project 0** requirements and emphasizes clean architecture, testability, and maintainability.

### High-Level Features (Current Scope)

- Manage Books (add, list, basic update, optional delete)
- Manage Members (add, list, basic update, optional delete)
- Check out a Book to a Member (create Loan)
- Return a Book (close Loan)
- Prevent checkout if a Book copy is already checked out
- List all Loans
- List active Loans (checked out, not yet returned)
- List Loans by Member
- List overdue Loans
- Application runs continuously until user exits
- Robust console input validation and graceful error handling

### Assumptions

- Each Book row represents a single physical copy
- A Book is unavailable if it has an active Loan (`return_date IS NULL`)
- A Loan is overdue if:
  ```
  return_date IS NULL AND due_date < current_date
  ```

---

## 2. Technology Stack

- **Language:** Java (OpenJDK 25.0.1)
- **Build Tool:** Maven
- **Database:** PostgreSQL (via JDBC, configured in `src/main/resources/database.properties`)
- **Persistence:** DAO pattern with JDBC
- **Logging:** Logback (`src/main/resources/logback.xml`)
- **Testing:** JUnit 5, Mockito

---

## 3. Project Structure & Responsibilities

```
src
├── main
│   ├── java
│   │   ├── app
│   │   │   ├── Main
│   │   │   └── DebugMain
│   │   │
│   │   ├── controller
│   │   │   ├── MainMenuController
│   │   │   ├── BookController
│   │   │   ├── MemberController
│   │   │   └── LoanController
│   │   │
│   │   ├── repository
│   │   │   ├── DAO
│   │   │   │   ├── BaseDAO
│   │   │   │   ├── BookDAO
│   │   │   │   ├── MemberDAO
│   │   │   │   └── LoanDAO
│   │   │   │
│   │   │   ├── entities
│   │   │   │   ├── BookEntity
│   │   │   │   ├── MemberEntity
│   │   │   │   └── LoanEntity
│   │   │   │
│   │   │   └── DbSetup
│   │   │
│   │   ├── service
│   │   │   ├── interfaces
│   │   │   │   └── ServiceInterface
│   │   │   │
│   │   │   ├── models
│   │   │   │   ├── Book
│   │   │   │   ├── Member
│   │   │   │   └── Loan
│   │   │   │
│   │   │   ├── BookService
│   │   │   ├── MemberService
│   │   │   └── LoanService
│   │   │
│   │   └── util
│   │       ├── DbConnectionUtil
│   │       ├── InputUtil
│   │       └── validators
│   │           ├── BookValidator
│   │           ├── MemberValidator
│   │           ├── LoanValidator
│   │           └── ValidationUtil
│   │
│   └── resources
│       ├── database.properties
│       └── logback.xml
│
└── test
    └── java
        └── service
            ├── BookServiceTest
            ├── MemberServiceTest
            └── LoanServiceTest
```


### Layer Responsibilities

#### Controller Layer
- Handles console input/output and menu navigation
- Uses `InputUtil` for safe typed input
- Uses validators in `util.validators` to validate user-entered fields before calling services
- Delegates application logic to the service layer

#### Service Layer
- Contains business rules (e.g., "a book cannot be checked out twice")
- Coordinates DAO calls
- Converts Entities ↔ Models (repository layer returns `*Entity`, UI uses service `models`)
- Can apply additional validation via `ValidationUtil` when rules depend on multiple fields

#### Repository / DAO Layer
- Executes SQL queries
- Maps ResultSets to Entity objects
- Contains no business logic

#### Resources
- `src/main/resources/database.properties` stores DB connection configuration.
- `src/main/resources/logback.xml` configures console + rolling file logging (logs written under the project `logs/` directory).

---

## 4. Data Model & SQL Schema

### Book (`books`)
- `id` (PK)
- `title`
- `author`
- `isbn` (nullable)
- `publication_year` (nullable)

### Member (`members`)
- `id` (PK)
- `name`
- `email` (nullable, optional unique)
- `phone` (nullable)

### Loan (`loans`)
- `id` (PK)
- `book_id` (FK → books.id)
- `member_id` (FK → members.id)
- `checkout_date`
- `due_date`
- `return_date` (nullable)

### Relationship
- Many-to-many between Books and Members via Loans
- One Book can appear in many Loans over time
- One Member can have many Loans

### Normalization
- All tables are normalized to **Third Normal Form (3NF)**

---

## 5. Console Interaction Design

### app.Main Menu
```
1. Manage Books
2. Manage Members
3. Manage Loans
0. Exit
```

### Loan Menu
```
1. Check Out Book
2. Return Book
3. List All Loans
4. List Active Loans
5. List Loans by Member
6. List Overdue Loans
0. Back
```

### Error Handling
- Invalid menu options handled safely
- Non-numeric input rejected gracefully
- Missing IDs reported clearly
- Business rule violations explained to the user

---

## 6. Service Layer Responsibilities

### BookService
- `createEntity(Book)`
- `getEntityById(int)`
- `getAllEntities()`
- `updateEntity(int, Book)`
- `deleteEntity(int)`

### MemberService
- Same CRUD responsibilities as BookService

### LoanService
- `checkOutBook(memberId, bookId, checkoutDate)`
- `returnBook(loanId, returnDate)`
- `listAllLoans()`
- `listActiveLoans()`
- `listLoansByMember(memberId)`
- `listOverdueLoans()`

---

## 7. Testing Strategy

- Focus on **Service layer** testing
- DAO layer mocked using Mockito
- Minimum of **10 unit tests**
- Includes:
  - Happy paths
  - Negative cases
  - Edge cases

### TDD Requirement
- `LoanService.checkOutBook(...)` implemented using **Test-Driven Development**

---

## 8. Current Status

- ✅ Project structure complete
- ✅ Database schema implemented
- ✅ DAO layer implemented
- ✅ Services implemented
- ✅ Controllers implemented
- ✅ Logging configured
- ✅ Unit tests created

---

## Future Improvements

- Add maximum loan limits per member
- Introduce fines for overdue books
- Add pagination for large lists
- Add CSV export or reporting

---

**Author:** Benjamin Tuley  
**Project:** Revature Project 0 – Library Management System
