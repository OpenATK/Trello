package com.openatk.trello.database;


import java.util.Date;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {
	 
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 2;
 
    // Database Name
    private static final String DATABASE_NAME = "trello.db";
 
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }    
    
    // Creating tables
    @Override
    public void onCreate(SQLiteDatabase db) {    	
        AppsTable.onCreate(db);
        LoginsTable.onCreate(db);
        OrganizationMembersTable.onCreate(db);
    }
 
    // Upgrading tables
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	AppsTable.onUpgrade(db, oldVersion, newVersion);
    	LoginsTable.onUpgrade(db, oldVersion, newVersion);
    	OrganizationMembersTable.onUpgrade(db, oldVersion, newVersion);
    }
    
    public static Date stringToDate(String date) {
		if(date == null){
			return new Date(0);
		}
		Date d;
		try {
			d = new Date((Long.parseLong(date)));
		} catch (Exception e) {
			d = new Date(0);
		}
		return d;
	}
	public static String dateToUnixString(Date date) {
		if(date == null){
			return null;
		}
		return Long.toString((date.getTime()));
	}
}
