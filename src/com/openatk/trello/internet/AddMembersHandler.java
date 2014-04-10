package com.openatk.trello.internet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.openatk.trello.AddMembersList;
import com.openatk.trello.R;
import com.openatk.trello.authenticator.AccountGeneral;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
public class AddMembersHandler  {
	private List<TrelloMember> memberList = null;
	private AddMembersList parent;
	private String organizationId;
	private String accountName;
	private getMembersFromTrello lastTaskgetMembersList = null;

	public AddMembersHandler(Context parent, List<TrelloMember> Members, String organizationId, String accountName) {
		memberList = Members;
		this.parent = (AddMembersList) parent;
		this.organizationId = organizationId;
		this.accountName = accountName;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public void getMembersList(String query) {
		// populate list
		if(lastTaskgetMembersList != null){
			lastTaskgetMembersList.cancel(true);
			lastTaskgetMembersList = null;
		}
		lastTaskgetMembersList = new getMembersFromTrello();
		if(query != null){
			memberList.clear();
			TrelloMember loadingMember = new TrelloMember(null, parent.getString(R.string.add_member_list_loading), null, null, true);
			memberList.add(loadingMember);
			parent.updateMemberListView();
		}
		lastTaskgetMembersList.execute(organizationId, accountName, query);
	}
	
	
	private class getMembersFromTrello extends AsyncTask<String, Integer, List<TrelloMember>> {
		private List<TrelloMember> existingMemberList = new ArrayList<TrelloMember>();
		private List<TrelloMember> newMemberList = new ArrayList<TrelloMember>();
		
		protected List<TrelloMember> doInBackground(String... query) {
			if(query[2] != null){
				//Wait a second for cancel
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e2) {
					//Canceled probably
				}
				if(this.isCancelled()){
					newMemberList = null;
					existingMemberList = null;
				} else {
					newMemberList.clear();
					existingMemberList.clear();
					
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
							String member_avatarHash = member.getString("avatarHash");
							newMember = new TrelloMember(member_id, member_fullname, member_username, member_avatarHash, true);
						} catch (JSONException e) {
							newMember = null;
							e.printStackTrace();
						}
						// Add this member to the list
						if (newMember != null) existingMemberList.add(newMember);
					}
					
					//Get members of with this query (Searching)
					String encodedQuery = null;
					if(query[2] != null && query[2].length() != 0){
						try {
							encodedQuery = URLEncoder.encode(query[2], "utf-8");
						} catch (UnsupportedEncodingException e1) {
							encodedQuery = null;
							e1.printStackTrace();
						}
						url = "https://api.trello.com/1/search/members?query=" + encodedQuery;
		
						response = CommonLibrary.getData(url);
						result = "";
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
		
						JSONArray members = null;
						try {
							members = new JSONArray(result);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						
						//Convert this to members
						// Add these to list
						for (int i = 0; i < members.length(); i++) {
		
							JSONObject member = null;
		
							TrelloMember newMember = null;
		
							try {
								member = members.getJSONObject(i);
								String member_id = member.getString("id");
								String member_username = member.getString("username");
								String member_fullname = member.getString("fullName");
								String member_avatarHash = member.getString("avatarHash");
	
								Boolean found = false;
								for(int x=0; x<existingMemberList.size(); x++){
									if(existingMemberList.get(x).getId().contentEquals(member_id)){
										found = true;
									}
								}
								if(found == false){
									Log.d("MembersHandler - getMembersFromTrello", "Search Member:" + member_fullname);
									newMember = new TrelloMember(member_id, member_fullname, member_username, member_avatarHash, false);
								}
							} catch (JSONException e) {
								newMember = null;
								e.printStackTrace();
							}
							// Add this member to the list
							if (newMember != null) newMemberList.add(newMember);
						}
					}
				}
			}
			TrelloMember newMember = new TrelloMember(null, parent.getString(R.string.add_member_list_new_person), null, null, true);
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

	public void AddMemberOnTrello(final TrelloMember member){
		new asyncAddMemberOnTrello().execute(member);
	}
	
	private class asyncAddMemberOnTrello extends AsyncTask<TrelloMember, Integer, TrelloMember> {		
		
		protected TrelloMember doInBackground(TrelloMember... query) {
			TrelloMember member = query[0];
			Log.d("AddMembersHandler - AddMemberOnTrello", "Called");
			HttpClient client = new DefaultHttpClient();
			
			
			
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
			results.add(new BasicNameValuePair("key",AccountGeneral.API_KEY));
			results.add(new BasicNameValuePair("token",authToken));
			if(member.getInOrgo() == true){
				//Add to orgo
				HttpPut put = new HttpPut("https://api.trello.com/1/organizations/"+ organizationId +"/members/" + member.getId());
				results.add(new BasicNameValuePair("type","normal"));
				try {
					String result = "";
					try {
						put.setEntity(new UrlEncodedFormEntity(results));
						HttpResponse response = client.execute(put);
						// Error here if no Internet TODO
						InputStream is = response.getEntity().getContent(); 
						result = CommonLibrary.convertStreamToString(is);
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					Log.d("AddMembersHandler - AddMemberOnTrello", "Add Response:" + result);
				} catch (Exception e) {
					// Auto-generated catch block
					Log.e("AddMembersHandler - AddMemberOnTrello","client protocol exception", e);
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
				//Completed success add it
				memberList.add(member);
				// Notify list that its done adding
				parent.doneLoadingList();
			} else {
				//TODO failed
				//Toast toast = Toast.makeText(parent, parent.getString(R.string.member_list_remove_failed), Toast.LENGTH_LONG);
				//toast.show();
			}
		}
	}

}
