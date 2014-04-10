package com.openatk.trello.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class AppsTable {
	// Contacts table name
	public static final String TABLE_NAME = "apps";
 
    // Contacts Table Columns names
    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_PACKAGE_NAME = "package_name";
    public static final String COL_ALLOW_SYNCING = "allow_syncing";
    public static final String COL_AUTO_SYNC = "auto_sync";
    public static final String COL_BOARD_NAME = "board_name";
    public static final String COL_LAST_SYNC = "lastsync";
    public static final String COL_LAST_ACTION = "lastaction"; //Added Jan23, need upgrade
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ "(" 
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
            + COL_NAME + " VARCHAR(200),"
            + COL_PACKAGE_NAME + " VARCHAR(200),"
            + COL_ALLOW_SYNCING + " INTEGER,"
            + COL_AUTO_SYNC + " INTEGER,"
            + COL_BOARD_NAME + " VARCHAR(50),"
            + COL_LAST_SYNC + " VARCHAR(50),"
            + COL_LAST_ACTION + " VARCHAR(50)"
    		+ ")";
    
    // Creating Tables
    public static void onCreate(SQLiteDatabase db) {    	
        db.execSQL(CREATE_TABLE); 
    }
 
    // Upgrading database
    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	Log.d("AppsTable - onUpgrade", "Upgrade from " + Integer.toString(oldVersion) + " to " + Integer.toString(newVersion));
    	
    	int version = oldVersion;
    	switch(version){
    		case 1: //Launch
    			//Do nothing this is the gplay launch version
    		case 2: //V2
    			//Nothing changed for version 2 in this table
    	}
        // Drop older table if existed
        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // Create tables again
        //onCreate(db);
    	
    	//TODO need upgrade new version, added lastaction
    }
}
