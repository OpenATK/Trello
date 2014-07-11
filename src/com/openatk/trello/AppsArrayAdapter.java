package com.openatk.trello;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.openatk.libtrello.TrelloContentProvider;
import com.openatk.libtrello.TrelloSyncInfo;
import com.openatk.trello.authenticator.AccountGeneral;
import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.internet.App;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class AppsArrayAdapter extends ArrayAdapter<App> {
	private final Context context;
	private ArrayList<App> apps = null;
	private int resId;
	
	private SQLiteDatabase database;
	private DatabaseHandler dbHandler;
    private static final String AUTHORITY = "com.openatk.trello";

	public AppsArrayAdapter(Context context, int layoutResourceId, ArrayList<App> data) {
		super(context, layoutResourceId, data);
		this.resId = layoutResourceId;
		this.context = context;
		this.apps = data;
		dbHandler = new DatabaseHandler(this.context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		AppHolder holder = null;
		
		if(row == null){
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(resId, parent, false);

			holder = new AppHolder();
			holder.txtTitle = (TextView) row.findViewById(R.id.app_name);
			holder.imgIcon = (ImageView) row.findViewById(R.id.app_icon);
			holder.chkSyncing = (CheckBox) row.findViewById(R.id.app_checkbox);
			holder.autoSync = (ToggleButton)  row.findViewById(R.id.app_auto_togglebutton);
			holder.sync = (ImageButton)  row.findViewById(R.id.app_sync);
			
			holder.txtViewTrello = (TextView)  row.findViewById(R.id.app_view_in_trello);
			holder.txtAppLastSync = (TextView)  row.findViewById(R.id.app_last_sync);
			
			final RelativeLayout left = (RelativeLayout) row.findViewById(R.id.app_left_column);
			final RelativeLayout right = (RelativeLayout) row.findViewById(R.id.app_right_column);
			
			right.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
	           @Override
				public void onGlobalLayout() {
	        	    //Adjust left column height
		       		ViewGroup.LayoutParams leftP = left.getLayoutParams();
		       		leftP.height = right.getHeight();
	            }
	        });
			left.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
	           @Override
				public void onGlobalLayout() {
	        	    //Adjust left column height
		       		ViewGroup.LayoutParams leftP = left.getLayoutParams();
		       		leftP.height = right.getHeight();
	            }
	        });
			
			
			SyncHolder syncHolder = new SyncHolder();
			syncHolder.app = apps.get(position);
			syncHolder.syncButton = holder.sync;
			
			holder.chkSyncing.setTag(syncHolder);
			holder.chkSyncing.setOnCheckedChangeListener(chkSyncingListener);
			
			holder.sync.setTag(apps.get(position));
			holder.sync.setOnClickListener(butSyncListener);
			
			holder.autoSync.setTag(apps.get(position));
			holder.autoSync.setOnCheckedChangeListener(toggleAutoSyncListener);
			
			row.setTag(holder);
		} else {
			holder = (AppHolder) row.getTag();
		}
		
		if(apps == null){
			Log.d("AppsArrayAdapter", "apps null");
		} else {
			Log.d("AppsArrayAdapter", "Length:" + Integer.toString(apps.size()));
			Log.d("AppsArrayAdapter", "Pos:" + Integer.toString(position));
			Log.d("AppsArrayAdapter", "PackName:" + apps.get(position).getPackageName());
			Log.d("AppsArrayAdapter", "Name:" + apps.get(position).getName());
		}
		
		if(holder == null){
			Log.d("AppsArrayAdapter", "holder null");
		}	
		
		
		
		holder.txtTitle.setText(apps.get(position).getName());
		holder.imgIcon.setImageDrawable(apps.get(position).getIcon());
		if(apps.get(position).getLastSync() == null){
			holder.txtAppLastSync.setText(context.getString(R.string.app_never_sync));
		} else {
			holder.txtAppLastSync.setText(apps.get(position).getLastSync());
		}
		
		if(apps.get(position).getName().contains("Incompatible")){
			holder.autoSync.setChecked(false);
			holder.autoSync.setEnabled(false);
			holder.sync.setVisibility(View.INVISIBLE);
			holder.chkSyncing.setChecked(false);
			holder.chkSyncing.setEnabled(false);
		} else {
			Boolean auto = (apps.get(position).getAutoSync() == 1) ? true : false;
			holder.autoSync.setChecked(auto);
			if(apps.get(position).getSyncApp()) holder.sync.setVisibility(View.VISIBLE);
			holder.chkSyncing.setChecked(apps.get(position).getSyncApp());
			holder.chkSyncing.setEnabled(true);
			holder.autoSync.setEnabled(true);
		}
		
		
		
		return row;
	}
	
	static class AppHolder
    {
        ImageView imgIcon;
        TextView txtTitle;
        CheckBox chkSyncing;
        TextView txtViewTrello;
        ImageButton sync;
        ToggleButton autoSync;
        TextView txtAppLastSync;
    }
	
	static class SyncHolder
    {
        App app;
        ImageButton syncButton;
    }
	
	private OnCheckedChangeListener chkSyncingListener = new OnCheckedChangeListener(){
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {			
			final SyncHolder parentSyncHolder = (SyncHolder) buttonView.getTag();
			final App parentApp = parentSyncHolder.app;
			
			if(isChecked){
				parentSyncHolder.syncButton.setVisibility(View.VISIBLE);
			} else {
				parentSyncHolder.syncButton.setVisibility(View.INVISIBLE);
			}
			
			//Pass it to the app
			ContentValues toPass = new ContentValues();
			Gson gson = new Gson();
			TrelloSyncInfo newInfo = new TrelloSyncInfo();
			newInfo.setSync(isChecked);
			String json = gson.toJson(newInfo);
			toPass.put("json", json);
			Uri uri = Uri.parse("content://" + parentApp.getPackageName() + ".trello.provider/set_sync_info");
	    	getContext().getContentResolver().update(uri, toPass, null, null);  
			
				
		    parentApp.setSyncApp(isChecked);
			Log.d("Selected", parentApp.getName());
			
			if(isChecked){
				//Send intent to package to enable trello syncing
				Log.d("Syncing enabled on:", parentApp.getPackageName());
			} else {
				//Send intent to package to disable trello syncing
				Log.d("Syncing disabled on:", parentApp.getPackageName());
			}
			
		}
	};
	
	private OnCheckedChangeListener toggleAutoSyncListener = new OnCheckedChangeListener(){
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			final App parentApp = (App) buttonView.getTag();

			//Pass it to the app
			ContentValues toPass = new ContentValues();
			Gson gson = new Gson();
			TrelloSyncInfo newInfo = new TrelloSyncInfo();
			newInfo.setAutoSync(isChecked);
			String json = gson.toJson(newInfo);
			toPass.put("json", json);
			Uri uri = Uri.parse("content://" + parentApp.getPackageName() + ".trello.provider/set_sync_info");
	    	getContext().getContentResolver().update(uri, toPass, null, null);  
			
	
	    	Account account = null;
			SharedPreferences prefs = getContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
	    	if(account ==  null){
				String accountName = prefs.getString("accountName", null);
				account = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);
	    	}
	    	
	    	//Do sync to enable setting
	    	if(isChecked){
		    	Bundle bundle = new Bundle();
		        bundle.putBoolean("isAutoSyncRequest", true);
		        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true); // Performing a sync no matter if it's off
		        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); // Performing a sync no matter if it's off
				TrelloContentProvider.Sync(parentApp.getPackageName(), bundle);	
	    	}
	    	
	    	Integer newValue = isChecked ? 1 : 0;
		    parentApp.setAutoSync(newValue);
		}
	};
	
	private OnClickListener butSyncListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			final App parentApp = (App) v.getTag();
			
			SharedPreferences prefs = getContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
			String accountName = prefs.getString("accountName", null);
			Account theAccount = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);
			
			Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true); // Performing a sync no matter if it's off
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); // Performing a sync no matter if it's off
            bundle.putString("appPackage", parentApp.getPackageName());
            ContentResolver.requestSync(theAccount, "com.openatk.trello.provider", bundle);
		}
	};
}
