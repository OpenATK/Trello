package com.openatk.trello;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.database.OrganizationMembersTable;
import com.openatk.trello.internet.MembersHandler;
import com.openatk.trello.internet.TrelloMember;


import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class MembersArrayAdapter extends ArrayAdapter<TrelloMember> {
	private final Context context;
	private List<TrelloMember> members = null;
	private int resId;
	private Bitmap defaultIcon;
	private Bitmap addIcon;
	private MembersHandler handler;
	DatabaseHandler dbHandler = null;

	
	public MembersArrayAdapter(Context context, int layoutResourceId,
			List<TrelloMember> data, MembersHandler handler) {
		super(context, layoutResourceId, data);
		this.resId = layoutResourceId;
		this.context = context;
		this.members = data;
		this.handler = handler;
		defaultIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.empty_person);
		addIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.add);
		dbHandler = new DatabaseHandler(context);
	}
	
	private class MemberComparator implements Comparator<TrelloMember> {
		public int compare(TrelloMember a, TrelloMember b) {
			if(a.getId() == null){
				return -1;
			}
		    if(a.getInOrgo() == b.getInOrgo()){
		    	return a.getFullname().compareTo(b.getFullname());
		    } else {
		    	if(a.getInOrgo()){
		    		return 1;
		    	} else {
		    		return -1;
		    	}
		    }
		}
	}

	public void dataChanged(){
		super.sort(new MemberComparator());
	}
	
	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View row = convertView;
		ListItemHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(resId, parent, false);
			holder = new ListItemHolder();
	
			holder.txtLargeCenter = (TextView) row
						.findViewById(R.id.member_list_item_largeText_center);
			holder.txtLarge = (TextView) row
						.findViewById(R.id.member_list_item_largeText);
			holder.txtSmall = (TextView) row
					.findViewById(R.id.member_list_item_smallText);
			holder.imgIcon = (ImageView) row
					.findViewById(R.id.member_list_item_image);
			holder.buttonRemove = (ImageButton) row.findViewById(R.id.member_list_item_remove);
			row.setTag(holder);
			
			holder.buttonRemove.setTag(holder);
			holder.buttonRemove.setOnClickListener(chkListener);
		} else {
			holder = (ListItemHolder) row.getTag();
		}

		holder.member = members.get(position);
		
		if(members.get(position).getId() == null){
			holder.txtLargeCenter.setText(members.get(position).getFullname());
			holder.txtLargeCenter.setVisibility(View.VISIBLE);
			holder.txtLarge.setVisibility(View.INVISIBLE);
			holder.txtSmall.setVisibility(View.INVISIBLE);
		} else {
			holder.txtLarge.setText(members.get(position).getFullname());
			holder.txtSmall.setText(members.get(position).getUsername());
			holder.txtLargeCenter.setVisibility(View.INVISIBLE);
			holder.txtLarge.setVisibility(View.VISIBLE);
			holder.txtSmall.setVisibility(View.VISIBLE);
		}
		
		if(members.get(position).getAvatar() != null){
			holder.imgIcon.setImageBitmap(members.get(position).getAvatar());
		} else {
			//No avatar image, load it if we have a hash
			if(members.get(position).getAvatarHash() != null && members.get(position).getAvatarHash().length() != 0 && members.get(position).getAvatarHash().contentEquals("null") == false) {
				new DownloadImageTask(members.get(position), this).execute("https://trello-avatars.s3.amazonaws.com/" + members.get(position).getAvatarHash() + "/170.png");
				Log.d("downling","here");
			} else {
				//Set avatar to default image
				if(members.get(position).getId() == null){
					//Add member icon
					members.get(position).setAvatar(addIcon);
					//Hide the remove button
					holder.buttonRemove.setVisibility(View.INVISIBLE);
				} else {
					members.get(position).setAvatar(defaultIcon);
					//Show the remove button
					holder.buttonRemove.setVisibility(View.VISIBLE);
				}
			}
			holder.imgIcon.setImageBitmap(members.get(position).getAvatar());
		}
		
		//Set check box by already in organization
		Log.d("MembersArrayAdapter - getView", members.get(position).getFullname() + " Checked:" + members.get(position).getInOrgo().toString());
		return row;
	}

	static class ListItemHolder {
		ImageView imgIcon;
		TextView txtLarge;
		TextView txtLargeCenter;
		TextView txtSmall;
		ImageButton buttonRemove;
		TrelloMember member;
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		TrelloMember parent;
		MembersArrayAdapter adapter;

		public DownloadImageTask(TrelloMember parent, MembersArrayAdapter adapter) {
			this.parent = parent;
			this.adapter = adapter;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			parent.setAvatar(result);
			this.adapter.notifyDataSetChanged();
		}
	}
	
	private OnClickListener chkListener = new OnClickListener(){
		@Override
		public void onClick(View arg0) {
			//Remove member
			final ListItemHolder holder = (ListItemHolder) arg0.getTag();
			if(holder.member.getInOrgo() == true){
				Log.d("Removing:", holder.member.getFullname());
				
				//Update organization add or remove this member
				String message = context.getString(R.string.member_list_remove_dialog_message_1) + " " + holder.member.getFullname() + " " + context.getString(R.string.member_list_remove_dialog_message_2);
				new AlertDialog.Builder(context)
				.setTitle(context.getString(R.string.member_list_remove_dialog_title))
				.setMessage(message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.member_list_remove_dialog_yes, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int whichButton) {
						holder.member.setInOrgo(false);
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
						String orgoId = prefs.getString("organizationId", "null");
						if(orgoId.contentEquals("null") == false){
							SQLiteDatabase database = dbHandler.getWritableDatabase();
							//Look for member
							String where = OrganizationMembersTable.COL_MEMBER_ID + " = '" + holder.member.getId() + "' AND " + OrganizationMembersTable.COL_ORGO_ID + " = '" + orgoId + "'";
							database.delete(OrganizationMembersTable.TABLE_NAME, where, null);
						    dbHandler.close();
						}
						handler.RemoveMemberOnTrello(holder.member);
				    }})
				 .setNegativeButton(R.string.member_list_remove_dialog_no, null).show();
			}			
		}
	};

}
