package de.graphicscore.pastebinscraper;

import org.json.JSONObject;

public class Paste{

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

    private JSONObject mJSON;

    public Paste(JSONObject object){
        this.mJSON = object;
    }


    public String getUser() { return mJSON.getString(KEY_USER); }
    public String getSyntax() { return mJSON.getString(KEY_SYNTAX); }
    public String getTitle() { return mJSON.getString(KEY_TITLE); }
    public String getScrapeUrl() { return mJSON.getString(KEY_SCRAPE_URL); }
    public String getFullUrl() { return mJSON.getString(KEY_FULL_URL); }
    public long getDate() { return mJSON.getLong(KEY_DATE);}
    public long getSize() { return mJSON.getLong(KEY_SIZE);}
    public long getExpire() { return mJSON.getLong(KEY_EXPIRE);}
    public String getKey() { return mJSON.getString(KEY_KEY);}
    public void print(){
        System.out.println(getKey());
        System.out.println(getUser());
        System.out.println(getTitle());
        System.out.println(getScrapeUrl());
        System.out.println(getFullUrl());
        System.out.println(getDate());
        System.out.println(getSize());
        System.out.println(getExpire());
    }
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
