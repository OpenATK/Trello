package com.openatk.trello.provider;

import com.google.gson.Gson;
import com.openatk.trello.database.AppsTable;
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
	private static final String DEFAULT_SORT_ORDER = AppsTable.COL_NAME;
	
	/*
	 * Builds the matcher which defines the request URI which will be answered
	 */
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
	    sURIMatcher.addURI(AUTHORITY, BASE_PATH, PACKAGES);
	    sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", PACKAGE_ID);
	    sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/*", PACKAGE);
	    
	    sURIMatcher.addURI(AUTHORITY, LOGINS_PATH, LOGINS);
	    sURIMatcher.addURI(AUTHORITY, MEMBERS_PATH, ORGANIZATION_MEMBERS);
	    sURIMatcher.addURI(AUTHORITY, ACTIVE_ORGANIZATION_PATH, ACTIVE_ORGANIZATION);
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
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		
		queryBuilder.setTables(AppsTable.TABLE_NAME);
		
		String orderBy;
		if(TextUtils.isEmpty(sort)) {
			orderBy = DEFAULT_SORT_ORDER;
		} else {
			orderBy=sort;
		}
		
		int uriType = sURIMatcher.match(uri);
	    switch (uriType) {
		    case PACKAGES:
		      break;
		    case PACKAGE_ID:
		      // Adding the ID to the original query
		      queryBuilder.appendWhere(AppsTable.COL_ID + "=" + uri.getLastPathSegment());
		      break;
		    case PACKAGE:
			      // Adding the NAME to the original query
			      queryBuilder.appendWhere(AppsTable.COL_PACKAGE_NAME + "= '" + uri.getLastPathSegment() + "'");
		      break;
		    case LOGINS:
				queryBuilder.setTables(LoginsTable.TABLE_NAME);
		    	queryBuilder.appendWhere(LoginsTable.COL_ACTIVE + "= 1");
		    	orderBy = null;
		    	break;
		    case ORGANIZATION_MEMBERS:
		    	queryBuilder.setTables(OrganizationMembersTable.TABLE_NAME);
		    	orderBy = null;
		    	break;
		    case ACTIVE_ORGANIZATION:
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
				String[] columns = {"orgoId"};
			    MatrixCursor cursor = new MatrixCursor(columns);
			    //TODO change default
			    cursor.addRow(new Object[] { prefs.getString("organizationId", "52c5ca99511d04d44600214f") });
				return cursor;
		    default:
		    	throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    SQLiteDatabase database = db.getReadableDatabase();
		Cursor c = queryBuilder.query(database, projection, selection, selectionArgs, null, null, orderBy);
		// Make sure that potential listeners are getting notified
	    c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	/*
	 * Used to add a rock to the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		 int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = db.getWritableDatabase();
	    long id = 0;
	    switch (uriType) {
		    case PACKAGES:
		        id = sqlDB.insert(AppsTable.TABLE_NAME, null, values);
		        break;
		    default:
		    	throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    
	    getContext().getContentResolver().notifyChange(uri, null);
	    return Uri.parse(BASE_PATH + "/" + id);
	}
	
	/*
	 * Used to update a rock in the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = db.getWritableDatabase();
	    int rowsUpdated = 0;
	    switch (uriType) {
		    case PACKAGES:
		      rowsUpdated = sqlDB.update(AppsTable.TABLE_NAME, values, selection, selectionArgs);
		      break;
		    case PACKAGE_ID:
		      String id = uri.getLastPathSegment();
		      if (TextUtils.isEmpty(selection)) {
		        rowsUpdated = sqlDB.update(AppsTable.TABLE_NAME, values, AppsTable.COL_ID + "=" + id, null);
		      } else {
		        rowsUpdated = sqlDB.update(AppsTable.TABLE_NAME, values, AppsTable.COL_ID + "=" + id  + " and "  + selection, selectionArgs);
		      }
		      break;
		    case PACKAGE:
	    	  String packageName = uri.getLastPathSegment();
		      if (TextUtils.isEmpty(selection)) {
		        rowsUpdated = sqlDB.update(AppsTable.TABLE_NAME, values, AppsTable.COL_PACKAGE_NAME + "= '" + packageName + "'", null);
		      } else {
		        rowsUpdated = sqlDB.update(AppsTable.TABLE_NAME, values, AppsTable.COL_PACKAGE_NAME + "= '" + packageName  + "' and "  + selection, selectionArgs);
		      }
		      break;
		    default:
		      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
		// Tell the world of the update
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsUpdated;
	}
	
	/*
	 * Used to delete a rock from the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {		
		int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = db.getWritableDatabase();
	    int rowsDeleted = 0;
	    switch (uriType) {
		    case PACKAGES:
		      rowsDeleted = sqlDB.delete(AppsTable.TABLE_NAME, selection, selectionArgs);
		      break;
		    case PACKAGE_ID:
		      String id = uri.getLastPathSegment();
		      if (TextUtils.isEmpty(selection)) {
		        rowsDeleted = sqlDB.delete(AppsTable.TABLE_NAME, AppsTable.COL_ID + "=" + id, null);
		      } else {
		        rowsDeleted = sqlDB.delete(AppsTable.TABLE_NAME, AppsTable.COL_ID + "=" + id + " and " + selection, selectionArgs);
		      }
		      break;
		    case PACKAGE:
			      String packageName = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection)) {
			        rowsDeleted = sqlDB.delete(AppsTable.TABLE_NAME, AppsTable.COL_PACKAGE_NAME + "=" + packageName, null);
			      } else {
			        rowsDeleted = sqlDB.delete(AppsTable.TABLE_NAME, AppsTable.COL_PACKAGE_NAME + "=" + packageName + " and " + selection, selectionArgs);
			      }
			      break;
		    default:
		      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
		// Tell the world of the delete
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsDeleted;
	}
	
}