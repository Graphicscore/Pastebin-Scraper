package de.graphicscore.pastebinscraper;

import com.sun.istack.internal.Nullable;
import org.json.JSONArray;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class PastebinScraper {

    private static Connection connection;

    private static URL SCRAPING_API_URL;
    private static long SCRAPE_INTERVAL = 60 * 1000;

    private static HashMap<String, Paste> mPasteCache;

    private static File startupDir;
    private static Properties settings;

    private static Thread storeThread;
    private static Thread scrapeThread;

    public static void main(String[] args){
        System.out.println("Running");

        startupDir = new File(System.getProperty("user.dir"));
        try {
            System.out.println("Working Directory : " + startupDir);
            loadSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }

        connection = setupMysqlConnection();
        if(connection != null){
            System.out.println("Database connection established !");
            mPasteCache = new HashMap<>();
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
            SCRAPING_API_URL = new URL("https://pastebin.com/api_scraping.php?limit=100");
            scrapeThread = new Thread(PastebinScraper::bulkScrape,"ScrapeThread");
            scrapeThread.start();
        } catch (Exception e) {
            System.err.println("Could not parse scraping url " + e.getMessage());
        }
    }

    private static @Nullable Connection setupMysqlConnection(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            System.err.println("Driver Not Found Error " + ex);
        }
        try {
            return DriverManager.getConnection("jdbc:mysql://" + settings.getProperty("db_address") + "/"  + settings.getProperty("db_database") + "?" +
                            "user=" + settings.getProperty("db_user") + "&password=" + settings.getProperty("db_user_password"));
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
                Set<String> keys = new HashSet<>(mPasteCache.keySet());
                for(String key : keys){
                    if(!isPasteStored(mPasteCache.get(key))) {
                        if (storePaste(mPasteCache.get(key))) {
                            System.out.println("Successfully stored : " + key);
                            mPasteCache.remove(key);
                        } else {
                            System.err.println("Error storing : " + key);
                        }
                    }else{
                        System.out.println("Already stored : " + key);
                        mPasteCache.remove(key);
                    }
                }
            }
            try {
                Thread.sleep(SCRAPE_INTERVAL / 4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean storePaste(Paste paste){
        boolean result = false;
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(paste.getScrapeUrl()).openConnection();
            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));

            String input;

            File pasteFile = new File("/Users/dlouven/pastebin",paste.getKey() + ".paste");

            if(!pasteFile.exists()){
                pasteFile.createNewFile();
            }

            FileOutputStream pasteOutputStream = new FileOutputStream(pasteFile);
            StringBuilder builder = new StringBuilder();


            while ((input = br.readLine()) != null) {
                pasteOutputStream.write(input.getBytes(Charset.forName("UTF-8")));
                builder.append(input);
            }
            br.close();
            pasteOutputStream.flush();
            pasteOutputStream.close();

            try {
                PreparedStatement statement = PastebinScraper.connection.prepareStatement("insert into pastes values(0,?,?,?,?,?,?,?,?,?,?)");
                statement.setString(1,paste.getScrapeUrl());
                statement.setString(2,paste.getFullUrl());
                statement.setString(3,paste.getDateAsString());
                statement.setString(4,paste.getKey());
                statement.setInt(5,(int)paste.getSize());
                statement.setString(6,paste.getExpireAsString());
                statement.setString(7,paste.getTitle());
                statement.setString(8,paste.getSyntax());
                statement.setString(9,paste.getUser());
                statement.setString(10,builder.toString());
                statement.execute();
                statement.close();
                result = true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }catch (Exception e){
            System.err.println("Could not store paste "+ e.getMessage());
            e.printStackTrace();
        }
        return result;
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
                System.out.println("PasteCache Size : " + mPasteCache.size());
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

    private static void loadSettings() throws IOException {
        File settingsFile = new File(startupDir,"scraper.conf");
        settings = new Properties();
        settings.setProperty("db_user","");
        settings.setProperty("db_user_password","");
        settings.setProperty("db_database","");
        settings.setProperty("db_address","");
        settings.setProperty("scraper_interval","60");
        settings.setProperty("scraper_limit","100");
        settings.setProperty("store_interval","15");
        if(settingsFile.exists()){
            FileInputStream input = new FileInputStream(settingsFile);
            settings.load(input);
            input.close();
        }else{
            FileOutputStream out = new FileOutputStream(settingsFile);
            settings.store(out,"Scraper Configuration");
            out.close();
        }
    }
}
