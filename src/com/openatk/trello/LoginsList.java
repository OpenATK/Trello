package com.openatk.trello;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.openatk.trello.R;
import com.openatk.trello.database.AppsTable;
import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.database.LoginsTable;
import com.openatk.trello.internet.App;
import com.openatk.trello.internet.Login;
import com.openatk.trello.internet.TrelloOrganization;


import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class LoginsList extends Activity implements OnClickListener, OnItemClickListener {	
	
	private ListView itemListView = null;
	
	private List<Login> itemList = null;
	private LoginsArrayAdapter itemListAdapter = null;
	
	DatabaseHandler dbHandler = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_view);
		
		this.setTitle("Select trello account");
		itemListView = (ListView) findViewById(R.id.list_view);
				
		//Load items
		itemList = new ArrayList<Login>();
		
		
		dbHandler = new DatabaseHandler(this);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		String[] columns = { LoginsTable.COL_ID, LoginsTable.COL_NAME, LoginsTable.COL_USERNAME, LoginsTable.COL_APIKEY, LoginsTable.COL_TOKEN, LoginsTable.COL_SECRET, LoginsTable.COL_ACTIVE, LoginsTable.COL_ORGO_ID};
		Cursor cursor = database.query(LoginsTable.TABLE_NAME, columns, null, null,null, null, null);
	    while (cursor.moveToNext()) {	    	
	    	Boolean isActive = (cursor.getInt(cursor.getColumnIndex(LoginsTable.COL_ACTIVE)) == 1) ? true : false;
	    	Login newLogin = new Login(cursor.getLong(cursor.getColumnIndex(LoginsTable.COL_ID)), cursor.getString(cursor.getColumnIndex(LoginsTable.COL_NAME)), cursor.getString(cursor.getColumnIndex(LoginsTable.COL_USERNAME)), cursor.getString(cursor.getColumnIndex(LoginsTable.COL_SECRET)), cursor.getString(cursor.getColumnIndex(LoginsTable.COL_TOKEN)), cursor.getString(cursor.getColumnIndex(LoginsTable.COL_APIKEY)), isActive, cursor.getString(cursor.getColumnIndex(LoginsTable.COL_ORGO_ID)));
	    	itemList.add(newLogin);
	    }
	    cursor.close();
	    dbHandler.close();
	    
	    if(itemList.isEmpty()){
	    	//Go to browser
			Intent go = new Intent(this, Browser.class);
			go.putExtra("Setup", true);
			startActivity(go);
	    } else {
	    	Login newLogin = new Login(null, "Add Account", null, null, null, null, null, null);
	    	itemList.add(newLogin);
	    }
    	
		itemListAdapter = new LoginsArrayAdapter(this, R.layout.list_item_2, itemList);
		itemListView.setAdapter(itemListAdapter);
		itemListView.setOnItemClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.apps_list, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if(item.getItemId() == R.id.menu_apps_organization){
			// Show new app menu
			Intent go = new Intent(this, OrganizationsList.class);
			startActivity(go);
		}  else if(item.getItemId() == R.id.menu_apps_members){
			// Show new app menu
			Intent go = new Intent(this, MembersList.class);
			startActivity(go);
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		
		Login item = (Login) itemListAdapter.getItem(position);
		if(item.getId() == null) {
			//Go to browser
			Intent go = new Intent(this, Browser.class);
			go.putExtra("todo", "change_account");
			startActivity(go);
		} else {
			SQLiteDatabase database = dbHandler.getWritableDatabase();
			ContentValues updateValues2 = new ContentValues();
			updateValues2.put(LoginsTable.COL_ACTIVE, 1);
			String where2 = LoginsTable.COL_ID + " = '" + item.getId() + "'";
			database.update(LoginsTable.TABLE_NAME, updateValues2, where2, null);
			dbHandler.close();
	
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("organizationId", item.getOrganizationId().trim());
			editor.putString("apiKey", item.getApiKey().trim());
			editor.putString("token", item.getToken().trim());		
			editor.putBoolean("FirstSetup", true);
			editor.commit();
	
			Intent go = new Intent(this, AppsList.class);
			startActivity(go);
		}
	}
}
