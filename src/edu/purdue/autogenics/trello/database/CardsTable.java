package edu.purdue.autogenics.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class CardsTable {
	// Table name
	public static final String TABLE_NAME = "cards";
 
    // Table Columns names
    public static final String COL_TRELLO_ID = "_id"; //How to tell if new board or not
    public static final String COL_DATE = "date";
    public static final String COL_SYNCED = "synced";
    public static final String COL_LIST_ID = "list_id";
    public static final String COL_OWNER = "owner";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ " (" 
            + COL_TRELLO_ID + " VARCHAR(50) PRIMARY KEY," 
    		+ COL_DATE + " VARCHAR(50),"
            + COL_SYNCED + " INTEGER,"
            + COL_LIST_ID + " VARCHAR(50),"
            + COL_OWNER + " VARCHAR(200)"
    		+ ")";
    
    // Creating Tables
    public static void onCreate(SQLiteDatabase db) {    	
        db.execSQL(CREATE_TABLE); 
    }
 
    // Upgrading database
    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // Create tables again
        onCreate(db);
    }
}