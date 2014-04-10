package com.openatk.trello;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.openatk.trello.R;
import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.database.LoginsTable;
import com.openatk.trello.internet.CommonLibrary;


import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;


public class Browser extends Activity {

	WebView browser = null;
	private TextView tv_loading = null;
	private static final String magicString = "25az225MAGICee4587da";
	private SQLiteDatabase database;
	private DatabaseHandler dbHandler;
	private String todo = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser);
		Bundle extras = getIntent().getExtras();
		if(extras != null){
			todo = extras.getString("todo");
		}
		if(todo != null && todo.contentEquals("add_account")){
			this.setTitle(R.string.add_member_list_topbar);
		} else if(todo != null && todo.contentEquals("change_account")) {
			this.setTitle(R.string.login_change_account);
		} else {
			this.setTitle(R.string.activity_main_topbar);
		}
		
		//Get database helper
		dbHandler = new DatabaseHandler(this);
		
		browser = (WebView) findViewById(R.id.browser);
		tv_loading = (TextView) findViewById(R.id.browser_loading);
		browser.getSettings().setJavaScriptEnabled(true);
		
		//Remove cookies to logout of trello and google
		android.webkit.CookieManager.getInstance().removeSessionCookie();
		android.webkit.CookieManager.getInstance().removeAllCookie();
		
		browser.setWebChromeClient(new PageHandler(this));
		browser.setWebViewClient(new WebViewClient() {
			
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				// TODO Auto-generated method stub
				super.onPageStarted(view, url, favicon);
				tv_loading.setVisibility(View.VISIBLE);
				browser.setVisibility(View.GONE);
			}

			public void onPageFinished(WebView view, String address){
				view.loadUrl("javascript:console.log('"+magicString+"'+'"+address+" - '+document.getElementsByTagName('html')[0].innerHTML);");
			}
		});		
		//Go to trello signup screen
		browser.loadUrl("https://trello.com/signup");
	}
	
	private class APIKeyHandler extends WebChromeClient {
		WebView apiBrowser = null;
		String apiKey = null;
		String token = null;
		String secret = null;
		String name = null;
		String username = null;
		
		Context appContext = null;
		
		public APIKeyHandler(WebView theBrowser, Context theContext, String Name, String Username){
			apiBrowser = theBrowser;
			appContext = theContext;
			name = Name;
			username = Username;
		}
		public boolean onConsoleMessage(ConsoleMessage cmsg){
			if(cmsg.message().startsWith(magicString)){
				
				String categoryMsg = cmsg.message().substring(magicString.length());
				if(categoryMsg.startsWith("KEY")){
					String msg = categoryMsg.substring(3);
					apiKey = msg;
					
					logMessage("The key: " + msg);
				} else if (categoryMsg.startsWith("SECRET")){
					String msg = categoryMsg.substring(6);
					secret = msg;
					
					logMessage("The secret: " + msg);
				} else if (categoryMsg.startsWith("TOKEN")){
					//After we automatically approve
					String msg = categoryMsg.substring(5);
					token = msg;
					logMessage("The token: " + msg);
				}
				if(apiKey!= null && secret != null && token == null){
					//Request the token
					apiBrowser.setWebViewClient(new WebViewClient() {
						public void onPageFinished(WebView view, String address){
							//Setup what to do after we approve
							apiBrowser.setWebViewClient(new WebViewClient() {
								public void onPageFinished(WebView view, String address){
									//Save the token
									view.loadUrl("javascript:console.log('"+magicString+"'+'TOKEN'+document.getElementsByTagName('pre')[0].innerHTML);");
								}
							});
							
							//Auto approve
							view.loadUrl("javascript:document.getElementsByTagName('form')[0].approve.click();");
						}
					});
					//Ask for approve of Application
					apiBrowser.loadUrl("https://trello.com/1/authorize?key="+
							apiKey+"&name=Test+Create+Trello&expiration=never&response_type=token&scope=read,write");
				}
				if(apiKey != null && secret != null && token != null){
					//Have all API keys
					//Store in database LOGINS table
					if(username != null && name != null){
						database = dbHandler.getWritableDatabase();
						
						//Look for username in database
						Long foundId = null;
						String[] columns = { LoginsTable.COL_ID, LoginsTable.COL_USERNAME};
						String where = LoginsTable.COL_USERNAME + " = '" + username + "'";
						Cursor cursor = database.query(LoginsTable.TABLE_NAME, columns, where, null,null, null, null);
					    if(cursor.moveToFirst()) {	    	
					    	foundId = cursor.getLong(cursor.getColumnIndex(LoginsTable.COL_ID));
					    }
					    cursor.close();
						
						//Update all others to inactive
						ContentValues updateValues = new ContentValues();
						updateValues.put(LoginsTable.COL_ACTIVE, 0);
						String where2 = LoginsTable.COL_ACTIVE + " = 1";
						database.update(LoginsTable.TABLE_NAME, updateValues, where2, null);
						
						ContentValues values = new ContentValues();
						values.put(LoginsTable.COL_NAME, name);
						values.put(LoginsTable.COL_USERNAME, username);
						values.put(LoginsTable.COL_APIKEY, apiKey);
						values.put(LoginsTable.COL_SECRET, secret);
						values.put(LoginsTable.COL_TOKEN, token);
						values.put(LoginsTable.COL_ACTIVE, 1);
						if(foundId == null){
							//Insert new as active
							foundId = database.insert(LoginsTable.TABLE_NAME, null, values);
							Log.d("Browser - APIKeyHandler", "Inserting login");
						} else {
							//Update this id to active
							String where3 = LoginsTable.COL_ID + " = " + Long.toString(foundId);
							database.update(LoginsTable.TABLE_NAME, values, where3, null);
						}
						dbHandler.close();
						//Save last login id to preferences
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						SharedPreferences.Editor editor = prefs.edit();
						editor.putLong("LastLoginId", foundId);
						
						editor.putString("apiKey", apiKey.trim()); //Temp
						editor.putString("token", token.trim()); //Temp
						editor.commit();
						logMessage("Saved login to database Name:" + name + " Username:" + username + " Id:" + Long.toString(foundId));
					}
					
					//Go to organization list
					Intent go = new Intent(appContext, OrganizationsList.class);
					startActivity(go);
				}
				return true;
			}
			return false;
		}
	}
	
	
	private class PageHandler extends WebChromeClient {
		Context appContext = null;
		String username = null;
		String name = null;
		public PageHandler(Context theContext){
			appContext = theContext;
		}
		
		public boolean onConsoleMessage(ConsoleMessage cmsg){
			if(cmsg.message().startsWith(magicString)){
				String categoryMsg = cmsg.message().substring(magicString.length());
				if(categoryMsg.contains("https://trello.com/ - ")){
					Integer intStartTitle = categoryMsg.indexOf("<title>", 0);
					Integer intEndTitle = categoryMsg.indexOf("</title>", intStartTitle);
					String title = categoryMsg.substring(intStartTitle + "<title>".length(), intEndTitle);
					logMessage(title);
					//Got name and username, get API keys if needed
					Pattern pattern = Pattern.compile("^(.* )[(](.*)[)]");
					Matcher matcher = pattern.matcher(title);
					if (matcher.find())
					{
						name = matcher.group(1);
						username = matcher.group(2);
					}

					//Done loading get API KEY's if needed
					if(todo != null && todo.contentEquals("add_account")){
						//Add this user to organization
						SharedPreferences prefs = PreferenceManager
								.getDefaultSharedPreferences(getApplicationContext());
						String orgoId = prefs.getString("organizationId", null);
						String apiKey = prefs.getString("apiKey", null);
						String token = prefs.getString("token", null);
						
						if(orgoId !=  null && apiKey != null && token != null){
							new asyncAddMemberOnTrello().execute(username, apiKey, token, orgoId);
						}
					} else {
						WebView apiBrowswer = new WebView(getApplicationContext());
						apiBrowswer.getSettings().setJavaScriptEnabled(true);
						apiBrowswer.setWebChromeClient(new APIKeyHandler(apiBrowswer, appContext, name, username));
						apiBrowswer.setWebViewClient(new WebViewClient() {
							public void onPageFinished(WebView view, String address){
								view.loadUrl("javascript:console.log('"+magicString+"'+'KEY'+document.getElementById('key').value);" +
										"console.log('"+magicString+"'+'SECRET'+document.getElementById('secret').value);");
							}
						});
						//Load api key page
						apiBrowswer.loadUrl("https://trello.com/1/appKey/generate");
					}
				} else {
					tv_loading.setVisibility(View.GONE);
					browser.setVisibility(View.VISIBLE);
				}
				return true;
			}
			return false;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode)
            {
            case KeyEvent.KEYCODE_BACK:
                if(browser.canGoBack() == true){
                	browser.goBack();
                }else{
                    finish();
                }
                return true;
            }

        }
		return super.onKeyDown(keyCode, event);
	}

	
	public void logMessage(String msg){
		if (msg.length() > 4000) {
		    Log.v("Trello", "sb.length = " + msg.length());
		    int chunkCount = msg.length() / 4000;     // integer division
		    for (int i = 0; i < chunkCount; i++) {
		        int max = 4000 * (i + 1);
		        if (max >= msg.length()) {
		            Log.v("Trello", "chunk " + (i+1) + " of " + chunkCount + ":" + msg.substring(4000 * i));
		        } else {
		            Log.v("Trello", "chunk " + (i+1) + " of " + chunkCount + ":" + msg.substring(4000 * i, max));
		        }
		    }
		} else {
			Log.v("Trello", msg);
		}
	}
	private class asyncAddMemberOnTrello extends AsyncTask<String, Integer, Boolean> {		
		protected Boolean doInBackground(String... query) {
			String username = query[0];
			Log.d("AddMembersHandler - AddMemberOnTrello", "Called");
			HttpClient client = new DefaultHttpClient();
			List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
			results.add(new BasicNameValuePair("key",query[1]));
			results.add(new BasicNameValuePair("token",query[2]));
			//Add to orgo
			HttpPut put = new HttpPut("https://api.trello.com/1/organizations/"+ query[3] +"/members/" + username);
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
			return true; //TODO return null on failure
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}

		protected void onPostExecute(Boolean success) {
			if(success) {
				//Completed success add it
				//Go back to MembersList
				Intent go = new Intent(getApplicationContext(), MembersList.class);
				startActivity(go);
			} else {
				//TODO failed
				//Toast toast = Toast.makeText(parent, parent.getString(R.string.member_list_remove_failed), Toast.LENGTH_LONG);
				//toast.show();
			}
		}
	}
}
