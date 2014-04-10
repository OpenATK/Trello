package com.openatk.trello.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class OrganizationMembersTable {
	// Table name
	public static final String TABLE_NAME = "organizationMembers";
 
    // Table Columns names
    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_MEMBER_ID = "member_id";
    public static final String COL_USERNAME = "username";
    public static final String COL_ORGO_ID = "orgo_id";

    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ " (" 
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
    		+ COL_NAME + " VARCHAR(100),"
    		+ COL_MEMBER_ID + " VARCHAR(50),"
    		+ COL_USERNAME + " VARCHAR(100),"
    		+ COL_ORGO_ID + " VARCHAR(50)"
    		+ ")";
    
    // Creating Tables
    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE); 
    }
 
    // Upgrading database
    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	Log.d("OrganizationMembersTable - onUpgrade", "Upgrade from " + Integer.toString(oldVersion) + " to " + Integer.toString(newVersion));
    	int version = oldVersion;
    	switch(version){
    		case 1: //Launch
    			//Do nothing this is the gplay launch version
    		case 2: //V2
    			//This table was added in version 2
    	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    	        onCreate(db);
    	}
    }
}
