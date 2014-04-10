package com.openatk.trello;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.openatk.trello.database.AppsTable;
import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.internet.App;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AppsList extends Activity implements OnClickListener, OnItemClickListener {	
	
	private ListView appsListView = null;
	private TextView appsOrganizationName = null;
	private List<App> appsList = null;
	
	private boolean loading = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.apps_list);
		
		this.setTitle(getString(R.string.AppsListTitle));
		appsListView = (ListView) findViewById(R.id.apps_list_view);
		appsOrganizationName = (TextView) findViewById(R.id.apps_farm);
		
		//Load organizations
		appsList = new ArrayList<App>();
		
		getAppList();
		
		AppsArrayAdapter adapter = new AppsArrayAdapter(this, R.layout.app, appsList);
		appsListView.setAdapter(adapter);
		appsListView.setOnItemClickListener(this);
	}
	
	@Override
	protected void onResume() {
		getAppList();
		((BaseAdapter) appsListView.getAdapter()).notifyDataSetChanged();
		super.onResume();
	}

	private void getAppList(){
		List<App> newAppsList = new ArrayList<App>();
		
		//Find all supported apps
		Intent sendIntent = new Intent();
		sendIntent.setAction("com.openatk.trello");
		
		PackageManager packageManager = getPackageManager();
		List<ResolveInfo> services = packageManager.queryIntentActivities(sendIntent, 0);
		Collections.sort(services, new ResolveInfo.DisplayNameComparator(packageManager));
		
		DatabaseHandler dbHandler = new DatabaseHandler(this);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		String orgoName = prefs.getString("organizationName", "Unknown");
		appsOrganizationName.setText(orgoName);
		
		String[] columns = { AppsTable.COL_ID, AppsTable.COL_NAME, AppsTable.COL_PACKAGE_NAME, AppsTable.COL_ALLOW_SYNCING, AppsTable.COL_LAST_SYNC, AppsTable.COL_AUTO_SYNC, AppsTable.COL_BOARD_NAME };
		
	    List<App> appsInDb = new ArrayList<App>();
	    
		Cursor cursor = database.query(AppsTable.TABLE_NAME, columns, null, null,null, null, null);
	    while (cursor.moveToNext()) {
	    	Log.d("In Db", "it:" + cursor.getString(cursor.getColumnIndex(AppsTable.COL_NAME)));
	    	Log.d("In Db", "it:" + cursor.getString(cursor.getColumnIndex(AppsTable.COL_PACKAGE_NAME)));
	    	
	    	Boolean isSynced = (cursor.getInt(cursor.getColumnIndex(AppsTable.COL_ALLOW_SYNCING)) == 1) ? true : false;
	    	//Assume uninstalled
	    	App newApp = new App(cursor.getLong(cursor.getColumnIndex(AppsTable.COL_ID)), isSynced, false, cursor.getString(cursor.getColumnIndex(AppsTable.COL_PACKAGE_NAME)), cursor.getString(cursor.getColumnIndex(AppsTable.COL_NAME)), null, null);
	    	newApp.setLastSync(cursor.getString(cursor.getColumnIndex(AppsTable.COL_LAST_SYNC)));
	    	newApp.setAutoSync(cursor.getInt(cursor.getColumnIndex(AppsTable.COL_AUTO_SYNC)));
	    	newApp.setBoardName(cursor.getString(cursor.getColumnIndex(AppsTable.COL_BOARD_NAME)));
	    	appsInDb.add(newApp);
	    }
	    cursor.close();
	    dbHandler.close();
		
		if(services != null){
			 final int count = services.size();
		    Log.d("AppsList - onCreate", "Here 2");

			 Log.d("AppsList - onCreate", Integer.toString(count));

            for (int i = 0; i < count; i++) {
	          	ResolveInfo info = services.get(i);
	          	String packageName = info.activityInfo.applicationInfo.packageName.toString();
	          	CharSequence csDesc = info.activityInfo.applicationInfo.loadDescription(packageManager);
	          	CharSequence csName = info.activityInfo.applicationInfo.loadLabel(packageManager);
	          	
	          	String name = (csName == null) ? null : csName.toString();
	          	String desc = (csDesc == null) ? null : csDesc.toString();
	          	Drawable icon = info.activityInfo.applicationInfo.loadIcon(packageManager);
	          	
	          	Log.d("AppList", "Name:" + name);
	          	Log.d("AppList", "Package:" + packageName);
	          	
	          	Long theId = null;
	          	Boolean isSynced = null;
	          	Integer isAutoSynced = 0;
	          	String lastSynced = null;
	          	String boardName = null;
	          	Iterator<App> iterator = appsInDb.iterator();
	        	while (iterator.hasNext()) {
	        		App compare = iterator.next();
	        		String comparePackageName = compare.getPackageName();
	        		if(comparePackageName != null && packageName.contentEquals(comparePackageName)){
	        			theId = compare.getId();
	        			isSynced = compare.getSyncApp();
	        			isAutoSynced = compare.getAutoSync();
	        			lastSynced = compare.getLastSync();
	        			boardName = compare.getBoardName();
	        			iterator.remove(); //Remove this found already
	        		}
	        	}
	          	App newApp = new App(theId, isSynced, true, packageName, name, desc, icon);
		    	newApp.setAutoSync(isAutoSynced);
		    	newApp.setInstalled(true);
		    	newApp.setLastSync(lastSynced);
		    	newApp.setBoardName(boardName);
		    	newAppsList.add(newApp);
            }
            //Add ones that arn't installed anymore (only one's left after removing)
            Iterator<App> iterator = appsInDb.iterator();
        	while (iterator.hasNext()) {
        		App compare = iterator.next();
        		compare.setSyncApp(false); //Can't sync anymore
        		compare.setInstalled(false); //Can't sync anymore
        		//newAppsList.add(compare);
        	}
		}
		appsList.clear();
		appsList.addAll(newAppsList);
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
		} else if(item.getItemId() == R.id.menu_apps_account){
			// Show new app menu
			Intent go = new Intent(this, LoginsList.class);
			startActivity(go);
		} else if(item.getItemId() == R.id.menu_help){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
	        alert.setTitle("Help");
	        WebView wv = new WebView(this);
	        wv.loadUrl("file:///android_asset/Help.html");
	        wv.setWebViewClient(new WebViewClient()
	        {
	            @Override
	            public boolean shouldOverrideUrlLoading(WebView view, String url)
	            {
	                view.loadUrl(url);
	                return true;
	            }
	        });
	        alert.setView(wv);
	        alert.setNegativeButton("Close", null);
	        alert.show();
		} else if(item.getItemId() == R.id.menu_legal){
			CharSequence licence= "The MIT License (MIT)\n" +
	                "\n" +
	                "Copyright (c) 2013 Purdue University\n" +
	                "\n" +
	                "Permission is hereby granted, free of charge, to any person obtaining a copy " +
	                "of this software and associated documentation files (the \"Software\"), to deal " +
	                "in the Software without restriction, including without limitation the rights " +
	                "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
	                "copies of the Software, and to permit persons to whom the Software is " +
	                "furnished to do so, subject to the following conditions:" +
	                "\n" +
	                "The above copyright notice and this permission notice shall be included in " +
	                "all copies or substantial portions of the Software.\n" +
	                "\n" +
	                "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR " +
	                "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, " +
	                "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE " +
	                "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER " +
	                "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, " +
	                "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN " +
	                "THE SOFTWARE.\n";
			new AlertDialog.Builder(this)
				.setTitle("Legal")
				.setMessage(licence)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton("Close", null).show();
		}
		return false;
	}

	public void doneLoadingList(){
		//Done loading apps update list
		if(loading){
			loading = false;
			//Remove loading screen
		}
		((BaseAdapter) appsListView.getAdapter()).notifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		Log.d("List item:", "test");
	}
}
