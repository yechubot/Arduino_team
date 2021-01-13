package com.example.cs50.arduino;

import android.provider.BaseColumns;

public final class StrollDB {
    private  StrollDB(){}

    //테이블
    public static class StrollEntry implements BaseColumns{
        public static final String TBL_NAME = "strollTBL";
        public static final String COL_NAME_NOTIFICATION_TIME = "notification_time";
        public static final String COL_NAME_STROLL_COUNTS= "stroll_counts";
    }
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + StrollEntry.TBL_NAME + " ("+
                    StrollEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"+
                    StrollEntry.COL_NAME_NOTIFICATION_TIME+" TEXT, " +
                    StrollEntry.COL_NAME_STROLL_COUNTS + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + StrollEntry.TBL_NAME;
}
