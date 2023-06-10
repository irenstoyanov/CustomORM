package ORM;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * The Connector class is used to make a connection with a MySQL database.
 *
 * After making a connection with a database of you choice, you can use it
 * to create an instance of the EntityManager interface. With it, you
 * can do all kinds of MySQL operations, with the database
 * you are connected to, from your Java code.
 *
 * @see EntityManager
 */
public class Connector {

    private static Connection connection;

    /**
     * Used to create a connection with a MySQL database
     * using the given parameters.
     *
     * @param username The name of the user
     * @param password The password of the user
     * @param databaseName The name of the database
     * @throws SQLException If any database access errors occur
     */
    public static void createConnection(String username, String password, String databaseName) throws SQLException {
        if (connection != null) {
            String message = String.format("Connection already made with database \"%s\".", databaseName);
            throw new IllegalArgumentException(message);
        }

        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);

        String connectionString = "jdbc:mysql://localhost:3306/" + databaseName;

        connection = DriverManager.getConnection(connectionString, properties);
    }

    /**
     * Used to return the connection created from
     * the method createConnection.
     *
     * @return A connection to the given database
     */
    public static Connection getConnection() {
        return connection;
    }
}
