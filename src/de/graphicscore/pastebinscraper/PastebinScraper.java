package de.graphicscore.pastebinscraper;

import com.sun.istack.internal.Nullable;
import org.json.JSONArray;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PastebinScraper {

    private static Connection connection;

    private static URL SCRAPING_API_URL;

    public static void main(String[] args){
        System.out.println("Running");
        String mysqlUser = args[0];
        String mysqlPassword = args[1];
        String mysqlDatabse = args[2];
        String serverDomain = args[3];
        connection = setupMysqlConnection(serverDomain, mysqlDatabse,mysqlUser,mysqlPassword);
        if(connection != null){
            try {
                SCRAPING_API_URL = new URL("https://pastebin.com/api_scraping.php");
                bulkScrape();
            } catch (MalformedURLException e) {
                System.err.println("Could not parse scraping url " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("Could not init mysql connection, abort!");
        }
    }

    private static @Nullable Connection setupMysqlConnection(String domain, String databaseName, String user, String password){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
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

    private static void bulkScrape() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) SCRAPING_API_URL.openConnection();
        BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));

        String input;
        StringBuilder builder = new StringBuilder();
        while ((input = br.readLine()) != null){
            builder.append(input);
        }
        br.close();

        JSONArray result = new JSONArray(builder.toString());
        for(int i = 0; i < result.length(); i++){
            new Paste(result.getJSONObject(i)).print();
        }
    }
}
