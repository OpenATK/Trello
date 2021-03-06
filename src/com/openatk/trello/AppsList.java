package com.openatk.trello;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import com.google.gson.Gson;
import com.openatk.libtrello.TrelloSyncInfo;
import com.openatk.trello.authenticator.AccountGeneral;
import com.openatk.trello.internet.App;

public class AppsList extends Activity implements OnClickListener, OnItemClickListener {	
	
	private ListView appsListView = null;
	private TextView appsOrganizationName = null;
	private ArrayList<App> appsList = null;
	AppsArrayAdapter appsListAdapter = null;
	
	private boolean loading = false;
	private static SimpleDateFormat dateFormaterLocal = new SimpleDateFormat("LLL d, yyyy h:mm a", Locale.US);
	private static SimpleDateFormat dateFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final String AUTHORITY = "com.openatk.trello";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.apps_list);
		
		dateFormaterLocal.setTimeZone(TimeZone.getDefault());
		dateFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));

		this.setTitle(getString(R.string.AppsListTitle));
		appsListView = (ListView) findViewById(R.id.apps_list_view);
		appsOrganizationName = (TextView) findViewById(R.id.apps_farm);
		
		//Load organizations
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
		String orgoName = prefs.getString("organizationName", "Unknown");
		appsOrganizationName.setText(orgoName);
		
		appsList = getAppList(this);
		appsListAdapter = new AppsArrayAdapter(this, R.layout.app, appsList);
		
		appsListView.setAdapter(appsListAdapter);
		appsListView.setOnItemClickListener(this);
	}
	
	@Override
	protected void onResume() {
		//Load organizations
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
		String orgoName = prefs.getString("organizationName", "Unknown");
		appsOrganizationName.setText(orgoName);
		
		List<App> newApps = getAppList(this);
		
		appsList.clear();
		appsList.addAll(newApps);
		
		appsListAdapter.notifyDataSetChanged();
		refreshAppList(10);
		super.onResume();
	}
	
	
	
	@Override
	protected void onPause() {
		if(delayRefreshAppList != null) handler.removeCallbacks(delayRefreshAppList);
		delayRefreshAppList = null;
		super.onPause();
	}

	Runnable delayRefreshAppList = null;
	private Handler handler = new Handler();
	private void refreshAppList(final int interval){
		//Auto sync for presentations, allows intervals under 60 sec androids syncprovider minimum.
		if(delayRefreshAppList != null) handler.removeCallbacks(delayRefreshAppList);
		final Runnable r = new Runnable() {
		    public void run() {
		    	List<App> newApps = getAppList(getApplicationContext());
				appsList.clear();
				appsList.addAll(newApps);
				appsListAdapter.notifyDataSetChanged();
		    	
		        if(delayRefreshAppList != null) handler.postDelayed(delayRefreshAppList, interval*1000);
		    }
		};
		delayRefreshAppList = r;
        handler.postDelayed(delayRefreshAppList, interval*1000);
	}
	
	
	public static String dateToStringLocal(Date date) {
		if(date == null){
			return null;
		}
		return AppsList.dateFormaterLocal.format(date);
	} 
	public static Date stringToDateUTC(String date) {
		if(date == null){
			return null;
		}
		Date d;
		try {
			d = AppsList.dateFormaterUTC.parse(date);
		} catch (ParseException e) {
			d = new Date(0);
		}
		return d;
	}
	public static String dateToStringUTC(Date date) {
		if(date == null){
			return null;
		}
		return AppsList.dateFormaterUTC.format(date);
	}

	public static ArrayList<App> getAppList(Context context){
		ArrayList<App> newAppsList = new ArrayList<App>();
		
		//Find all supported apps
		Intent sendIntent = new Intent();
		sendIntent.setAction("com.openatk.trello");
		
		PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> services = packageManager.queryIntentActivities(sendIntent, 0);
		Collections.sort(services, new ResolveInfo.DisplayNameComparator(packageManager));
		
	
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
	          	
	          	Boolean isSynced = false;
	          	Integer isAutoSynced = 0;
	          	String lastSynced = null;
	          	String boardName = null;
	          	
	          	App newApp = new App(isSynced, packageName, name, desc, icon);
	          	newApp.setAutoSync(isAutoSynced);
		    	newApp.setLastSync(lastSynced);
		    	newApp.setBoardName(boardName);
		    	
	          	Uri uri = Uri.parse("content://" + newApp.getPackageName() + ".trello.provider/get_sync_info");
		    	Cursor cursor2 = null;
		    	boolean failed = false;
		    	try {
		    		cursor2 = context.getContentResolver().query(uri, null, null, null, null);
		    	} catch(Exception e) {
		    		failed = true;
		    	}
		    	if(failed == false){
			    	Gson gson = new Gson();
					TrelloSyncInfo syncInfo = null;
			    	if(cursor2 != null){
			    		while(cursor2.moveToNext()){
			    			//Only 1 item for now
			    			if(cursor2.getColumnCount() > 0 && cursor2.getColumnIndex("json") != -1){
				    			String json = cursor2.getString(cursor2.getColumnIndex("json"));
				    			try {
				    				syncInfo = gson.fromJson(json, TrelloSyncInfo.class);
				    			} catch (Exception e){
				    				Log.d("Failed to convert json to info:", json);
				    			}
			    			}
			    		}
			    		cursor2.close();
			    	}
			    	if(syncInfo != null){
					    Log.d("AppsList - onCreate",  "Has sync info");

				    	if(syncInfo.getLastSync() == null){
				    		newApp.setLastSync(null);
				    	} else {
				    		newApp.setLastSync(AppsList.dateToStringLocal(syncInfo.getLastSync()));
				    	}
				    	
				    	if(syncInfo.getAutoSync() == null) syncInfo.setAutoSync(false);
			    		newApp.setAutoSync(((syncInfo.getAutoSync() == true) ? 1 : 0));
			    		
			    		if(syncInfo.getSync() == null) syncInfo.setSync(false);
			    		newApp.setSyncApp(syncInfo.getSync());
			    		
					    Log.d("AppsList - onCreate",  "Has sync info" + syncInfo.getSync());
			    	}
		    	} else {
		    		//TODO add a "Incompatible Warning - Please Update"
				    Log.d("AppsList - onCreate", newApp.getPackageName() + " Incompatible Warning - Please Update");
		    		newApp.setSyncApp(false);
		    		newApp.setAutoSync(0);
		    		newApp.setLastSync(null);
		    		newApp.setName(newApp.getName() + " - Incompatible version, please upgrade.");
		    	}
		    	newAppsList.add(newApp);
            }
		}
		return newAppsList;
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
			//Intent go = new Intent(this, LoginsList.class);
			//startActivity(go);
			removeAccountAndRemake();
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
	
	private void removeAccountAndRemake(){
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("FirstSetup", false);
		editor.commit();
		
		final Activity parent = this;
		AccountManager mAccountManager = AccountManager.get(this);
        mAccountManager.getAccountsByTypeAndFeatures(AccountGeneral.ACCOUNT_TYPE, null,
                new AccountManagerCallback<Account[]>() {
					@Override
					public void run(AccountManagerFuture<Account[]> future) {
						Account[] accounts = null;
                        try {
                        	accounts = future.getResult();
                        	for(int i=0; i<accounts.length; i++){
                        		Account acc = accounts[i];
                        		Log.d("AppsList - getAccountList", "Account Name:" + acc.name);
                        	}
                        	if(accounts.length > 1) Log.w("AppsList getAccountList", "More than 1 account.");
                        	if(accounts.length > 0) { 
                        		removeAccount(accounts[0]);
                        	} else {
                        		Intent go = new Intent(parent, MainActivity.class);
                    			parent.startActivity(go);
                        	}
                        } catch (Exception e) {
                        	Log.d("getAccountList", "error");
                            e.printStackTrace();
                        }
					}
                }
        , null);
	}
	
	private void removeAccount(Account account){
		final Activity parent = this;
		AccountManager mAccountManager = AccountManager.get(this);
		mAccountManager.removeAccount(account,
                new AccountManagerCallback<Boolean>() {
					@Override
					public void run(AccountManagerFuture<Boolean> future) {
						//Now we need to make a new account
						Intent go = new Intent(parent, MainActivity.class);
            			parent.startActivity(go);
					}
                }
        , null);
	}
}
