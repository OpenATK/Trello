package com.openatk.trello;

import java.util.ArrayList;
import java.util.List;

import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.database.OrganizationMembersTable;
import com.openatk.trello.internet.MembersHandler;
import com.openatk.trello.internet.TrelloMember;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class MembersList extends Activity implements OnClickListener, OnItemClickListener {

	private ListView membersListView = null;
	private Button buttonNext = null;

	private MembersHandler membersHandler = null;
	private List<TrelloMember> membersList = null;
	MembersArrayAdapter memberAdapter = null;

	private boolean loading = false;
	DatabaseHandler dbHandler = null;
    private static final String AUTHORITY = "com.openatk.trello";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading);
		
		dbHandler = new DatabaseHandler(this);

		membersList = new ArrayList<TrelloMember>();
		
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);

		String orgoId = prefs.getString("organizationId", "null");
		String accountName = prefs.getString("accountName", null);
		
		membersHandler = new MembersHandler(this, membersList, orgoId, accountName);
		
		memberAdapter = new MembersArrayAdapter(this, R.layout.member_list_item, membersList, membersHandler);
		loading = true;
		membersHandler.getExistingMembersList();
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.member_list_next){
			//Go to app list
			Intent go = new Intent(this, AppsList.class);
			startActivity(go);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		//TODO refresh button
		//getMenuInflater().inflate(R.menu.members_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}

	public void doneLoadingList() {
		// Done loading organizations update list
		if (loading) {
			loading = false;
			// Remove loading screen
			setContentView(R.layout.member_list_view);
			membersListView = (ListView) findViewById(R.id.member_list_view);
			buttonNext = (Button) findViewById(R.id.member_list_next);
			membersListView.setAdapter(memberAdapter);
			membersListView.setOnItemClickListener(this);

			buttonNext.setOnClickListener(this);
		}
		membersListView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

		((MembersArrayAdapter) membersListView.getAdapter()).dataChanged();
		
		//Add Members to database
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);

		String orgoId = prefs.getString("organizationId", "null");
		if(orgoId.contentEquals("null") == false){
			SQLiteDatabase database = dbHandler.getWritableDatabase();
			//Look for members
			String[] columns = { OrganizationMembersTable.COL_MEMBER_ID, OrganizationMembersTable.COL_ORGO_ID };
			String where =  OrganizationMembersTable.COL_ORGO_ID + " = '" + orgoId + "'";
			Cursor cursor = database.query(OrganizationMembersTable.TABLE_NAME, columns, where, null,null, null, null);
		    List<String> inDb = new ArrayList<String>();
			while(cursor.moveToNext()) {
				String currentId = cursor.getString(cursor.getColumnIndex(OrganizationMembersTable.COL_MEMBER_ID));
				inDb.add(currentId);
		    }
		    cursor.close();
		    
		    for(int i=0; i<membersList.size(); i++){
		    	if(membersList.get(i).getId() != null){
					if(inDb.contains(membersList.get(i).getId()) == false){
						ContentValues values = new ContentValues();
						values.put(OrganizationMembersTable.COL_MEMBER_ID, membersList.get(i).getId());
						values.put(OrganizationMembersTable.COL_USERNAME, membersList.get(i).getUsername());
						values.put(OrganizationMembersTable.COL_NAME, membersList.get(i).getFullname());
						values.put(OrganizationMembersTable.COL_ORGO_ID, orgoId);
						database.insert(OrganizationMembersTable.TABLE_NAME, null,values);
				    	Log.d("AddMembersList - doneLoadingList", "Adding member to OrganizationMembersTable" + membersList.get(i).getUsername());
	
					}
		    	}
		    }
		    dbHandler.close();
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {
		// Just move on for now
		Log.d("Member List", "Selected HRE");
		TrelloMember item = (TrelloMember) memberAdapter.getItem(position);
		Log.d("Member List", "Selected:" + item.getFullname());
		
		if(item.getId() == null){
			//Add new member
			Intent go = new Intent(this, AddMembersList.class);
			startActivity(go);
		}
	}
}
