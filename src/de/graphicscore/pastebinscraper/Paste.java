package de.graphicscore.pastebinscraper;

import org.json.JSONObject;

public class Paste extends JSONObject{

    private static final String
    KEY_USER = "user",
    KEY_SYNTAX = "syntax",
    KEY_TITLE = "title",
    KEY_EXPIRE = "expire",
    KEY_SIZE = "size",
    KEY_KEY = "key",
    KEY_DATE = "date",
    KEY_FULL_URL = "full_url",
    KEY_SCRAPE_URL = "scrape_url";

    public String getUser() { return getString(KEY_USER); }
    public String getSyntax() { return getString(KEY_SYNTAX); }
    public String getTitle() { return getString(KEY_TITLE); }
    public String getScrapeUrl() { return getString(KEY_SCRAPE_URL); }
    public String getFullUrl() { return getString(KEY_FULL_URL); }
    public long getDate() { return getLong(KEY_DATE);}
    public long getSize() { return getLong(KEY_SIZE);}
    public long getExpire() { return getLong(KEY_EXPIRE);}
    public String getKey() { return getString(KEY_KEY);}
}

/*
{
        "scrape_url": "",
        "full_url": "",
        "date": "",
        "key": "",
        "size": "",
        "expire": "0",
        "title": "",
        "syntax": "",
        "user": ""
    }
 */
