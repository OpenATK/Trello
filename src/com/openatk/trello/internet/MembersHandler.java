package com.openatk.trello.internet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.openatk.trello.MembersList;
import com.openatk.trello.R;
import com.openatk.trello.authenticator.AccountGeneral;
import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.database.OrganizationMembersTable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class MembersHandler  {
	private List<TrelloMember> memberList = null;
	private MembersList parent;
	private String organizationId;
	private String accountName;
	private getExistingMembersFromTrello lastTaskgetExistingMembersList = null;

	public MembersHandler(Context parent, List<TrelloMember> Members, String organizationId, String accountName) {
		memberList = Members;
		this.parent = (MembersList) parent;
		this.organizationId = organizationId;
		this.accountName = accountName;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}
	
	public void getExistingMembersList() {
		// populate list
		if(lastTaskgetExistingMembersList != null){
			lastTaskgetExistingMembersList.cancel(true);
			lastTaskgetExistingMembersList = null;
		}
		lastTaskgetExistingMembersList = new getExistingMembersFromTrello();
		lastTaskgetExistingMembersList.execute(organizationId, accountName);
	}
	
	private class getExistingMembersFromTrello extends AsyncTask<String, Integer, List<TrelloMember>> {
		private List<TrelloMember> newMemberList = new ArrayList<TrelloMember>();
		
		protected List<TrelloMember> doInBackground(String... query) {
			//Wait a second for cancel
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e2) {
				//Canceled probably
			}
			if(this.isCancelled()){
				newMemberList = null;
			} else {
				newMemberList.clear();
				
				Account theAccount = new Account(query[1], AccountGeneral.ACCOUNT_TYPE);
		        AccountManager mAccountManager = AccountManager.get(parent.getApplicationContext());
		        String authToken = "";
				try {
					authToken = mAccountManager.blockingGetAuthToken(theAccount, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, true);
				} catch (OperationCanceledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (AuthenticatorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//Get existing members for this organization
				String url = "https://api.trello.com/1/organizations/" + query[0] + "/members?key=" + AccountGeneral.API_KEY + "&token=" + authToken + "&fields=fullName,username,avatarHash";
				
				HttpResponse response = CommonLibrary.getData(url);
				String result = "";
				try {
					// Error here if no Internet
					InputStream is = response.getEntity().getContent();
					result = CommonLibrary.convertStreamToString(is);
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	
				Log.d("MembersHandler", result);
				
				JSONArray orgoMembers = null;
				try {
					orgoMembers = new JSONArray(result);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				//Convert this to members
				// Add these to list
				for (int i = 0; i < orgoMembers.length(); i++) {
	
					JSONObject member = null;
	
					TrelloMember newMember = null;
	
					try {
						member = orgoMembers.getJSONObject(i);
						String member_id = member.getString("id");
						String member_username = member.getString("username");
						String member_fullname = member.getString("fullName");
						Log.d("MembersHandler - getMembersFromTrello", "Orgo Member:" + member_fullname);
						String member_avatarHash = member.getString("avatarHash");
						newMember = new TrelloMember(member_id, member_fullname, member_username, member_avatarHash, true);
					} catch (JSONException e) {
						newMember = null;
						e.printStackTrace();
					}
					// Add this member to the list
					if (newMember != null) newMemberList.add(newMember);
				}
			}
			
			TrelloMember newMember = new TrelloMember(null, parent.getString(R.string.member_list_add_person), null, null, true);
			newMemberList.add(newMember);
			return newMemberList;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}

		protected void onPostExecute(List<TrelloMember> newMemberList) {
			if(newMemberList == null) {
				//was canceled do nothing
			} else {
				memberList.clear();
				memberList.addAll(newMemberList);
				// Notify list that its done loading
				parent.doneLoadingList();
			}
		}
	}

	public void RemoveMemberOnTrello(final TrelloMember member){
		new asyncRemoveMemberOnTrello().execute(member);
	}
	
	
	private class asyncRemoveMemberOnTrello extends AsyncTask<TrelloMember, Integer, TrelloMember> {		
		
		protected TrelloMember doInBackground(TrelloMember... query) {
			TrelloMember member = query[0];
			Log.d("TrelloController - DeleteMemberOnTrello", "Called");
			HttpClient client = new DefaultHttpClient();
			HttpDeleteWithBody delete;
			
			
			Account theAccount = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);
	        AccountManager mAccountManager = AccountManager.get(parent.getApplicationContext());
	        String authToken = "";
			try {
				authToken = mAccountManager.blockingGetAuthToken(theAccount, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, true);
			} catch (OperationCanceledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AuthenticatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
			results.add(new BasicNameValuePair("key", AccountGeneral.API_KEY));
			results.add(new BasicNameValuePair("token",authToken));
			if(member.getInOrgo() == false){
				//Remove from orgo
				delete = new HttpDeleteWithBody("https://api.trello.com/1/organizations/" + organizationId + "/members/" + member.getId());
				try {
					String result = "";
					try {
						delete.setEntity(new UrlEncodedFormEntity(results));
						HttpResponse response = client.execute(delete);
						// Error here if no Internet TODO
						InputStream is = response.getEntity().getContent(); 
						result = CommonLibrary.convertStreamToString(is);
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					Log.d("TrelloController - DeleteMemberOnTrello", "Remove Response:" + result);
					if(result.contains("Not enough admins")){
						Log.d("TrelloController - DeleteMemberOnTrello", "Failed to remove");
						member.setInOrgo(true);
						return null;
					}
				} catch (Exception e) {
					// Auto-generated catch block
					Log.e("TrelloController - DeleteMemberOnTrello","client protocol exception", e);
				}
			}
			return member; //TODO return null on failure
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}

		protected void onPostExecute(TrelloMember member) {
			if(member != null) {
				//Completed success remove it
				int foundIndex = -1;
				for(int i=0; i<memberList.size(); i++){
					if(memberList.get(i).getId() != null && member.getId() != null){
						if(memberList.get(i).getId().contentEquals(member.getId())){
							foundIndex = i;
							break;
						}
					}
				}
				if(foundIndex != -1){
					memberList.remove(foundIndex);
				}
				// Notify list that its done removing
				parent.doneLoadingList();
			} else {
				//TODO failed
				Toast toast = Toast.makeText(parent, parent.getString(R.string.member_list_remove_failed), Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
	class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
	    public static final String METHOD_NAME = "DELETE";
	    public String getMethod() { return METHOD_NAME; }

	    public HttpDeleteWithBody(final String uri) {
	        super();
	        setURI(URI.create(uri));
	    }
	    public HttpDeleteWithBody(final URI uri) {
	        super();
	        setURI(uri);
	    }
	    public HttpDeleteWithBody() { super(); }
	}

}
