package com.openatk.trello.sync_adapter;

import com.google.gson.Gson;
import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.database.LoginsTable;
import com.openatk.trello.database.OrganizationMembersTable;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/*
 * The provider which manages the "apps"
 * 
 * This should not be used directly but rather
 * accessed via the LibWaterApps Rock class.
 */
public class SyncProvider extends ContentProvider {
	
	private DatabaseHandler db;
	private static final int PACKAGES = 1;
	private static final int PACKAGE_ID = 2;
	private static final int PACKAGE = 3;
	private static final int LOGINS = 4;
	private static final int ORGANIZATION_MEMBERS = 5;
	private static final int ACTIVE_ORGANIZATION = 6;


	private static final String AUTHORITY = "com.openatk.trello.provider";
	private static final String BASE_PATH = "apps";
	private static final String LOGINS_PATH = "logins";
	private static final String MEMBERS_PATH = "organization_members";
	private static final String ACTIVE_ORGANIZATION_PATH = "activeOrganization";

	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	public static final Uri CONTENT_URI_LOGINS = Uri.parse("content://" + AUTHORITY + "/" + LOGINS_PATH);
	public static final Uri CONTENT_URI_ORGANIZATION_MEMBERS = Uri.parse("content://" + AUTHORITY + "/" + MEMBERS_PATH);
	public static final Uri CONTENT_URI_ACTIVE_ORGANIZATION = Uri.parse("content://" + AUTHORITY + "/" + ACTIVE_ORGANIZATION_PATH);

	

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/apps";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/app";
	
	/*
	 * Builds the matcher which defines the request URI which will be answered
	 */
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
	    sURIMatcher.addURI(AUTHORITY, BASE_PATH, PACKAGES);
	}
	
	/*
	 * Called by Android when the content provider is first created
	 * Here we should get the resources we need.
	 * This should be fast
	 */
	@Override
	public boolean onCreate() {
		db=new DatabaseHandler(getContext());
		return ((db == null) ? false : true);
	}
	
	/*
	 * Used to determine the type of response which will be returned for a given request
	 */
	@Override
	public String getType(Uri uri) {
		return null;
	}
	
	/*
	 * Used to get a rock from the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
		Cursor c = null;
		return c;
	}
	
	/*
	 * Used to add a rock to the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
	    return Uri.parse(BASE_PATH);
	}
	
	/*
	 * Used to update a rock in the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
	    int rowsUpdated = 0;
	    return rowsUpdated;
	}
	
	/*
	 * Used to delete a rock from the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {		
	    int rowsDeleted = 0;
	    return rowsDeleted;
	}
	
}