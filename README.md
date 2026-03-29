# Secure Audit Logging and Monitoring System
### DBMS Project — OWASP A10: Insufficient Logging & Monitoring
**Symbiosis Institute of Technology, Pune**
**Department of Computer Science**

**Team:**
- Mukund Ganesh Prajapati (240701222)
- Vikas Kumar (24070122275)
- Aseem Kelkar (24070122272)
- Het Bhalani

**Guide:** Anuradha Pillai

---

## Overview

A fully menu-driven, terminal-based Java application that implements a **Secure Audit Logging and Monitoring System** using:

- **MySQL** – Relational database backend (aligned with Codd's 12 Rules)
- **JDBC** – Java Database Connectivity for all DB operations
- **Java OOP** – Layered architecture: Model → DAO → Service → Menu
- **OWASP A10** – Addresses Insufficient Logging & Monitoring

---

## Project Structure

```
SecureAuditSystem/
│
├── sql/
│   └── schema.sql                        ← MySQL DDL (run this first)
│
├── src/com/audit/
│   ├── model/                            ← POJOs / Entities
│   │   ├── User.java
│   │   ├── AuthLog.java
│   │   ├── PasswordEvent.java
│   │   ├── UserEvent.java
│   │   ├── Session.java
│   │   └── InputValidationLog.java
│   │
│   ├── dao/                              ← Data Access Objects (JDBC)
│   │   ├── UserDAO.java
│   │   ├── AuthLogDAO.java
│   │   ├── PasswordEventDAO.java
│   │   ├── UserEventDAO.java
│   │   ├── SessionDAO.java
│   │   └── InputValidationLogDAO.java
│   │
│   ├── service/
│   │   └── AuditService.java             ← Business logic layer
│   │
│   ├── util/
│   │   ├── DBConnection.java             ← Singleton JDBC connection
│   │   └── PasswordUtil.java             ← SHA-256 password hashing
│   │
│   └── menu/
│       ├── MenuHelper.java               ← Formatting & colors
│       └── MainMenu.java                 ← Entry point (main method)
│
├── lib/                                  ← Place MySQL connector JAR here
│
├── build.sh  / build.bat
├── run.sh    / run.bat
└── README.md
```

---

## System Modules (from Report)

| # | Module                        | DB Table               | Description                                              |
|---|-------------------------------|------------------------|----------------------------------------------------------|
| 1 | User Management               | `USER`                 | Create, update role/status, delete users                 |
| 2 | Authentication Logging        | `AUTHENTICATION`       | Log all login/logout attempts (success, failure, block)  |
| 3 | Password Event Logging        | `PASSWORD_EVENT`       | Track password changes, resets, failures                 |
| 4 | User Event Audit Trail        | `USER_EVENT`           | Record who did what to which user account                |
| 5 | Session Management            | `SESSION`              | Open/close sessions, track active sessions               |
| 6 | Input Validation Logging      | `INPUT_VALIDATION_LOG` | Capture invalid inputs, injection attempts               |

---

## Prerequisites

| Requirement | Version     |
|-------------|-------------|
| Java JDK    | 8 or higher |
| MySQL       | 5.7 or higher |
| MySQL JDBC Connector | 8.x |

---

## Setup Instructions

### Step 1 — MySQL Setup

```sql
-- In MySQL Workbench or mysql CLI:
SOURCE /path/to/SecureAuditSystem/sql/schema.sql;
```

This creates the database `secure_audit_db` with all 6 tables and a default admin user.

### Step 2 — Configure DB Credentials

Edit `src/com/audit/util/DBConnection.java`:
```java
private static final String DB_URL  = "jdbc:mysql://localhost:3306/secure_audit_db?...";
private static final String DB_USER = "root";      // your MySQL username
private static final String DB_PASS = "root";      // your MySQL password
```

### Step 3 — Download MySQL JDBC Connector

1. Go to: https://dev.mysql.com/downloads/connector/j/
2. Download the **Platform Independent** `.jar`
3. Place it inside the `lib/` folder:
   ```
   lib/mysql-connector-j-8.x.x.jar
   ```

### Step 4 — Build

**Linux/Mac:**
```bash
chmod +x build.sh run.sh
./build.sh
```

**Windows:**
```cmd
build.bat
```

### Step 5 — Run

**Linux/Mac:**
```bash
./run.sh
```

**Windows:**
```cmd
run.bat
```

Or manually:
```bash
java -cp "SecureAuditSystem.jar:lib/mysql-connector-j-8.x.x.jar" com.audit.menu.MainMenu
```

---

## Default Login

| Field    | Value        |
|----------|--------------|
| Username | `admin`      |
| Password | `Admin@123`  |
| Role     | `ADMIN`      |

---

## Main Menu Structure

```
LOGIN
  └── MAIN MENU
        ├── 1. User Management
        │     ├── List All Users
        │     ├── Create New User
        │     ├── Change User Role
        │     ├── Change User Status
        │     └── Delete User
        │
        ├── 2. Authentication Logs
        │     ├── View All Auth Logs
        │     ├── View Failed Login Attempts
        │     ├── View Auth Logs by User ID
        │     └── Simulate Login Attempt
        │
        ├── 3. Password Events
        │     ├── View All Password Events
        │     └── Request Password Reset (Log)
        │
        ├── 4. Session Management
        │     ├── View All Sessions
        │     ├── View Active Sessions
        │     └── Terminate a Session
        │
        ├── 5. Input Validation Logs
        │     ├── View All Validation Failures
        │     └── Log a New Validation Failure
        │
        ├── 6. User Event Audit Trail
        │
        ├── 7. Change My Password
        │
        └── 0. Logout
```

---

## OOP Design

| Concept         | Where Applied                                                   |
|-----------------|------------------------------------------------------------------|
| Encapsulation   | All model fields are private with getters/setters               |
| Abstraction     | DAO layer hides all SQL from business logic                      |
| Inheritance     | (extensible — base DAO interface can be added)                  |
| Polymorphism    | `toString()` overridden in every model for uniform display      |
| Single Resp.    | Each class has one responsibility (Model / DAO / Service / Menu) |
| Singleton       | `DBConnection` maintains a single JDBC connection               |

---

## Codd's 12 Rules — Compliance Summary

| Rule | Description                      | Implementation                                      |
|------|----------------------------------|-----------------------------------------------------|
| 0    | Foundation Rule                  | Fully MySQL-based relational DBMS                   |
| 1    | Information Rule                 | All data stored in tables                           |
| 2    | Guaranteed Access Rule           | PK + table name uniquely identifies any value       |
| 3    | Null Values                      | `logout_time`, `user_id` in validation log nullable |
| 4    | Dynamic Catalog                  | MySQL system catalogs (information_schema)          |
| 5    | Comprehensive Data Sublanguage   | SQL DDL + DML + DCL used throughout                 |
| 6    | View Updating                    | Views reflect live data                             |
| 7    | High-Level Insert/Update/Delete  | Set-based SQL operations                            |
| 8    | Physical Data Independence       | Storage changes don't affect Java code              |
| 9    | Logical Data Independence        | Schema changes isolated in DAO layer                |
| 10   | Integrity Independence           | PKs, FKs, ENUMs enforce integrity in DB             |
| 11   | Distribution Independence        | Design works distributed                            |
| 12   | Non-Subversion Rule              | Role-based access in Java prevents bypassing DB     |

---

## ER Diagram Summary

```
USER ──< AUTHENTICATION
USER ──< PASSWORD_EVENT
USER ──< SESSION
USER ──< INPUT_VALIDATION_LOG
USER ──< USER_EVENT (as target_user_id)
USER ──< USER_EVENT (as performed_by)
```

---

## Security Notes

- Passwords are stored as **SHA-256 hashes** (never plain text)
- All login attempts (success/failure/blocked) are logged to `AUTHENTICATION`
- Locked accounts are blocked at service layer before DB query
- Role-based access: only `ADMIN` can create/delete users or change roles
- All audit logs are **insert-only** — no update/delete on log tables
- Input validation failures are captured with field name, reason, and IP

---
