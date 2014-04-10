package com.openatk.trello;

import java.util.List;

import com.openatk.trello.R;
import com.openatk.trello.database.AppsTable;
import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.internet.App;
import com.openatk.trello.internet.Login;



import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class LoginsArrayAdapter extends ArrayAdapter<Login> {
	private final Context context;
	private List<Login> items = null;
	private int resId;
	
	private SQLiteDatabase database;
	private DatabaseHandler dbHandler;
	
	public LoginsArrayAdapter(Context context, int layoutResourceId, List<Login> data) {
		super(context, layoutResourceId, data);
		this.resId = layoutResourceId;
		this.context = context;
		this.items = data;
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
			holder.txtTitle = (TextView) row.findViewById(R.id.list_item_2_text);
			holder.imgIcon = (ImageView) row.findViewById(R.id.list_item_2_icon);
			
			row.setTag(holder);
		} else {
			holder = (AppHolder) row.getTag();
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
		holder.txtTitle.setText(items.get(position).getName());
		holder.imgIcon.setImageDrawable( this.context.getResources().getDrawable( R.drawable.empty_person ));
		return row;
	}
	
	static class AppHolder
    {
        ImageView imgIcon;
        TextView txtTitle;
    }
}
