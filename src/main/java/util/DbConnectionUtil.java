package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class responsible for establishing and managing the application's
 * JDBC database connection.
 *
 * <p>This class:</p>
 * <ul>
 *   <li>Loads database configuration from {@code database.properties}</li>
 *   <li>Initializes a single shared {@link Connection} (simple singleton)</li>
 *   <li>Provides controlled access to that connection</li>
 *   <li>Handles safe shutdown of the connection</li>
 * </ul>
 *
 * <p><strong>Design notes:</strong></p>
 * <ul>
 *   <li>Connection is created eagerly in a static initializer</li>
 *   <li>Failures during initialization are treated as fatal</li>
 *   <li>This approach is appropriate for a small CLI application (Project 0)</li>
 *   <li>Not intended for high-concurrency or connection-pooling scenarios</li>
 * </ul>
 *
 * <p><strong>Required properties (on classpath):</strong></p>
 * <ul>
 *   <li>{@code db.driver}</li>
 *   <li>{@code db.url}</li>
 *   <li>{@code db.username}</li>
 *   <li>{@code db.password}</li>
 * </ul>
 */
public class DbConnectionUtil {

    /**
     * Logger for database connection lifecycle events.
     */
    private static final Logger log = LoggerFactory.getLogger(DbConnectionUtil.class);

    /**
     * Shared JDBC connection instance.
     */
    private static Connection connection;

    /**
     * Static initializer that loads configuration and establishes
     * the database connection exactly once.
     *
     * <p>If initialization fails, a {@link RuntimeException} is thrown
     * to prevent the application from running in a partially configured state.</p>
     */
    static {
        if (connection == null) {
            Properties properties = new Properties();

            try (InputStream input =
                         DbConnectionUtil.class
                                 .getClassLoader()
                                 .getResourceAsStream("database.properties")) {

                if (input == null) {
                    log.error("Unable to find database.properties on the classpath.");
                    throw new RuntimeException("Unable to find database.properties");
                }

                // Load properties file
                properties.load(input);
                log.debug("Loaded database.properties successfully.");

                String driver = properties.getProperty("db.driver");
                String url = properties.getProperty("db.url");
                String username = properties.getProperty("db.username");
                String password = properties.getProperty("db.password");

                // Validate required properties
                if (driver == null || url == null || username == null || password == null) {
                    log.error(
                            "Missing required database properties. " +
                                    "Required keys: db.driver, db.url, db.username, db.password"
                    );
                    throw new RuntimeException("Missing required database properties.");
                }

                // Load JDBC driver class
                Class.forName(driver);
                log.debug("JDBC driver loaded: {}", driver);

                // Establish database connection
                connection = DriverManager.getConnection(url, username, password);

                // Avoid logging sensitive data (password)
                log.info(
                        "Database connection established successfully (url={}, username={}).",
                        url,
                        username
                );

            } catch (IOException e) {
                log.error("Failed to load database configuration (IOException).", e);
                throw new RuntimeException(
                        "Failed to load database configuration (IOException).", e
                );

            } catch (ClassNotFoundException e) {
                log.error("JDBC Driver class not found.", e);
                throw new RuntimeException(
                        "JDBC Driver class not found.", e
                );

            } catch (SQLException e) {
                log.error("Failed to establish database connection (SQLException).", e);
                throw new RuntimeException(
                        "Failed to establish database connection.", e
                );

            } catch (RuntimeException e) {
                // Preserve explicit runtime failures (e.g., missing properties)
                throw e;

            } catch (Exception e) {
                log.error("Unexpected error while initializing database connection.", e);
                throw new RuntimeException(
                        "Unexpected error while initializing database connection.", e
                );
            }
        }
    }

    /**
     * Returns the active JDBC connection.
     *
     * @return initialized {@link Connection}
     * @throws RuntimeException if the connection was not successfully initialized
     */
    public static Connection getConnection() {
        if (connection == null) {
            log.error("Connection requested, but connection is null (initialization failed).");
            throw new RuntimeException("Connection failed to set up correctly.");
        }
        return connection;
    }

    /**
     * Closes the active database connection and clears the reference.
     *
     * <p>This method is safe to call multiple times.</p>
     * <ul>
     *   <li>If the connection is open, it will be closed</li>
     *   <li>If the connection is already {@code null}, nothing happens</li>
     * </ul>
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                log.info("Database connection closed successfully.");
            } catch (SQLException e) {
                log.error("Failed to close database connection.", e);
            } finally {
                connection = null;
            }
        } else {
            log.debug("closeConnection() called, but connection was already null.");
        }
    }
}
