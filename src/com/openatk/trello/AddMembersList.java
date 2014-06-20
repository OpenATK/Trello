package com.openatk.trello;

import java.util.ArrayList;
import java.util.List;

import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.database.OrganizationMembersTable;
import com.openatk.trello.internet.AddMembersHandler;
import com.openatk.trello.internet.TrelloMember;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

public class AddMembersList extends Activity implements
		OnItemClickListener, TextWatcher {

	private ListView membersListView = null;
	private EditText searchBox = null;

	private AddMembersHandler membersHandler = null;
	private List<TrelloMember> membersList = null;
	AddMembersArrayAdapter memberAdapter = null;

	private boolean loading = false;
	private boolean adding = false;
	
	DatabaseHandler dbHandler = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_member_list);
		membersListView = (ListView) findViewById(R.id.add_member_list_view);
		searchBox = (EditText) findViewById(R.id.member_search);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String orgoId = prefs.getString("organizationId", "null");
		String accountName = prefs.getString("accountName", null);
		
		membersList = new ArrayList<TrelloMember>();
		membersHandler = new AddMembersHandler(this, membersList, orgoId, accountName);
		
		memberAdapter = new AddMembersArrayAdapter(this, R.layout.member_list_item, membersList, membersHandler);
		
		dbHandler = new DatabaseHandler(this);

		searchBox.addTextChangedListener(this);
		membersListView.setAdapter(memberAdapter);
		membersListView.setOnItemClickListener(this);
		
		membersHandler.getMembersList(null);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		//Todo refresh button
		getMenuInflater().inflate(R.menu.members_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == R.id.menu_members_done) {
			//Go back to memberList
			Intent go = new Intent(this, MembersList.class);
			startActivity(go);
		}
		return false;
	}

	public void doneLoadingList() {
		// Done loading organizations update list
		if (loading) {
			loading = false;
			// Remove loading screen
		}
		if(adding){
			adding = false;
			//Go back to memberList
			Intent go = new Intent(this, MembersList.class);
			startActivity(go);
		}
		((AddMembersArrayAdapter) membersListView.getAdapter()).dataChanged();
	}
	
	public void updateMemberListView() {
		((AddMembersArrayAdapter) membersListView.getAdapter()).dataChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {
		// Just move on for now
		final TrelloMember item = (TrelloMember) memberAdapter.getItem(position);
		Log.d("AddMembersList - onItemClick", "Selected:" + item.getFullname());
		if(item.getId() == null){
			//New or loading
			if(item.getFullname().contentEquals(this.getString(R.string.add_member_list_new_person))){
				//Create New Trello Account
				Log.d("AddMembersList - onItemClick", "Create new Trello Account");
				/*Intent go = new Intent(this, Browser.class);
				go.putExtra("todo", "add_account");
				startActivity(go);*/
				Log.w("AddMembersList", "Create new trello account");
			}
		} else {
			if(item.getInOrgo() == false){
				Log.d("Adding:", item.getFullname());
				
				//Update organization add this member
				String message = this.getString(R.string.add_member_list_add_dialog_message_1) + " " + item.getFullname() + " (" + item.getUsername() + ") " + this.getString(R.string.add_member_list_add_dialog_message_2);
				new AlertDialog.Builder(this)
				.setTitle(this.getString(R.string.add_member_list_add_dialog_title))
				.setMessage(message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.add_member_list_add_dialog_yes, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int whichButton) {
				    	item.setInOrgo(true);
				    	adding = true;
				    	//Add to database
				    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						String orgoId = prefs.getString("organizationId", "null");
				    	Log.d("AddMembersList - onItemClick", "Adding member, orgid:" + orgoId);
						if(orgoId.contentEquals("null") == false){
							SQLiteDatabase database = dbHandler.getWritableDatabase();
							//Look for member
							String[] columns = { OrganizationMembersTable.COL_MEMBER_ID, OrganizationMembersTable.COL_ORGO_ID };
							String where = OrganizationMembersTable.COL_MEMBER_ID + " = '" + item.getId() + "' AND " + OrganizationMembersTable.COL_ORGO_ID + " = '" + orgoId + "'";
							Cursor cursor = database.query(OrganizationMembersTable.TABLE_NAME, columns, where, null,null, null, null);
						    Boolean found = false;
							if(cursor.moveToFirst() == false) {
						    	//Found member
								found = true;
						    	Log.d("AddMembersList - onItemClick", "Already found member, ignoring");
						    }
						    cursor.close();
						    if(found == false){
						    	Log.d("AddMembersList - onItemClick", "Adding member to OrganizationMembersTable");
						    	ContentValues values = new ContentValues();
								values.put(OrganizationMembersTable.COL_MEMBER_ID, item.getId());
								values.put(OrganizationMembersTable.COL_USERNAME, item.getUsername());
								values.put(OrganizationMembersTable.COL_NAME, item.getFullname());
								values.put(OrganizationMembersTable.COL_ORGO_ID, orgoId);
								database.insert(OrganizationMembersTable.TABLE_NAME, null,values);
						    }
						    dbHandler.close();
						}
				    	membersHandler.AddMemberOnTrello(item);
				    }})
				 .setNegativeButton(R.string.add_member_list_add_dialog_no, null).show();
			}	
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		Log.d("MembersList - afterTextChanged", "NewText:" + s.toString());
		membersHandler.getMembersList(s.toString());
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		
	}
}
