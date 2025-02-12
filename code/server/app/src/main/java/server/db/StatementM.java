package server.db;

import server.web.ServerStatistics;

import java.sql.*;

public class StatementM implements Statement {
    private final Statement stmt;
    protected ServerStatistics stats;

    public StatementM(Statement smt) {
        this.stmt = smt;
    }

    /**
     * Executes the given SQL statement, which returns a single
     * {@code ResultSet} object.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql an SQL statement to be sent to the database, typically a
     *            static SQL {@code SELECT} statement
     * @return a {@code ResultSet} object that contains the data produced
     * by the given query; never {@code null}
     * @throws SQLException        if a database access error occurs,
     *                             this method is called on a closed {@code Statement}, the given
     *                             SQL statement produces anything other than a single
     *                             {@code ResultSet} object, the method is called on a
     *                             {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeQuery(sql);
    }

    /**
     * Executes the given SQL statement, which may be an {@code INSERT},
     * {@code UPDATE}, or {@code DELETE} statement or an
     * SQL statement that returns nothing, such as an SQL DDL statement.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql an SQL Data Manipulation Language (DML) statement, such as {@code INSERT}, {@code UPDATE} or
     *            {@code DELETE}; or an SQL statement that returns nothing,
     *            such as a DDL statement.
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException        if a database access error occurs,
     *                             this method is called on a closed {@code Statement}, the given
     *                             SQL statement produces a {@code ResultSet} object, the method is called on a
     *                             {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     */
    @Override
    public int executeUpdate(String sql) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeUpdate(sql);
    }

    /**
     * Releases this {@code Statement} object's database
     * and JDBC resources immediately instead of waiting for
     * this to happen when it is automatically closed.
     * It is generally good practice to release resources as soon as
     * you are finished with them to avoid tying up database
     * resources.
     * <p>
     * Calling the method {@code close} on a {@code Statement}
     * object that is already closed has no effect.
     * <p>
     * <B>Note:</B>When a {@code Statement} object is
     * closed, its current {@code ResultSet} object, if one exists, is
     * also closed.
     *
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void close() throws SQLException {
        stmt.close();
    }

    /**
     * Retrieves the maximum number of bytes that can be
     * returned for character and binary column values in a {@code ResultSet}
     * object produced by this {@code Statement} object.
     * This limit applies only to  {@code BINARY}, {@code VARBINARY},
     * {@code LONGVARBINARY}, {@code CHAR}, {@code VARCHAR},
     * {@code NCHAR}, {@code NVARCHAR}, {@code LONGNVARCHAR}
     * and {@code LONGVARCHAR} columns.  If the limit is exceeded, the
     * excess data is silently discarded.
     *
     * @return the current column size limit for columns storing character and
     * binary values; zero means there is no limit
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #setMaxFieldSize
     */
    @Override
    public int getMaxFieldSize() throws SQLException {
        return stmt.getMaxFieldSize();
    }

    /**
     * Sets the limit for the maximum number of bytes that can be returned for
     * character and binary column values in a {@code ResultSet}
     * object produced by this {@code Statement} object.
     * <p>
     * This limit applies
     * only to {@code BINARY}, {@code VARBINARY},
     * {@code LONGVARBINARY}, {@code CHAR}, {@code VARCHAR},
     * {@code NCHAR}, {@code NVARCHAR}, {@code LONGNVARCHAR} and
     * {@code LONGVARCHAR} fields.  If the limit is exceeded, the excess data
     * is silently discarded. For maximum portability, use values
     * greater than 256.
     *
     * @param max the new column size limit in bytes; zero means there is no limit
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed {@code Statement}
     *                      or the condition {@code max >= 0} is not satisfied
     * @see #getMaxFieldSize
     */
    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        stmt.setMaxFieldSize(max);
    }

    /**
     * Retrieves the maximum number of rows that a
     * {@code ResultSet} object produced by this
     * {@code Statement} object can contain.  If this limit is exceeded,
     * the excess rows are silently dropped.
     *
     * @return the current maximum number of rows for a {@code ResultSet}
     * object produced by this {@code Statement} object;
     * zero means there is no limit
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #setMaxRows
     */
    @Override
    public int getMaxRows() throws SQLException {
        return stmt.getMaxRows();
    }

    /**
     * Sets the limit for the maximum number of rows that any
     * {@code ResultSet} object  generated by this {@code Statement}
     * object can contain to the given number.
     * If the limit is exceeded, the excess
     * rows are silently dropped.
     *
     * @param max the new max rows limit; zero means there is no limit
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed {@code Statement}
     *                      or the condition {@code max >= 0} is not satisfied
     * @see #getMaxRows
     */
    @Override
    public void setMaxRows(int max) throws SQLException {
        stmt.setMaxRows(max);
    }

    /**
     * Sets escape processing on or off.
     * If escape scanning is on (the default), the driver will do
     * escape substitution before sending the SQL statement to the database.
     * <p>
     * The {@code Connection} and {@code DataSource} property
     * {@code escapeProcessing} may be used to change the default escape processing
     * behavior.  A value of true (the default) enables escape Processing for
     * all {@code Statement} objects. A value of false disables escape processing
     * for all {@code Statement} objects.  The {@code setEscapeProcessing}
     * method may be used to specify the escape processing behavior for an
     * individual {@code Statement} object.
     * <p>
     * Note: Since prepared statements have usually been parsed prior
     * to making this call, disabling escape processing for
     * {@code PreparedStatements} objects will have no effect.
     *
     * @param enable {@code true} to enable escape processing;
     *               {@code false} to disable it
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     */
    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        stmt.setEscapeProcessing(enable);
    }

    /**
     * Retrieves the number of seconds the driver will
     * wait for a {@code Statement} object to execute.
     * If the limit is exceeded, a
     * {@code SQLException} is thrown.
     *
     * @return the current query timeout limit in seconds; zero means there is
     * no limit
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #setQueryTimeout
     */
    @Override
    public int getQueryTimeout() throws SQLException {
        return stmt.getQueryTimeout();
    }

    /**
     * Sets the number of seconds the driver will wait for a
     * {@code Statement} object to execute to the given number of seconds.
     * By default there is no limit on the amount of time allowed for a running
     * statement to complete. If the limit is exceeded, an
     * {@code SQLTimeoutException} is thrown.
     * A JDBC driver must apply this limit to the {@code execute},
     * {@code executeQuery} and {@code executeUpdate} methods.
     * <p>
     * <strong>Note:</strong> JDBC driver implementations may also apply this
     * limit to {@code ResultSet} methods
     * (consult your driver vendor documentation for details).
     * <p>
     * <strong>Note:</strong> In the case of {@code Statement} batching, it is
     * implementation defined as to whether the time-out is applied to
     * individual SQL commands added via the {@code addBatch} method or to
     * the entire batch of SQL commands invoked by the {@code executeBatch}
     * method (consult your driver vendor documentation for details).
     *
     * @param seconds the new query timeout limit in seconds; zero means
     *                there is no limit
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed {@code Statement}
     *                      or the condition {@code seconds >= 0} is not satisfied
     * @see #getQueryTimeout
     */
    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        stmt.setQueryTimeout(seconds);
    }

    /**
     * Cancels this {@code Statement} object if both the DBMS and
     * driver support aborting an SQL statement.
     * This method can be used by one thread to cancel a statement that
     * is being executed by another thread.
     *
     * @throws SQLException                    if a database access error occurs or
     *                                         this method is called on a closed {@code Statement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     */
    @Override
    public void cancel() throws SQLException {
        stmt.cancel();
    }

    /**
     * Retrieves the first warning reported by calls on this {@code Statement} object.
     * Subsequent {@code Statement} object warnings will be chained to this
     * {@code SQLWarning} object.
     *
     * <p>The warning chain is automatically cleared each time
     * a statement is (re)executed. This method may not be called on a closed
     * {@code Statement} object; doing so will cause an {@code SQLException}
     * to be thrown.
     *
     * <P><B>Note:</B> If you are processing a {@code ResultSet} object, any
     * warnings associated with reads on that {@code ResultSet} object
     * will be chained on it rather than on the {@code Statement}
     * object that produced it.
     *
     * @return the first {@code SQLWarning} object or {@code null}
     * if there are no warnings
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        return stmt.getWarnings();
    }

    /**
     * Clears all the warnings reported on this {@code Statement}
     * object. After a call to this method,
     * the method {@code getWarnings} will return
     * {@code null} until a new warning is reported for this
     * {@code Statement} object.
     *
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     */
    @Override
    public void clearWarnings() throws SQLException {
        stmt.clearWarnings();
    }

    /**
     * Sets the SQL cursor name to the given {@code String}, which
     * will be used by subsequent {@code Statement} object
     * {@code execute} methods. This name can then be
     * used in SQL positioned update or delete statements to identify the
     * current row in the {@code ResultSet} object generated by this
     * statement.  If the database does not support positioned update/delete,
     * this method is a noop.  To insure that a cursor has the proper isolation
     * level to support updates, the cursor's {@code SELECT} statement
     * should have the form {@code SELECT FOR UPDATE}.  If
     * {@code FOR UPDATE} is not present, positioned updates may fail.
     *
     * <P><B>Note:</B> By definition, the execution of positioned updates and
     * deletes must be done by a different {@code Statement} object than
     * the one that generated the {@code ResultSet} object being used for
     * positioning. Also, cursor names must be unique within a connection.
     *
     * @param name the new cursor name, which must be unique within
     *             a connection
     * @throws SQLException                    if a database access error occurs or
     *                                         this method is called on a closed {@code Statement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     */
    @Override
    public void setCursorName(String name) throws SQLException {
        stmt.setCursorName(name);
    }

    /**
     * Executes the given SQL statement, which may return multiple results.
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p>
     * The {@code execute} method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * {@code getResultSet} or {@code getUpdateCount}
     * to retrieve the result, and {@code getMoreResults} to
     * move to any subsequent result(s).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql any SQL statement
     * @return {@code true} if the first result is a {@code ResultSet}
     * object; {@code false} if it is an update count or there are
     * no results
     * @throws SQLException        if a database access error occurs,
     *                             this method is called on a closed {@code Statement},
     *                             the method is called on a
     *                             {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     */
    @Override
    public boolean execute(String sql) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.execute(sql);
    }

    /**
     * Retrieves the current result as a {@code ResultSet} object.
     * This method should be called only once per result.
     *
     * @return the current result as a {@code ResultSet} object or
     * {@code null} if the result is an update count or there are no more results
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #execute
     */
    @Override
    public ResultSet getResultSet() throws SQLException {
        return stmt.getResultSet();
    }

    /**
     * Retrieves the current result as an update count;
     * if the result is a {@code ResultSet} object or there are no more results, -1
     * is returned. This method should be called only once per result.
     *
     * @return the current result as an update count; -1 if the current result is a
     * {@code ResultSet} object or there are no more results
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #execute
     */
    @Override
    public int getUpdateCount() throws SQLException {
        return stmt.getUpdateCount();
    }

    /**
     * Moves to this {@code Statement} object's next result, returns
     * {@code true} if it is a {@code ResultSet} object, and
     * implicitly closes any current {@code ResultSet}
     * object(s) obtained with the method {@code getResultSet}.
     *
     * <P>There are no more results when the following is true:
     * <PRE>{@code
     * // stmt is a Statement object
     * ((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1))
     * }</PRE>
     *
     * @return {@code true} if the next result is a {@code ResultSet}
     * object; {@code false} if it is an update count or there are
     * no more results
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #execute
     */
    @Override
    public boolean getMoreResults() throws SQLException {
        return stmt.getMoreResults();
    }

    /**
     * Gives the driver a hint as to the direction in which
     * rows will be processed in {@code ResultSet}
     * objects created using this {@code Statement} object.  The
     * default value is {@code ResultSet.FETCH_FORWARD}.
     * <p>
     * Note that this method sets the default fetch direction for
     * result sets generated by this {@code Statement} object.
     * Each result set has its own methods for getting and setting
     * its own fetch direction.
     *
     * @param direction the initial direction for processing rows
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed {@code Statement}
     *                      or the given direction
     *                      is not one of {@code ResultSet.FETCH_FORWARD},
     *                      {@code ResultSet.FETCH_REVERSE}, or {@code ResultSet.FETCH_UNKNOWN}
     * @see #getFetchDirection
     * @since 1.2
     */
    @Override
    public void setFetchDirection(int direction) throws SQLException {
        stmt.setFetchDirection(direction);
    }

    /**
     * Retrieves the direction for fetching rows from
     * database tables that is the default for result sets
     * generated from this {@code Statement} object.
     * If this {@code Statement} object has not set
     * a fetch direction by calling the method {@code setFetchDirection},
     * the return value is implementation-specific.
     *
     * @return the default fetch direction for result sets generated
     * from this {@code Statement} object
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #setFetchDirection
     * @since 1.2
     */
    @Override
    public int getFetchDirection() throws SQLException {
        return stmt.getFetchDirection();
    }

    /**
     * Gives the JDBC driver a hint as to the number of rows that should
     * be fetched from the database when more rows are needed for
     * {@code ResultSet} objects generated by this {@code Statement}.
     * If the value specified is zero, then the hint is ignored.
     * The default value is zero.
     *
     * @param rows the number of rows to fetch
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed {@code Statement} or the
     *                      condition {@code rows >= 0} is not satisfied.
     * @see #getFetchSize
     * @since 1.2
     */
    @Override
    public void setFetchSize(int rows) throws SQLException {
        stmt.setFetchSize(rows);
    }

    /**
     * Retrieves the number of result set rows that is the default
     * fetch size for {@code ResultSet} objects
     * generated from this {@code Statement} object.
     * If this {@code Statement} object has not set
     * a fetch size by calling the method {@code setFetchSize},
     * the return value is implementation-specific.
     *
     * @return the default fetch size for result sets generated
     * from this {@code Statement} object
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #setFetchSize
     * @since 1.2
     */
    @Override
    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    /**
     * Retrieves the result set concurrency for {@code ResultSet} objects
     * generated by this {@code Statement} object.
     *
     * @return either {@code ResultSet.CONCUR_READ_ONLY} or
     * {@code ResultSet.CONCUR_UPDATABLE}
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @since 1.2
     */
    @Override
    public int getResultSetConcurrency() throws SQLException {
        return stmt.getResultSetConcurrency();
    }

    /**
     * Retrieves the result set type for {@code ResultSet} objects
     * generated by this {@code Statement} object.
     *
     * @return one of {@code ResultSet.TYPE_FORWARD_ONLY},
     * {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     * {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @since 1.2
     */
    @Override
    public int getResultSetType() throws SQLException {
        return stmt.getResultSetType();
    }

    /**
     * Adds the given SQL command to the current list of commands for this
     * {@code Statement} object. The commands in this list can be
     * executed as a batch by calling the method {@code executeBatch}.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql typically this is a SQL {@code INSERT} or
     *            {@code UPDATE} statement
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed {@code Statement}, the
     *                      driver does not support batch updates, the method is called on a
     *                      {@code PreparedStatement} or {@code CallableStatement}
     * @see #executeBatch
     * @see DatabaseMetaData#supportsBatchUpdates
     * @since 1.2
     */
    @Override
    public void addBatch(String sql) throws SQLException {
        stmt.addBatch(sql);
    }

    /**
     * Empties this {@code Statement} object's current list of
     * SQL commands.
     *
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed {@code Statement} or the
     *                      driver does not support batch updates
     * @see #addBatch
     * @see DatabaseMetaData#supportsBatchUpdates
     * @since 1.2
     */
    @Override
    public void clearBatch() throws SQLException {
        stmt.clearBatch();
    }

    /**
     * Submits a batch of commands to the database for execution and
     * if all commands execute successfully, returns an array of update counts.
     * The {@code int} elements of the array that is returned are ordered
     * to correspond to the commands in the batch, which are ordered
     * according to the order in which they were added to the batch.
     * The elements in the array returned by the method {@code executeBatch}
     * may be one of the following:
     * <OL>
     * <LI>A number greater than or equal to zero -- indicates that the
     * command was processed successfully and is an update count giving the
     * number of rows in the database that were affected by the command's
     * execution
     * <LI>A value of {@code SUCCESS_NO_INFO} -- indicates that the command was
     * processed successfully but that the number of rows affected is
     * unknown
     * <p>
     * If one of the commands in a batch update fails to execute properly,
     * this method throws a {@code BatchUpdateException}, and a JDBC
     * driver may or may not continue to process the remaining commands in
     * the batch.  However, the driver's behavior must be consistent with a
     * particular DBMS, either always continuing to process commands or never
     * continuing to process commands.  If the driver continues processing
     * after a failure, the array returned by the method
     * {@code BatchUpdateException.getUpdateCounts}
     * will contain as many elements as there are commands in the batch, and
     * at least one of the elements will be the following:
     *
     * <LI>A value of {@code EXECUTE_FAILED} -- indicates that the command failed
     * to execute successfully and occurs only if a driver continues to
     * process commands after a command fails
     * </OL>
     * <p>
     * The possible implementations and return values have been modified in
     * the Java 2 SDK, Standard Edition, version 1.3 to
     * accommodate the option of continuing to process commands in a batch
     * update after a {@code BatchUpdateException} object has been thrown.
     *
     * @return an array of update counts containing one element for each
     * command in the batch.  The elements of the array are ordered according
     * to the order in which commands were added to the batch.
     * @throws SQLException        if a database access error occurs,
     *                             this method is called on a closed {@code Statement} or the
     *                             driver does not support batch statements. Throws {@link BatchUpdateException}
     *                             (a subclass of {@code SQLException}) if one of the commands sent to the
     *                             database fails to execute properly or attempts to return a result set.
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     * @see #addBatch
     * @see DatabaseMetaData#supportsBatchUpdates
     * @since 1.2
     */
    @Override
    public int[] executeBatch() throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeBatch();
    }

    /**
     * Retrieves the {@code Connection} object
     * that produced this {@code Statement} object.
     *
     * @return the connection that produced this statement
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @since 1.2
     */
    @Override
    public Connection getConnection() throws SQLException {
        return stmt.getConnection();
    }

    /**
     * Moves to this {@code Statement} object's next result, deals with
     * any current {@code ResultSet} object(s) according  to the instructions
     * specified by the given flag, and returns
     * {@code true} if the next result is a {@code ResultSet} object.
     *
     * <P>There are no more results when the following is true:
     * <PRE>{@code
     * // stmt is a Statement object
     * ((stmt.getMoreResults(current) == false) && (stmt.getUpdateCount() == -1))
     * }</PRE>
     *
     * @param current one of the following {@code Statement}
     *                constants indicating what should happen to current
     *                {@code ResultSet} objects obtained using the method
     *                {@code getResultSet}:
     *                {@code Statement.CLOSE_CURRENT_RESULT},
     *                {@code Statement.KEEP_CURRENT_RESULT}, or
     *                {@code Statement.CLOSE_ALL_RESULTS}
     * @return {@code true} if the next result is a {@code ResultSet}
     * object; {@code false} if it is an update count or there are no
     * more results
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement} or the argument
     *                                         supplied is not one of the following:
     *                                         {@code Statement.CLOSE_CURRENT_RESULT},
     *                                         {@code Statement.KEEP_CURRENT_RESULT} or
     *                                         {@code Statement.CLOSE_ALL_RESULTS}
     * @throws SQLFeatureNotSupportedException if
     *                                         {@code DatabaseMetaData.supportsMultipleOpenResults} returns
     *                                         {@code false} and either
     *                                         {@code Statement.KEEP_CURRENT_RESULT} or
     *                                         {@code Statement.CLOSE_ALL_RESULTS} are supplied as
     *                                         the argument.
     * @see #execute
     * @since 1.4
     */
    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return stmt.getMoreResults(current);
    }

    /**
     * Retrieves any auto-generated keys created as a result of executing this
     * {@code Statement} object. If this {@code Statement} object did
     * not generate any keys, an empty {@code ResultSet}
     * object is returned.
     *
     * <p><B>Note:</B>If the columns which represent the auto-generated keys were not specified,
     * the JDBC driver implementation will determine the columns which best represent the auto-generated keys.
     *
     * @return a {@code ResultSet} object containing the auto-generated key(s)
     * generated by the execution of this {@code Statement} object
     * @throws SQLException                    if a database access error occurs or
     *                                         this method is called on a closed {@code Statement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return stmt.getGeneratedKeys();
    }

    /**
     * Executes the given SQL statement and signals the driver with the
     * given flag about whether the
     * auto-generated keys produced by this {@code Statement} object
     * should be made available for retrieval.  The driver will ignore the
     * flag if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql               an SQL Data Manipulation Language (DML) statement, such as {@code INSERT}, {@code UPDATE} or
     *                          {@code DELETE}; or an SQL statement that returns nothing,
     *                          such as a DDL statement.
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *                          should be made available for retrieval;
     *                          one of the following constants:
     *                          {@code Statement.RETURN_GENERATED_KEYS}
     *                          {@code Statement.NO_GENERATED_KEYS}
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement}, the given
     *                                         SQL statement returns a {@code ResultSet} object,
     *                                         the given constant is not one of those allowed, the method is called on a
     *                                         {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @since 1.4
     */
    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeUpdate(sql, autoGeneratedKeys);
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the indexes of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql           an SQL Data Manipulation Language (DML) statement, such as {@code INSERT}, {@code UPDATE} or
     *                      {@code DELETE}; or an SQL statement that returns nothing,
     *                      such as a DDL statement.
     * @param columnIndexes an array of column indexes indicating the columns
     *                      that should be returned from the inserted row
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement}, the SQL
     *                                         statement returns a {@code ResultSet} object,the second argument
     *                                         supplied to this method is not an
     *                                         {@code int} array whose elements are valid column indexes, the method is called on a
     *                                         {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @since 1.4
     */
    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeUpdate(sql, columnIndexes);
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the names of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql         an SQL Data Manipulation Language (DML) statement, such as {@code INSERT}, {@code UPDATE} or
     *                    {@code DELETE}; or an SQL statement that returns nothing,
     *                    such as a DDL statement.
     * @param columnNames an array of the names of the columns that should be
     *                    returned from the inserted row
     * @return either the row count for {@code INSERT}, {@code UPDATE},
     * or {@code DELETE} statements, or 0 for SQL statements
     * that return nothing
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement}, the SQL
     *                                         statement returns a {@code ResultSet} object, the
     *                                         second argument supplied to this method is not a {@code String} array
     *                                         whose elements are valid column names, the method is called on a
     *                                         {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @since 1.4
     */
    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeUpdate(sql, columnNames);
    }

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that any
     * auto-generated keys should be made available
     * for retrieval.  The driver will ignore this signal if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p>
     * The {@code execute} method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * {@code getResultSet} or {@code getUpdateCount}
     * to retrieve the result, and {@code getMoreResults} to
     * move to any subsequent result(s).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql               any SQL statement
     * @param autoGeneratedKeys a constant indicating whether auto-generated
     *                          keys should be made available for retrieval using the method
     *                          {@code getGeneratedKeys}; one of the following constants:
     *                          {@code Statement.RETURN_GENERATED_KEYS} or
     *                          {@code Statement.NO_GENERATED_KEYS}
     * @return {@code true} if the first result is a {@code ResultSet}
     * object; {@code false} if it is an update count or there are
     * no results
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement}, the second
     *                                         parameter supplied to this method is not
     *                                         {@code Statement.RETURN_GENERATED_KEYS} or
     *                                         {@code Statement.NO_GENERATED_KEYS},
     *                                         the method is called on a
     *                                         {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @see #getGeneratedKeys
     * @since 1.4
     */
    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.execute(sql, autoGeneratedKeys);
    }

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.  This array contains the indexes of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available.  The driver will ignore the array if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * Under some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p>
     * The {@code execute} method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * {@code getResultSet} or {@code getUpdateCount}
     * to retrieve the result, and {@code getMoreResults} to
     * move to any subsequent result(s).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql           any SQL statement
     * @param columnIndexes an array of the indexes of the columns in the
     *                      inserted row that should be  made available for retrieval by a
     *                      call to the method {@code getGeneratedKeys}
     * @return {@code true} if the first result is a {@code ResultSet}
     * object; {@code false} if it is an update count or there
     * are no results
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement}, the
     *                                         elements in the {@code int} array passed to this method
     *                                         are not valid column indexes, the method is called on a
     *                                         {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @since 1.4
     */
    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.execute(sql, columnIndexes);
    }

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval. This array contains the names of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available.  The driver will ignore the array if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p>
     * The {@code execute} method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * {@code getResultSet} or {@code getUpdateCount}
     * to retrieve the result, and {@code getMoreResults} to
     * move to any subsequent result(s).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     *
     * @param sql         any SQL statement
     * @param columnNames an array of the names of the columns in the inserted
     *                    row that should be made available for retrieval by a call to the
     *                    method {@code getGeneratedKeys}
     * @return {@code true} if the next result is a {@code ResultSet}
     * object; {@code false} if it is an update count or there
     * are no more results
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement},the
     *                                         elements of the {@code String} array passed to this
     *                                         method are not valid column names, the method is called on a
     *                                         {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @see #getGeneratedKeys
     * @since 1.4
     */
    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.execute(sql, columnNames);
    }

    /**
     * Retrieves the result set holdability for {@code ResultSet} objects
     * generated by this {@code Statement} object.
     *
     * @return either {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or
     * {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @since 1.4
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        return stmt.getResultSetHoldability();
    }

    /**
     * Retrieves whether this {@code Statement} object has been closed. A {@code Statement} is closed if the
     * method close has been called on it, or if it is automatically closed.
     *
     * @return true if this {@code Statement} object is closed; false if it is still open
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    @Override
    public boolean isClosed() throws SQLException {
        return stmt.isClosed();
    }

    /**
     * Requests that a {@code Statement} be pooled or not pooled.  The value
     * specified is a hint to the statement pool implementation indicating
     * whether the application wants the statement to be pooled.  It is up to
     * the statement pool manager as to whether the hint is used.
     * <p>
     * The poolable value of a statement is applicable to both internal
     * statement caches implemented by the driver and external statement caches
     * implemented by application servers and other applications.
     * <p>
     * By default, a {@code Statement} is not poolable when created, and
     * a {@code PreparedStatement} and {@code CallableStatement}
     * are poolable when created.
     *
     * @param poolable requests that the statement be pooled if true and
     *                 that the statement not be pooled if false
     * @throws SQLException if this method is called on a closed
     *                      {@code Statement}
     * @since 1.6
     */
    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        stmt.setPoolable(poolable);
    }

    /**
     * Returns a  value indicating whether the {@code Statement}
     * is poolable or not.
     *
     * @return {@code true} if the {@code Statement}
     * is poolable; {@code false} otherwise
     * @throws SQLException if this method is called on a closed
     *                      {@code Statement}
     * @see Statement#setPoolable(boolean) setPoolable(boolean)
     * @since 1.6
     */
    @Override
    public boolean isPoolable() throws SQLException {
        return stmt.isPoolable();
    }

    /**
     * Specifies that this {@code Statement} will be closed when all its
     * dependent result sets are closed. If execution of the {@code Statement}
     * does not produce any result sets, this method has no effect.
     * <p>
     * <strong>Note:</strong> Multiple calls to {@code closeOnCompletion} do
     * not toggle the effect on this {@code Statement}. However, a call to
     * {@code closeOnCompletion} does effect both the subsequent execution of
     * statements, and statements that currently have open, dependent,
     * result sets.
     *
     * @throws SQLException if this method is called on a closed
     *                      {@code Statement}
     * @since 1.7
     */
    @Override
    public void closeOnCompletion() throws SQLException {
        stmt.closeOnCompletion();
    }

    /**
     * Returns a value indicating whether this {@code Statement} will be
     * closed when all its dependent result sets are closed.
     *
     * @return {@code true} if the {@code Statement} will be closed when all
     * of its dependent result sets are closed; {@code false} otherwise
     * @throws SQLException if this method is called on a closed
     *                      {@code Statement}
     * @since 1.7
     */
    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return stmt.isCloseOnCompletion();
    }

    /**
     * Returns an object that implements the given interface to allow access to
     * non-standard methods, or standard methods not exposed by the proxy.
     * <p>
     * If the receiver implements the interface then the result is the receiver
     * or a proxy for the receiver. If the receiver is a wrapper
     * and the wrapped object implements the interface then the result is the
     * wrapped object or a proxy for the wrapped object. Otherwise return the
     * the result of calling {@code unwrap} recursively on the wrapped object
     * or a proxy for that result. If the receiver is not a
     * wrapper and does not implement the interface, then an {@code SQLException} is thrown.
     *
     * @param iface A Class defining an interface that the result must implement.
     * @return an object that implements the interface. May be a proxy for the actual implementing object.
     * @throws SQLException If no object found that implements the interface
     * @since 1.6
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return stmt.unwrap(iface);
    }

    /**
     * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
     * for an object that does. Returns false otherwise. If this implements the interface then return true,
     * else if this is a wrapper then return the result of recursively calling {@code isWrapperFor} on the wrapped
     * object. If this does not implement the interface and is not a wrapper, return false.
     * This method should be implemented as a low-cost operation compared to {@code unwrap} so that
     * callers can use this method to avoid expensive {@code unwrap} calls that may fail. If this method
     * returns true then calling {@code unwrap} with the same argument should succeed.
     *
     * @param iface a Class defining an interface.
     * @return true if this implements the interface or directly or indirectly wraps an object that does.
     * @throws SQLException if an error occurs while determining whether this is a wrapper
     *                      for an object with the given interface.
     * @since 1.6
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return stmt.isWrapperFor(iface);
    }


    /**
     * Retrieves the current result as an update count; if the result
     * is a {@code ResultSet} object or there are no more results, -1
     * is returned. This method should be called only once per result.
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @return the current result as an update count; -1 if the current result
     * is a {@code ResultSet} object or there are no more results
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #execute
     * @since 1.8
     */
    @Override
    public long getLargeUpdateCount() throws SQLException {
        return stmt.getLargeUpdateCount();
    }

    /**
     * Sets the limit for the maximum number of rows that any
     * {@code ResultSet} object  generated by this {@code Statement}
     * object can contain to the given number.
     * If the limit is exceeded, the excess
     * rows are silently dropped.
     * <p>
     * This method should be used when the row limit may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @param max the new max rows limit; zero means there is no limit
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed {@code Statement}
     *                      or the condition {@code max >= 0} is not satisfied
     * @see #getMaxRows
     * @since 1.8
     */
    @Override
    public void setLargeMaxRows(long max) throws SQLException {
        stmt.setLargeMaxRows(max);
    }

    /**
     * Retrieves the maximum number of rows that a
     * {@code ResultSet} object produced by this
     * {@code Statement} object can contain.  If this limit is exceeded,
     * the excess rows are silently dropped.
     * <p>
     * This method should be used when the returned row limit may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * The default implementation will return {@code 0}
     *
     * @return the current maximum number of rows for a {@code ResultSet}
     * object produced by this {@code Statement} object;
     * zero means there is no limit
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed {@code Statement}
     * @see #setMaxRows
     * @since 1.8
     */
    @Override
    public long getLargeMaxRows() throws SQLException {
        return stmt.getLargeMaxRows();
    }

    /**
     * Submits a batch of commands to the database for execution and
     * if all commands execute successfully, returns an array of update counts.
     * The {@code long} elements of the array that is returned are ordered
     * to correspond to the commands in the batch, which are ordered
     * according to the order in which they were added to the batch.
     * The elements in the array returned by the method {@code executeLargeBatch}
     * may be one of the following:
     * <OL>
     * <LI>A number greater than or equal to zero -- indicates that the
     * command was processed successfully and is an update count giving the
     * number of rows in the database that were affected by the command's
     * execution
     * <LI>A value of {@code SUCCESS_NO_INFO} -- indicates that the command was
     * processed successfully but that the number of rows affected is
     * unknown
     * <p>
     * If one of the commands in a batch update fails to execute properly,
     * this method throws a {@code BatchUpdateException}, and a JDBC
     * driver may or may not continue to process the remaining commands in
     * the batch.  However, the driver's behavior must be consistent with a
     * particular DBMS, either always continuing to process commands or never
     * continuing to process commands.  If the driver continues processing
     * after a failure, the array returned by the method
     * {@code BatchUpdateException.getLargeUpdateCounts}
     * will contain as many elements as there are commands in the batch, and
     * at least one of the elements will be the following:
     *
     * <LI>A value of {@code EXECUTE_FAILED} -- indicates that the command failed
     * to execute successfully and occurs only if a driver continues to
     * process commands after a command fails
     * </OL>
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @return an array of update counts containing one element for each
     * command in the batch.  The elements of the array are ordered according
     * to the order in which commands were added to the batch.
     * @throws SQLException        if a database access error occurs,
     *                             this method is called on a closed {@code Statement} or the
     *                             driver does not support batch statements. Throws {@link BatchUpdateException}
     *                             (a subclass of {@code SQLException}) if one of the commands sent to the
     *                             database fails to execute properly or attempts to return a result set.
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     * @see #addBatch
     * @see DatabaseMetaData#supportsBatchUpdates
     * @since 1.8
     */
    @Override
    public long[] executeLargeBatch() throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeLargeBatch();
    }

    /**
     * Executes the given SQL statement, which may be an {@code INSERT},
     * {@code UPDATE}, or {@code DELETE} statement or an
     * SQL statement that returns nothing, such as an SQL DDL statement.
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     * <p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @param sql an SQL Data Manipulation Language (DML) statement,
     *            such as {@code INSERT}, {@code UPDATE} or
     *            {@code DELETE}; or an SQL statement that returns nothing,
     *            such as a DDL statement.
     * @return either (1) the row count for SQL Data Manipulation Language
     * (DML) statements or (2) 0 for SQL statements that return nothing
     * @throws SQLException        if a database access error occurs,
     *                             this method is called on a closed {@code Statement}, the given
     *                             SQL statement produces a {@code ResultSet} object, the method is called on a
     *                             {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLTimeoutException when the driver has determined that the
     *                             timeout value that was specified by the {@code setQueryTimeout}
     *                             method has been exceeded and has at least attempted to cancel
     *                             the currently running {@code Statement}
     * @since 1.8
     */
    @Override
    public long executeLargeUpdate(String sql) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeLargeUpdate(sql);
    }

    /**
     * Executes the given SQL statement and signals the driver with the
     * given flag about whether the
     * auto-generated keys produced by this {@code Statement} object
     * should be made available for retrieval.  The driver will ignore the
     * flag if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     * <p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param sql               an SQL Data Manipulation Language (DML) statement,
     *                          such as {@code INSERT}, {@code UPDATE} or
     *                          {@code DELETE}; or an SQL statement that returns nothing,
     *                          such as a DDL statement.
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *                          should be made available for retrieval;
     *                          one of the following constants:
     *                          {@code Statement.RETURN_GENERATED_KEYS}
     *                          {@code Statement.NO_GENERATED_KEYS}
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement}, the given
     *                                         SQL statement returns a {@code ResultSet} object,
     *                                         the given constant is not one of those allowed, the method is called on a
     *                                         {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @since 1.8
     */
    @Override
    public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeLargeUpdate(sql, autoGeneratedKeys);
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the indexes of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     * <p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param sql           an SQL Data Manipulation Language (DML) statement,
     *                      such as {@code INSERT}, {@code UPDATE} or
     *                      {@code DELETE}; or an SQL statement that returns nothing,
     *                      such as a DDL statement.
     * @param columnIndexes an array of column indexes indicating the columns
     *                      that should be returned from the inserted row
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement}, the SQL
     *                                         statement returns a {@code ResultSet} object,the second argument
     *                                         supplied to this method is not an
     *                                         {@code int} array whose elements are valid column indexes, the method is called on a
     *                                         {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @since 1.8
     */
    @Override
    public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeLargeUpdate(sql, columnIndexes);
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the names of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an {@code INSERT} statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * {@code PreparedStatement} or {@code CallableStatement}.
     * <p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param sql         an SQL Data Manipulation Language (DML) statement,
     *                    such as {@code INSERT}, {@code UPDATE} or
     *                    {@code DELETE}; or an SQL statement that returns nothing,
     *                    such as a DDL statement.
     * @param columnNames an array of the names of the columns that should be
     *                    returned from the inserted row
     * @return either the row count for {@code INSERT}, {@code UPDATE},
     * or {@code DELETE} statements, or 0 for SQL statements
     * that return nothing
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed {@code Statement}, the SQL
     *                                         statement returns a {@code ResultSet} object, the
     *                                         second argument supplied to this method is not a {@code String} array
     *                                         whose elements are valid column names, the method is called on a
     *                                         {@code PreparedStatement} or {@code CallableStatement}
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @since 1.8
     */
    @Override
    public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
        if(stats!=null)stats.executed_statement();
        return stmt.executeLargeUpdate(sql, columnNames);
    }

    /**
     * Returns a {@code String} enclosed in single quotes. Any occurrence of a
     * single quote within the string will be replaced by two single quotes.
     *
     * <blockquote>
     * <table class="striped">
     * <caption>Examples of the conversion:</caption>
     * <thead>
     * <tr><th scope="col">Value</th><th scope="col">Result</th></tr>
     * </thead>
     * <tbody style="text-align:center">
     * <tr> <th scope="row">Hello</th> <td>'Hello'</td> </tr>
     * <tr> <th scope="row">G'Day</th> <td>'G''Day'</td> </tr>
     * <tr> <th scope="row">'G''Day'</th>
     * <td>'''G''''Day'''</td> </tr>
     * <tr> <th scope="row">I'''M</th> <td>'I''''''M'</td>
     * </tr>
     *
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @param val a character string
     * @return A string enclosed by single quotes with every single quote
     * converted to two single quotes
     * @throws NullPointerException if val is {@code null}
     * @throws SQLException         if a database access error occurs
     * @implNote JDBC driver implementations may need to provide their own implementation
     * of this method in order to meet the requirements of the underlying
     * datasource.
     * @since 9
     */
    @Override
    public String enquoteLiteral(String val) throws SQLException {
        return stmt.enquoteLiteral(val);
    }

    /**
     * Returns a SQL identifier. If {@code identifier} is a simple SQL identifier:
     * <ul>
     * <li>Return the original value if {@code alwaysQuote} is
     * {@code false}</li>
     * <li>Return a delimited identifier if {@code alwaysQuote} is
     * {@code true}</li>
     * </ul>
     * <p>
     * If {@code identifier} is not a simple SQL identifier, {@code identifier} will be
     * enclosed in double quotes if not already present. If the datasource does
     * not support double quotes for delimited identifiers, the
     * identifier should be enclosed by the string returned from
     * {@link DatabaseMetaData#getIdentifierQuoteString}.  If the datasource
     * does not support delimited identifiers, a
     * {@code SQLFeatureNotSupportedException} should be thrown.
     * <p>
     * A {@code SQLException} will be thrown if {@code identifier} contains any
     * characters invalid in a delimited identifier or the identifier length is
     * invalid for the datasource.
     *
     * @param identifier  a SQL identifier
     * @param alwaysQuote indicates if a simple SQL identifier should be
     *                    returned as a quoted identifier
     * @return A simple SQL identifier or a delimited identifier
     * @throws SQLException                    if identifier is not a valid identifier
     * @throws SQLFeatureNotSupportedException if the datasource does not support
     *                                         delimited identifiers
     * @throws NullPointerException            if identifier is {@code null}
     * @implSpec The default implementation uses the following criteria to
     * determine a valid simple SQL identifier:
     * <ul>
     * <li>The string is not enclosed in double quotes</li>
     * <li>The first character is an alphabetic character from a through z, or
     * from A through Z</li>
     * <li>The name only contains alphanumeric characters or the character "_"</li>
     * </ul>
     * <p>
     * The default implementation will throw a {@code SQLException} if:
     * <ul>
     * <li>{@code identifier} contains a {@code null} character or double quote and is not
     * a simple SQL identifier.</li>
     * <li>The length of {@code identifier} is less than 1 or greater than 128 characters
     * </ul>
     * <blockquote>
     * <table class="striped" >
     * <caption>Examples of the conversion:</caption>
     * <thead>
     * <tr>
     * <th scope="col">identifier</th>
     * <th scope="col">alwaysQuote</th>
     * <th scope="col">Result</th></tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row">Hello</th>
     * <td>false</td>
     * <td>Hello</td>
     * </tr>
     * <tr>
     * <th scope="row">Hello</th>
     * <td>true</td>
     * <td>"Hello"</td>
     * </tr>
     * <tr>
     * <th scope="row">G'Day</th>
     * <td>false</td>
     * <td>"G'Day"</td>
     * </tr>
     * <tr>
     * <th scope="row">"Bruce Wayne"</th>
     * <td>false</td>
     * <td>"Bruce Wayne"</td>
     * </tr>
     * <tr>
     * <th scope="row">"Bruce Wayne"</th>
     * <td>true</td>
     * <td>"Bruce Wayne"</td>
     * </tr>
     * <tr>
     * <th scope="row">GoodDay$</th>
     * <td>false</td>
     * <td>"GoodDay$"</td>
     * </tr>
     * <tr>
     * <th scope="row">Hello"World</th>
     * <td>false</td>
     * <td>SQLException</td>
     * </tr>
     * <tr>
     * <th scope="row">"Hello"World"</th>
     * <td>false</td>
     * <td>SQLException</td>
     * </tr>
     * </tbody>
     * </table>
     * </blockquote>
     * @implNote JDBC driver implementations may need to provide their own implementation
     * of this method in order to meet the requirements of the underlying
     * datasource.
     * @since 9
     */
    @Override
    public String enquoteIdentifier(String identifier, boolean alwaysQuote) throws SQLException {
        return stmt.enquoteIdentifier(identifier, alwaysQuote);
    }

    /**
     * Retrieves whether {@code identifier} is a simple SQL identifier.
     *
     * @param identifier a SQL identifier
     * @return true if  a simple SQL identifier, false otherwise
     * @throws NullPointerException if identifier is {@code null}
     * @throws SQLException         if a database access error occurs
     * @implSpec The default implementation uses the following criteria to
     * determine a valid simple SQL identifier:
     * <ul>
     * <li>The string is not enclosed in double quotes</li>
     * <li>The first character is an alphabetic character from a through z, or
     * from A through Z</li>
     * <li>The string only contains alphanumeric characters or the character
     * "_"</li>
     * <li>The string is between 1 and 128 characters in length inclusive</li>
     * </ul>
     *
     * <blockquote>
     * <table class="striped" >
     * <caption>Examples of the conversion:</caption>
     * <thead>
     * <tr>
     * <th scope="col">identifier</th>
     * <th scope="col">Simple Identifier</th>
     * </thead>
     *
     * <tbody>
     * <tr>
     * <th scope="row">Hello</th>
     * <td>true</td>
     * </tr>
     * <tr>
     * <th scope="row">G'Day</th>
     * <td>false</td>
     * </tr>
     * <tr>
     * <th scope="row">"Bruce Wayne"</th>
     * <td>false</td>
     * </tr>
     * <tr>
     * <th scope="row">GoodDay$</th>
     * <td>false</td>
     * </tr>
     * <tr>
     * <th scope="row">Hello"World</th>
     * <td>false</td>
     * </tr>
     * <tr>
     * <th scope="row">"Hello"World"</th>
     * <td>false</td>
     * </tr>
     * </tbody>
     * </table>
     * </blockquote>
     * @implNote JDBC driver implementations may need to provide their own
     * implementation of this method in order to meet the requirements of the
     * underlying datasource.
     * @since 9
     */
    @Override
    public boolean isSimpleIdentifier(String identifier) throws SQLException {
        return stmt.isSimpleIdentifier(identifier);
    }

    /**
     * Returns a {@code String} representing a National Character Set Literal
     * enclosed in single quotes and prefixed with a upper case letter N.
     * Any occurrence of a single quote within the string will be replaced
     * by two single quotes.
     *
     * <blockquote>
     * <table class="striped">
     * <caption>Examples of the conversion:</caption>
     * <thead>
     * <tr>
     * <th scope="col">Value</th>
     * <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr> <th scope="row">Hello</th> <td>N'Hello'</td> </tr>
     * <tr> <th scope="row">G'Day</th> <td>N'G''Day'</td> </tr>
     * <tr> <th scope="row">'G''Day'</th>
     * <td>N'''G''''Day'''</td> </tr>
     * <tr> <th scope="row">I'''M</th> <td>N'I''''''M'</td>
     * <tr> <th scope="row">N'Hello'</th> <td>N'N''Hello'''</td> </tr>
     *
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @param val a character string
     * @return the result of replacing every single quote character in the
     * argument by two single quote characters where this entire result is
     * then prefixed with 'N'.
     * @throws NullPointerException if val is {@code null}
     * @throws SQLException         if a database access error occurs
     * @implNote JDBC driver implementations may need to provide their own implementation
     * of this method in order to meet the requirements of the underlying
     * datasource. An implementation of enquoteNCharLiteral may accept a different
     * set of characters than that accepted by the same drivers implementation of
     * enquoteLiteral.
     * @since 9
     */
    @Override
    public String enquoteNCharLiteral(String val) throws SQLException {
        return stmt.enquoteNCharLiteral(val);
    }
}
