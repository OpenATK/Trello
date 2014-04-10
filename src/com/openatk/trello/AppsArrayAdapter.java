package com.openatk.trello;

import java.util.List;

import com.openatk.trello.authenticator.AccountGeneral;
import com.openatk.trello.database.AppsTable;
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
	private List<App> apps = null;
	private int resId;
	
	private SQLiteDatabase database;
	private DatabaseHandler dbHandler;
	
	public AppsArrayAdapter(Context context, int layoutResourceId, List<App> data) {
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
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(resId, parent, false);
			

			holder = new AppHolder();
			holder.txtTitle = (TextView) row.findViewById(R.id.app_name);
			holder.imgIcon = (ImageView) row.findViewById(R.id.app_icon);
			holder.chkSyncing = (CheckBox) row.findViewById(R.id.app_checkbox);
			holder.chkSyncing.setChecked(apps.get(position).getSyncApp());
			holder.autoSync = (ToggleButton)  row.findViewById(R.id.app_auto_togglebutton);
			Boolean auto = (apps.get(position).getAutoSync() == 1) ? true : false;
			holder.autoSync.setChecked(auto);
			holder.sync = (ImageButton)  row.findViewById(R.id.app_sync);
			if(apps.get(position).getSyncApp()) holder.sync.setVisibility(View.VISIBLE);
			
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
			
			
			//TODO different color for uninstalled
			if(apps.get(position).getInstalled() == false){
				holder.chkSyncing.setEnabled(false);
			}
			
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
		} else {
			if(holder.txtTitle == null){
				Log.d("AppsArrayAdapter", "txtTitle null");
			} else {
				holder.txtTitle.setText("test");
			}
		}
		
		
		
		holder.txtTitle.setText(apps.get(position).getName());
		holder.imgIcon.setImageDrawable(apps.get(position).getIcon());
		if(apps.get(position).getLastSync() == null){
			holder.txtAppLastSync.setText(context.getString(R.string.app_never_sync));
		} else {
			holder.txtAppLastSync.setText(apps.get(position).getLastSync());
		}
		
				//ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		// Change the icon for Windows and iPhone
//		String s = values[position];
//		if (s.startsWith("iPhone")) {
//			imageView.setImageResource(R.drawable.no);
//		} else {
//			imageView.setImageResource(R.drawable.ok);
//		}
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
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {			
			final SyncHolder parentSyncHolder = (SyncHolder) buttonView.getTag();
			final App parentApp = parentSyncHolder.app;
			
			if(isChecked){
				parentSyncHolder.syncButton.setVisibility(View.VISIBLE);
			} else {
				parentSyncHolder.syncButton.setVisibility(View.INVISIBLE);
			}
			
			
			//Save this selection in database
			database = dbHandler.getWritableDatabase();
			
			String[] columns = { AppsTable.COL_ID, AppsTable.COL_PACKAGE_NAME, AppsTable.COL_ALLOW_SYNCING };
			
			Cursor cursor = database.query(AppsTable.TABLE_NAME,
					columns, AppsTable.COL_PACKAGE_NAME + " = '" + parentApp.getPackageName() + "'", null,
		        null, null, null);
			
			
	    	final Integer newValue = isChecked ? 1 : 0;
	    	ContentValues values = new ContentValues();
	    	values.put(AppsTable.COL_ALLOW_SYNCING, newValue);
	    	
			Long id = null;
			Integer selected = null;
		    if(cursor.moveToFirst()){
		    	id = cursor.getLong(cursor.getColumnIndex(AppsTable.COL_ID));
			    selected = cursor.getInt(cursor.getColumnIndex(AppsTable.COL_ALLOW_SYNCING));
			    if((selected == 0 && isChecked == true) || (selected == 1 && isChecked == false)){
			    	//update
			    	database.update(AppsTable.TABLE_NAME, values,(AppsTable.COL_ID + "=" + id.toString()),null);
			    }
		    } else {
            	ContentValues newValues = new ContentValues();
            	newValues.put(AppsTable.COL_ALLOW_SYNCING, newValue);
            	newValues.put(AppsTable.COL_NAME, parentApp.getName());
            	newValues.put(AppsTable.COL_PACKAGE_NAME, parentApp.getPackageName());
 		    	database.insert(AppsTable.TABLE_NAME, null, newValues);
 		    	Log.d("AppsArrayAdapter - chkSyncingListener", "Inserting" + parentApp.getPackageName());
		    }
		    parentApp.setSyncApp(isChecked);
			Log.d("Selected", parentApp.getName());
			cursor.close();
			dbHandler.close();
			
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
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			final App parentApp = (App) buttonView.getTag();

			//Save this selection in database
			database = dbHandler.getWritableDatabase();
			
			String[] columns = { AppsTable.COL_ID, AppsTable.COL_PACKAGE_NAME, AppsTable.COL_AUTO_SYNC };
			
			Cursor cursor = database.query(AppsTable.TABLE_NAME,
					columns, AppsTable.COL_PACKAGE_NAME + " = '" + parentApp.getPackageName() + "'", null,
		        null, null, null);
			
			
	    	final Integer newValue = isChecked ? 1 : 0;
	    	ContentValues values = new ContentValues();
	    	values.put(AppsTable.COL_AUTO_SYNC, newValue);
	    	
			Long id = null;
			Integer selected = null;
		    if(cursor.moveToFirst()){
		    	id = cursor.getLong(cursor.getColumnIndex(AppsTable.COL_ID));
			    selected = cursor.getInt(cursor.getColumnIndex(AppsTable.COL_AUTO_SYNC));
			    if((selected == 0 && isChecked == true) || (selected == 1 && isChecked == false)){
			    	//update
			    	Log.d("AppsArrayAdapter - onCheckChanged", "Updating autosync in db");
			    	database.update(AppsTable.TABLE_NAME, values,(AppsTable.COL_ID + "=" + id.toString()),null);
			    }
		    } else {
            	ContentValues newValues = new ContentValues();
            	newValues.put(AppsTable.COL_AUTO_SYNC, newValue);
            	newValues.put(AppsTable.COL_NAME, parentApp.getName());
            	newValues.put(AppsTable.COL_PACKAGE_NAME, parentApp.getPackageName());
 		    	database.insert(AppsTable.TABLE_NAME, null, newValues);
 		    	Log.d("AppsArrayAdapter - toggleAutoSyncListener", "Inserting" + parentApp.getPackageName());
		    }
		    parentApp.setAutoSync(newValue);
			cursor.close();
			dbHandler.close();
		}
	};
	
	private OnClickListener butSyncListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			final App parentApp = (App) v.getTag();
			//Send a sync intent to this app
			PackageManager pm = context.getPackageManager();
			/*Intent appStartIntent = pm.getLaunchIntentForPackage(parentApp.getPackageName());
			appStartIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			if(appStartIntent != null){
				appStartIntent.putExtra("todo", "sync");
			    context.startActivity(appStartIntent);
			}*/
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String accountName = prefs.getString("accountName", null);
			Account theAccount = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);
			
			Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true); // Performing a sync no matter if it's off
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); // Performing a sync no matter if it's off
            bundle.putString("appPackage", parentApp.getPackageName());
            //bundle.putLong("appId", parentApp.getId());
            ContentResolver.requestSync(theAccount, "com.openatk.trello.provider", bundle);
		}
	};
}
