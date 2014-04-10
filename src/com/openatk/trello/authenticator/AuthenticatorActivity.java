package com.openatk.trello.authenticator;

import java.io.UnsupportedEncodingException;

import com.openatk.trello.R;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * The Authenticator activity.
 * <p/>
 * Called by the Authenticator and in charge of identifing the user.
 * <p/>
 * It sends back to the Authenticator the result.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    private final String TAG = this.getClass().getSimpleName();

    private AccountManager mAccountManager;
    private String mAuthTokenType;
    private TrelloServer trelloServer = null;
    private WebView browser;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authenticator);
        
        try {
			trelloServer = new TrelloServer(this.getApplicationContext());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
  
        mAccountManager = AccountManager.get(getBaseContext());
        
        mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE); //IDK when would happen no editing?
        
        if (mAuthTokenType == null) mAuthTokenType = AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS;

        browser = (WebView) findViewById(R.id.browser);
		browser.getSettings().setJavaScriptEnabled(true);
		
		//Remove cookies to logout of trello and google
		//android.webkit.CookieManager.getInstance().removeSessionCookie();
		//android.webkit.CookieManager.getInstance().removeAllCookie();
		
		browser.setWebViewClient(new WebViewClient() {	
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				if(url.contains("openatk")){
					browser.setVisibility(View.GONE);
            		step2(url);
				}
			}
			public void onPageFinished(WebView view, String address){
				
			}
		});
		
		//Start the OAuth process for trello
        step1();
    }

    //Get reqTokens from trello and load the browser so user can accept
    public void step1() {
        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
        //Save account type
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("accountType", accountType);
		editor.commit();
        
        new AsyncTask<String, Void, Intent>() {
            @Override
            protected Intent doInBackground(String... params) {
                Log.d("udinic", TAG + "> Started authenticating");
                Bundle data = new Bundle();
                try {
                    String uri  = trelloServer.beginOAuth();
                    if(uri == null){
                        data.putString(KEY_ERROR_MESSAGE, "Unable to get request token");
                    } else {
                    	data.putString("URI", uri);
                    }
                    // We keep the user's object id as an extra data on the account.
                    // It's used later for determine ACL for the data we send to the Parse.com service
                    //TODO can store additional info about user here...
                    //Bundle userData = new Bundle();
                    //userData.putString(USERDATA_USER_OBJ_ID, user.getObjectId());
                    //data.putBundle(AccountManager.KEY_USERDATA, userData);
                } catch (Exception e) {
                    data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                }

                final Intent res = new Intent();
                res.putExtras(data);
                return res;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    Toast.makeText(getBaseContext(), intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show();
                } else {
                	Log.d("Returned no error", "starting browser if have uri");
                	if(intent.hasExtra("URI")){
                		browser.loadUrl(intent.getStringExtra("URI"));
                		browser.setVisibility(View.VISIBLE);
                	}
                }
            }
        }.execute();
    }
    
    //Use request token from browser and upgrade to access token
    private void step2(String uriString){
    	Uri uri = Uri.parse(uriString);
		if(uri != null){
			new AsyncTask<Uri, Void, Intent>() {
	            @Override
	            protected Intent doInBackground(Uri... params) {
	                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	                String accountType = prefs.getString("accountType", "");
	                Bundle data = new Bundle();
	                try {
	                	String[] accessToken = trelloServer.finishOAuth(params[0]);   
	                	data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
	                    data.putString(AccountManager.KEY_AUTHTOKEN, accessToken[0]);
	                } catch (Exception e) {
	                    data.putString(KEY_ERROR_MESSAGE, e.getMessage());
	                }
	                final Intent res = new Intent();
	                res.putExtras(data);
	                return res;
	            }
	
	            @Override
	            protected void onPostExecute(Intent intent) {
	                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
	                    Toast.makeText(getBaseContext(), intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show();
	                } else {
	                	//We are done
	                    step3(intent);
	                }
	            }
	        }.execute(uri);
		} else {
			Log.d("step2", "URI null skipping");
		}
    }  

    //Gather info about user
    private void step3(Intent intent){
    	new AsyncTask<Intent, Void, Intent>() {
            @Override
            protected Intent doInBackground(Intent... params) {
        		String authtoken = params[0].getStringExtra(AccountManager.KEY_AUTHTOKEN);
        		Log.d("authtoken:", authtoken);
                TrelloMember member = trelloServer.getTrelloAccountInfo(authtoken);
                if(member != null){
                	String accountName = "";
                	if(member.getEmail() != null){
                		accountName = member.getEmail();
                	} else {
                		accountName = member.getUsername();
                	}
                	params[0].putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
                }
                return params[0];
            }

            @Override
            protected void onPostExecute(Intent intent) {
            	step4(intent);
            }
        }.execute(intent);
    }
    
    //Return token and info to Account Manager
    private void step4(Intent intent) {
        //Get the account name from Trello
    	String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String authtokenType = mAuthTokenType;
        
        if(intent.hasExtra(AccountManager.KEY_ACCOUNT_NAME)){
        	String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

        	final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
        	Log.d("udinic", TAG + "> finishLogin > addAccountExplicitly");
	        // Creating the account on the device and setting the auth token we got
	        // (Not setting the auth token will cause another call to the server to authenticate the user)
	        mAccountManager.addAccountExplicitly(account, null, null);
	        
	        mAccountManager.setAuthToken(account, authtokenType, authtoken);
	        
	        //New intent to return
	        Intent res = new Intent();
	        res.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
            res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
            res.putExtra(AccountManager.KEY_AUTHTOKEN, authtoken);
	        
	        setAccountAuthenticatorResult(res.getExtras());
	        setResult(RESULT_OK, res);
        } else {
        	Log.d("udinic", TAG + "> finishLogin > Failed");
            Toast.makeText(getBaseContext(), "Failed to load account information. Please try again.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED, intent);
        }
        finish();
    }
}