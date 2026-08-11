package org.ugoptimizer.database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Opens configured SQLite databases and initializes the project schema.
 *
 * <p>Each call to {@link #openConnection()} returns a new connection with
 * SQLite foreign-key enforcement enabled. The manager does not retain a
 * mutable global connection; callers own and close the connections they open.</p>
 *
 * <p>For the current Maven project layout, the schema is read from
 * {@code database/schema.sql} relative to the process working directory. This
 * keeps one schema source of truth, but a packaged application launched outside
 * the project directory will eventually need a classpath-resource strategy.</p>
 */
public final class DatabaseManager {

    private static final String JDBC_PREFIX = "jdbc:sqlite:";
    private static final Path PROJECT_SCHEMA_PATH = Path.of("database", "schema.sql");

    private final Path databasePath;
    private final Path schemaPath;

    /**
     * Creates a manager for the supplied SQLite database file.
     *
     * @param databasePath configurable database file path
     * @throws NullPointerException if {@code databasePath} is {@code null}
     */
    public DatabaseManager(Path databasePath) {
        this.databasePath = Objects.requireNonNull(
                databasePath, "databasePath cannot be null").toAbsolutePath().normalize();
        this.schemaPath = PROJECT_SCHEMA_PATH.toAbsolutePath().normalize();
    }

    /** Returns the normalized absolute path of the configured database file. */
    public Path getDatabasePath() {
        return databasePath;
    }

    /** Returns the JDBC URL used to open the configured SQLite database. */
    public String getJdbcUrl() {
        return JDBC_PREFIX + databasePath;
    }

    /**
     * Opens a new connection and enables foreign-key enforcement on it.
     *
     * @return a new caller-owned connection
     * @throws SQLException if the connection cannot be opened or foreign keys
     *         cannot be enabled
     */
    public Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(getJdbcUrl());
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            if (!isForeignKeyEnforcementEnabled(connection)) {
                throw new SQLException("SQLite foreign-key enforcement could not be enabled");
            }
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    /**
     * Reports whether SQLite foreign-key enforcement is active for a connection.
     *
     * @param connection connection to inspect
     * @return {@code true} when {@code PRAGMA foreign_keys} is {@code 1}
     * @throws NullPointerException if {@code connection} is {@code null}
     * @throws SQLException if the PRAGMA cannot be queried
     */
    public boolean isForeignKeyEnforcementEnabled(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection cannot be null");
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA foreign_keys")) {
            return resultSet.next() && resultSet.getInt(1) == 1;
        }
    }

    /**
     * Creates the configured database when necessary and executes the complete
     * project schema in one transaction. Repeated calls are safe because the
     * schema uses {@code IF NOT EXISTS}.
     *
     * @throws IOException if {@code database/schema.sql} cannot be read
     * @throws SQLException if a connection or schema statement fails
     */
    public void initializeSchema() throws IOException, SQLException {
        String script;
        try {
            script = Files.readString(schemaPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IOException("Unable to read SQLite schema at " + schemaPath, exception);
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                executeSqlScript(connection, script);
                connection.commit();
            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }

    /**
     * Executes the current schema format statement by statement. Semicolons in
     * quoted text are retained, while line and block comments are ignored.
     */
    private static void executeSqlScript(Connection connection, String script) throws SQLException {
        StringBuilder sql = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        int statementNumber = 0;

        try (Statement statement = connection.createStatement()) {
            for (int index = 0; index < script.length(); index++) {
                char current = script.charAt(index);
                char next = index + 1 < script.length() ? script.charAt(index + 1) : '\0';

                if (inLineComment) {
                    if (current == '\n') {
                        inLineComment = false;
                        sql.append('\n');
                    }
                    continue;
                }

                if (inBlockComment) {
                    if (current == '*' && next == '/') {
                        inBlockComment = false;
                        index++;
                        sql.append(' ');
                    }
                    continue;
                }

                if (!inSingleQuote && !inDoubleQuote) {
                    if (current == '-' && next == '-') {
                        inLineComment = true;
                        index++;
                        continue;
                    }
                    if (current == '/' && next == '*') {
                        inBlockComment = true;
                        index++;
                        continue;
                    }
                    if (current == ';') {
                        statementNumber = executeStatement(statement, sql, statementNumber);
                        continue;
                    }
                }

                sql.append(current);
                if (current == '\'' && !inDoubleQuote) {
                    if (inSingleQuote && next == '\'') {
                        sql.append(next);
                        index++;
                    } else {
                        inSingleQuote = !inSingleQuote;
                    }
                } else if (current == '"' && !inSingleQuote) {
                    if (inDoubleQuote && next == '"') {
                        sql.append(next);
                        index++;
                    } else {
                        inDoubleQuote = !inDoubleQuote;
                    }
                }
            }

            if (inSingleQuote || inDoubleQuote || inBlockComment) {
                throw new SQLException("SQLite schema contains an unterminated quote or comment");
            }
            executeStatement(statement, sql, statementNumber);
        }
    }

    private static int executeStatement(
            Statement statement, StringBuilder sql, int completedStatements) throws SQLException {
        String command = sql.toString().trim();
        sql.setLength(0);
        if (command.isEmpty()) {
            return completedStatements;
        }

        int statementNumber = completedStatements + 1;
        try {
            statement.execute(command);
        } catch (SQLException exception) {
            throw new SQLException(
                    "Failed to execute SQLite schema statement "
                            + statementNumber
                            + ": "
                            + summarize(command),
                    exception);
        }
        return statementNumber;
    }

    private static String summarize(String sql) {
        String singleLine = sql.replaceAll("\\s+", " ");
        int maximumLength = 120;
        return singleLine.length() <= maximumLength
                ? singleLine
                : singleLine.substring(0, maximumLength) + "...";
    }
}
