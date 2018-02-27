package de.graphicscore.pastebinscraper;

import com.sun.istack.internal.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PastebinScraper {

    private static Connection connection;

    public static void main(String[] args){
        System.out.println("Running");
        String mysqlUser = args[0];
        String mysqlPassword = args[1];
        String mysqlDatabse = args[2];
        String serverDomain = args[3];
        connection = setupMysqlConnection(serverDomain, mysqlDatabse,mysqlUser,mysqlPassword);
        if(connection != null){

        }else{
            System.out.println("Could not init mysql connection, abort!");
        }
    }

    private static @Nullable Connection setupMysqlConnection(String domain, String databaseName, String user, String password){
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            System.err.println("Driver Not Found Error " + ex);
        }
        try {
            return DriverManager.getConnection("jdbc:mysql://" + domain + "/"  + databaseName + "?" +
                            "user=" + user + "&password=" + password);
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return null;
    }
}
