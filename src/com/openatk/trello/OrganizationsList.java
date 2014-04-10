package com.openatk.trello;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.openatk.trello.authenticator.AccountGeneral;
import com.openatk.trello.database.AppsTable;
import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.database.LoginsTable;
import com.openatk.trello.internet.OrganizationsHandler;
import com.openatk.trello.internet.TrelloOrganization;
import com.openatk.trello.provider.SyncProvider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class OrganizationsList extends Activity implements OnClickListener,
		OnItemClickListener {

	private SQLiteDatabase database;
	private DatabaseHandler dbHandler;

	private ListView orgoList = null;

	private OrganizationsHandler organizationHandler = null;
	private List<TrelloOrganization> organizationList = null;
	OrganizationArrayAdapter orgoAdapter = null;

	private boolean loading = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading);

		dbHandler = new DatabaseHandler(this);

		// Load organizations
		organizationList = new ArrayList<TrelloOrganization>();

		orgoAdapter = new OrganizationArrayAdapter(this, R.layout.organization, organizationList);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String accountName = prefs.getString("accountName", null);
		
       
		organizationHandler = new OrganizationsHandler(this, organizationList, accountName);
		loading = true;
		organizationHandler.getOrganizationsList();
	}

	@Override
	public void onClick(View v) {

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.organizations_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == R.id.menu_addOrganization) {
			// Show new organization dialog
			Log.d("Orgo List", "Add Organization");
			Intent go = new Intent(this, AddOrganization.class);
			startActivity(go);
		}
		return false;
	}

	public void doneLoadingList() {
		// Done loading organizations update list
		if(loading) {
			if(organizationList.size() < 2) {
				//No organizations exist go straight to add
				Intent go = new Intent(this, AddOrganization.class);
				go.putExtra("todo", "mustAdd");
				startActivity(go);
				finish();
			} else {
				loading = false;
				// Remove loading screen
				setContentView(R.layout.organizations_list);
				orgoList = (ListView) findViewById(R.id.list_view);
				orgoList.setAdapter(orgoAdapter);
				orgoList.setOnItemClickListener(this);
				((BaseAdapter) orgoList.getAdapter()).notifyDataSetChanged();
			}
		} else {
			((BaseAdapter) orgoList.getAdapter()).notifyDataSetChanged();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {
		TrelloOrganization item = (TrelloOrganization) orgoAdapter
				.getItem(position);

		if(item.getId() == null){
			//Add new organization
			Intent go = new Intent(this, AddOrganization.class);
			startActivity(go);
		} else {
			database = dbHandler.getWritableDatabase();
			//Get old active orgo
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String oldOrgo = prefs.getString("organizationId", null); //"52c5ca99511d04d44600214f"

			//Set new active orgo
			String newOrgo = item.getId().trim();
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("organizationId", newOrgo);
			editor.putString("organizationName", item.getDisplayName().trim());
			editor.putBoolean("FirstSetup", true);
			editor.commit();
			
			//Notify all apps of change
			if(oldOrgo != null && oldOrgo.contentEquals(newOrgo) == false){
				String[] columns = { AppsTable.COL_PACKAGE_NAME, AppsTable.COL_ID };
				Cursor cursor = database.query(AppsTable.TABLE_NAME, columns, null, null, null, null, null);
				
				ContentValues values = new ContentValues();
		    	values.put("oldOrg", oldOrgo);
		    	values.put("newOrg", newOrgo);
		    	
			    while(cursor.moveToNext()){
			    	String appPackage = cursor.getString(cursor.getColumnIndex(AppsTable.COL_PACKAGE_NAME));
					Uri uri = Uri.parse("content://" + appPackage + ".trello.provider/organization");
			    	this.getApplicationContext().getContentResolver().update(uri, values, null, null);  
			    }
				cursor.close();
				dbHandler.close();
			}
			
			Intent go = new Intent(this, MembersList.class);
			startActivity(go);
		}		
	}
}
