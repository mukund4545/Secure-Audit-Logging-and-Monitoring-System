package com.audit.interfaces;

import java.sql.SQLException;
import java.util.List;

/**
 * INTERFACE — BaseDAO<T>
 *
 * Generic DAO contract that every DAO must honour.
 * Enables POLYMORPHISM: AuditService can hold a List<BaseDAO<?>>
 * and call insert/getAll uniformly across all log types.
 *
 * T = the model type (AuthLog, PasswordEvent, Session, etc.)
 */
public interface BaseDAO<T> {

    /**
     * Insert a new record into the database.
     * @return true if insertion succeeded
     */
    boolean insert(T entity) throws SQLException;

    /**
     * Retrieve all records of this type.
     */
    List<T> getAll() throws SQLException;

    /**
     * Retrieve records for a specific user.
     */
    List<T> getByUserId(int userId) throws SQLException;
}
