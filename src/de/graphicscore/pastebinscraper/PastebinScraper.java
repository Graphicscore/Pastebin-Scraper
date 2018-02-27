package de.graphicscore.pastebinscraper;

import com.sun.istack.internal.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Set;

public class PastebinScraper {

    private static Connection connection;

    private static URL SCRAPING_API_URL;
    private static long SCRAPE_INTERVAL = 60 * 1000;

    private static HashMap<String, Paste> mPasteCache;

    private static Thread storeThread;
    private static Thread scrapeThread;

    public static void main(String[] args){
        System.out.println("Running");
        String mysqlUser = args[0];
        String mysqlPassword = args[1];
        String mysqlDatabse = args[2];
        String serverDomain = args[3];
        connection = setupMysqlConnection(serverDomain, mysqlDatabse,mysqlUser,mysqlPassword);
        if(connection != null){
            System.out.println("Database connection established !");
            isPasteStored(new Paste(new JSONObject("{key:\"ABCDF\"}")));
            setupScrapeThread();
            setupStoreThread();
        }else{
            System.out.println("Could not init mysql connection, abort!");
        }
    }

    private static void setupStoreThread(){
        storeThread = new Thread(PastebinScraper::bulkStore,"StoreThread");
        storeThread.start();
    }

    private static void setupScrapeThread(){
        try {
            SCRAPING_API_URL = new URL("https://pastebin.com/api_scraping.php");
            scrapeThread = new Thread(PastebinScraper::bulkScrape,"ScrapeThread");
            scrapeThread.start();
        } catch (MalformedURLException e) {
            System.err.println("Could not parse scraping url " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
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

    private static void bulkStore(){
        while(storeThread.isAlive()){
            if(mPasteCache.size() > 0){
                Set<String> keys = mPasteCache.keySet();
                for(String key : keys){
                    if(!isPasteStored(mPasteCache.get(key))) {
                        /*if (mPasteCache.get(key)) {
                            System.out.println("Successfully stored : " + key);
                            mPasteCache.remove(key);
                        } else {
                            System.err.println("Error storing : " + key);
                        }*/
                    }else{
                        System.out.println("Already stored : " + key);
                    }
                }
            }
        }
    }

    private static boolean isPasteStored(Paste paste){
        boolean result = false;
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM pastes WHERE paste_key LIKE '" + paste.getKey() + "'");
            if(resultSet.next()){
                result = true;
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void bulkScrape() {
        while (scrapeThread.isAlive()) {
            try {
                HttpsURLConnection connection = (HttpsURLConnection) SCRAPING_API_URL.openConnection();
                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(connection.getInputStream()));

                String input;
                StringBuilder builder = new StringBuilder();
                while ((input = br.readLine()) != null) {
                    builder.append(input);
                }
                br.close();

                JSONArray result = new JSONArray(builder.toString());
                for (int i = 0; i < result.length(); i++) {
                    Paste newPaste = new Paste(result.getJSONObject(i));
                    mPasteCache.putIfAbsent(newPaste.getKey(), newPaste);
                }
            }catch (Exception e){
                System.out.println("Error scraping latest pastes " + e.getMessage());
                e.printStackTrace();
            }
            try {
                Thread.sleep(SCRAPE_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
