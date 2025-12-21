package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbConnectionUtil {

    private static final Logger log = LoggerFactory.getLogger(DbConnectionUtil.class);

    private static Connection connection;

    static {
        if (connection == null) {
            Properties properties = new Properties();

            try (InputStream input =
                         DbConnectionUtil.class.getClassLoader().getResourceAsStream("database.properties")) {

                if (input == null) {
                    log.error("Unable to find database.properties on the classpath.");
                    throw new RuntimeException("Unable to find database.properties");
                }

                properties.load(input);
                log.debug("Loaded database.properties successfully.");

                String driver = properties.getProperty("db.driver");
                String url = properties.getProperty("db.url");
                String username = properties.getProperty("db.username");
                String password = properties.getProperty("db.password");

                if (driver == null || url == null || username == null || password == null) {
                    log.error("Missing required database properties. Required keys: db.driver, db.url, db.username, db.password");
                    throw new RuntimeException("Missing required database properties.");
                }

                // Load JDBC Driver
                Class.forName(driver);
                log.debug("JDBC driver loaded: {}", driver);

                connection = DriverManager.getConnection(url, username, password);

                // Avoid logging password
                log.info("Database connection established successfully (url={}, username={}).", url, username);

            } catch (IOException e) {
                log.error("Failed to load database configuration (IOException).", e);
                throw new RuntimeException("Failed to load database configuration (IOException).", e);

            } catch (ClassNotFoundException e) {
                log.error("JDBC Driver class not found.", e);
                throw new RuntimeException("JDBC Driver class not found.", e);

            } catch (SQLException e) {
                log.error("Failed to establish database connection (SQLException).", e);
                throw new RuntimeException("Failed to establish database connection.", e);

            } catch (RuntimeException e) {
                // Preserve our own runtime exceptions (like missing props)
                throw e;

            } catch (Exception e) {
                log.error("Unexpected error while initializing database connection.", e);
                throw new RuntimeException("Unexpected error while initializing database connection.", e);
            }
        }
    }

    public static Connection getConnection() throws RuntimeException {
        if (connection == null) {
            log.error("Connection requested, but connection is null (initialization failed).");
            throw new RuntimeException("Connection failed to setup correctly.");
        }
        return connection;
    }

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
