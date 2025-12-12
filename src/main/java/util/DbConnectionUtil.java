package util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbConnectionUtil {

    private static Connection connection;

    static {
        if (connection == null){
            Properties properties = new Properties();

            try (InputStream input = DbConnectionUtil.class.getClassLoader().getResourceAsStream("database.properties")){

                if(input == null){
                    throw new Exception("Unable to find database.properties");
                }else{
                    properties.load(input);
                }

                // Load JDBC Driver
                Class.forName(properties.getProperty("db.driver"));

                connection = DriverManager.getConnection(
                        properties.getProperty("db.url"),
                        properties.getProperty("db.username"),
                        properties.getProperty("db.password")
                );

            }catch(IOException e){
                throw new RuntimeException("Failed to load database configuration: IOException");
            }catch(ClassNotFoundException e){
                throw new RuntimeException("Class not found Exception");
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    public static Connection getConnection() throws RuntimeException {
        if(connection == null){
            throw new RuntimeException("Connection failed to setup correctly");
        }

        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}