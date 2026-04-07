# Secure Audit Logging and Monitoring System
### DBMS Project — OWASP A10: Insufficient Logging & Monitoring
**Symbiosis Institute of Technology, Pune — Department of Computer Science**

**Team:** Mukund Prajapati (24070122280) · Vikas Kumar (24070122275) · Aseem Kelkar (24070122272) · Het Bhalani (25070122513)  

---

## workflow
┌─────────────────────────────────────────────┐
│           LAYER 4: MENU                     │
│   MainMenu.java + MenuHelper.java           │
│   • Reads user input from terminal          │
│   • Calls service methods                  │
│   • Catches exceptions, prints results     │
└──────────────────┬──────────────────────────┘
                   │ calls
┌──────────────────▼──────────────────────────┐
│           LAYER 3: SERVICE                  │
│   AuditService.java                         │
│   • Contains ALL business logic             │
│   • Validates input, enforces rules         │
│   • Coordinates multiple DAO calls          │
│   • Throws typed exceptions on failure      │
└──────────────────┬──────────────────────────┘
                   │ calls
┌──────────────────▼──────────────────────────┐
│           LAYER 2: DAO                      │
│   UserDAO, AuthLogDAO, SessionDAO, etc.     │
│   ProcedureDAO (stored procedures)          │
│   • Contains ALL SQL queries                │
│   • Translates Java objects ↔ DB rows       │
│   • Returns model objects to service        │
└──────────────────┬──────────────────────────┘
                   │ uses
┌──────────────────▼──────────────────────────┐
│           LAYER 1: DATABASE                 │
│   MySQL — secure_audit_db                  │
│   6 tables + 4 triggers + 5 procedures     │
└─────────────────────────────────────────────┘

## OOP Concepts — Where They Are Applied

### 1. INTERFACE
Two interfaces define contracts across the system:

| Interface | Package | Purpose |
|-----------|---------|---------|
| `Auditable` | `interfaces` | Every log model must implement `getSummary()`, `getCategory()`, `getLogId()` |
| `BaseDAO<T>` | `interfaces` | Every DAO must implement `insert(T)`, `getAll()`, `getByUserId(int)` |

`BaseDAO<T>` enables **polymorphism** — `AuditService.getAllLogSummaries()` stores all DAOs as `List<BaseDAO<?>>` and calls `getAll()` uniformly.

---

### 2. INHERITANCE

```
BaseLog  (abstract, implements Auditable)
  ├── AuthLog
  ├── PasswordEvent
  ├── UserEvent
  ├── Session
  └── InputValidationLog

AuditException  (extends Exception)
  ├── AuthException
  ├── UserNotFoundException
  ├── AccessDeniedException
  └── ValidationException
```

`BaseLog` provides shared fields (`userId`, `timestamp`, `getFormattedTimestamp()`) and declares two abstract methods that all subclasses must implement.

---


### 3. POLYMORPHISM

**Runtime method dispatch** in `AuditService.getAllLogSummaries()`:
```java
List<BaseDAO<?>> allDAOs = new ArrayList<>();
allDAOs.add(authLogDAO);          // BaseDAO<AuthLog>
allDAOs.add(passwordEventDAO);    // BaseDAO<PasswordEvent>
allDAOs.add(sessionDAO);          // BaseDAO<Session>
// ...
for (BaseDAO<?> dao : allDAOs) {
    for (Object obj : dao.getAll()) {
        ((BaseLog) obj).getSummary();  // dispatches to correct subclass at runtime
    }
}
```

Menu option **8** ("All Log Summaries") demonstrates this live — the same `getSummary()` call produces different output per subtype:
```
[AUTH]       User:1 | LOGIN | SUCCESS | IP:127.0.0.1 | 2025-04-01 10:00:00
[PASSWORD]   User:1 | CHANGE | SUCCESS | 2025-04-01 10:05:00
[SESSION]    User:1 | ACTIVE | IP:127.0.0.1 | Login:2025-04-01 10:00:00
[VALIDATION] User:1 | Field:email | Invalid format | 2025-04-01 10:10:00
```

---

### 4. USER-DEFINED EXCEPTIONS

All exceptions extend `AuditException` which extends `java.lang.Exception`.

| Exception Class | Error Code | Thrown When |
|----------------|-----------|-------------|
| `AuditException` | `AUDIT_ERR` | Base — generic audit failures |
| `AuthException` | `AUTH_<REASON>` | Login fails (wrong pass, locked, not found) |
| `UserNotFoundException` | `USER_NOT_FOUND` | getUserById / changeRole with bad ID |
| `AccessDeniedException` | `ACCESS_DENIED` | Non-admin calls admin-only operation |
| `ValidationException` | `VALIDATION_FAILED` | Empty fields, bad email, short password |

Each exception carries **typed metadata** (e.g. `AuthException.getReason()`, `ValidationException.getFieldName()`), allowing the menu to display precise diagnostic messages.

---

## Project Structure

```
SecureAuditSystem/
├── sql/
│   └── schema.sql
├── src/com/audit/
│   ├── interfaces/
│   │   ├── Auditable.java          ← INTERFACE
│   │   └── BaseDAO.java            ← INTERFACE (generic)
│   ├── exception/
│   │   ├── AuditException.java     ← USER-DEFINED EXCEPTION (base)
│   │   ├── AuthException.java      ← extends AuditException
│   │   ├── UserNotFoundException.java
│   │   ├── AccessDeniedException.java
│   │   └── ValidationException.java
│   ├── model/
│   │   ├── BaseLog.java            ← ABSTRACT CLASS (inheritance root)
│   │   ├── AuthLog.java            ← extends BaseLog
│   │   ├── PasswordEvent.java      ← extends BaseLog
│   │   ├── UserEvent.java          ← extends BaseLog
│   │   ├── Session.java            ← extends BaseLog
│   │   ├── InputValidationLog.java ← extends BaseLog
│   │   └── User.java
│   ├── dao/
│   │   ├── UserDAO.java
│   │   ├── AuthLogDAO.java         ← implements BaseDAO<AuthLog>
│   │   ├── PasswordEventDAO.java   ← implements BaseDAO<PasswordEvent>
│   │   ├── UserEventDAO.java       ← implements BaseDAO<UserEvent>
│   │   ├── SessionDAO.java         ← implements BaseDAO<Session>
│   │   └── InputValidationLogDAO.java
│   ├── service/
│   │   └── AuditService.java       ← throws typed exceptions, polymorphic DAOs
│   ├── util/
│   │   ├── DBConnection.java       ← Singleton pattern
│   │   └── PasswordUtil.java
│   └── menu/
│       ├── MainMenu.java           ← catches typed exceptions per operation
│       └── MenuHelper.java
├── lib/                            ← place mysql-connector-j-*.jar here
├── build.sh / build.bat
├── run.sh   / run.bat
└── README.md
```

---

## Setup

### Step 1 — Database
```sql
SOURCE /path/to/SecureAuditSystem/sql/schema.sql;
```

### Step 2 — Configure credentials
Edit `src/com/audit/util/DBConnection.java`:
```java
private static final String DB_USER = "root";
private static final String DB_PASS = "your_password";
```

### Step 3 — MySQL JDBC Connector
Download from https://dev.mysql.com/downloads/connector/j/  
Place the `.jar` in the `lib/` directory.

### Step 4 — Build & Run
```bash
# Linux/Mac
chmod +x build.sh run.sh
./build.sh && ./run.sh

# Windows
build.bat
run.bat or  
cd SecureAuditSystem 
java -cp "out;lib\mysql-connector-j-9.6.0.jar" com.audit.menu.MainMenu

```

### Default Login
| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | Admin@123 | ADMIN |

---

## Menu Overview

```
LOGIN  →  catches AuthException (typed reason: INVALID_CREDENTIALS / ACCOUNT_LOCKED / ...)
│
└─ MAIN MENU
     1. User Management
        ├─ Create User    → throws ValidationException (bad email, short password)
        ├─ View by ID     → throws UserNotFoundException
        ├─ Change Role    → throws AccessDeniedException (non-admin)
        ├─ Change Status  → throws AccessDeniedException, ValidationException
        └─ Delete User    → throws UserNotFoundException
     2. Authentication Logs
        └─ Simulate Login → catches AuthException with per-reason messages
     3. Password Events
     4. Session Management
     5. Input Validation Logs
     6. User Event Audit Trail
     7. Change My Password  → throws ValidationException
     8. [POLYMORPHISM DEMO] All Log Summaries  ← getSummary() across all subtypes
     0. Logout
```
