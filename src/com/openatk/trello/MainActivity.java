package com.openatk.trello;

import com.openatk.trello.authenticator.AccountGeneral;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	WebView browser = null;
	private Button setupAccount = null;
	private AccountManager mAccountManager;
	private Account mConnectedAccount;
	WebView webView = null;
    private static final String AUTHORITY = "com.openatk.trello";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.setTitle(R.string.activity_main_topbar);
		
		
        mAccountManager = AccountManager.get(this);
		setupAccount = (Button) findViewById(R.id.setupAccount);
		setupAccount.setOnClickListener(this);

		
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);

		Boolean setup = prefs.getBoolean("FirstSetup", false);
		if(setup){
			Intent go = new Intent(this, AppsList.class);
			startActivity(go);
			finish();
		} else {
			//Make setupAccount button visible
			setupAccount.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.setupAccount){
			//Go to browser
			/*Intent go = new Intent(this, Browser.class);
			go.putExtra("todo", "setup_account");
			startActivity(go);*/
            MainActivity.getTokenForAccountCreateIfNeeded(AccountGeneral.ACCOUNT_TYPE, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, this);
		}		
	}
	
	public static void getTokenForAccountCreateIfNeeded(String accountType, String authTokenType, final Activity activity) {
        final AccountManagerFuture<Bundle> future = AccountManager.get(activity).getAuthTokenByFeatures(accountType, authTokenType, null, activity, null, null,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        Bundle bnd = null;
                        try {
                            bnd = future.getResult();
                            String authToken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                            if (authToken != null) {
                                String accountName = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                                //mConnectedAccount = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);
                                //Save the accountname to prefs
                        		SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        						SharedPreferences.Editor editor = prefs.edit();
        						Log.d("MainActivity", "Account Name:" + accountName);
        						editor.putString("accountName", accountName); //Temp
        						editor.commit();
                            }
                            //showMessage(((authToken != null) ? "SUCCESS!\ntoken: " + authToken : "FAIL"));
                            Log.d("udinic", "GetTokenForAccount Bundle is " + bnd);
                            //Done and worked... move on
                            //Go to organization list
        					Intent go = new Intent(activity.getApplicationContext(), OrganizationsList.class);
        					activity.startActivity(go);
                        } catch (Exception e) {
                        	Log.d("getTokenForAccountCreateIfNeeded", "error");
                            e.printStackTrace();
                        }
                    }
                }
        , null);
    }
	
	private void showMessage(final String msg) {
		if (msg == null || msg.trim().equals(""))
		    return;
		
		runOnUiThread(new Runnable() {
		    @Override
		    public void run() {
		    	Log.d("MainActivity", "I am here");
		        //Toast.makeText(getBaseContext(),"MainActivity" + msg, Toast.LENGTH_SHORT).show();
		    }
		});
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mainactivity, menu);
		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.menu_legal){
			CharSequence licence= "The MIT License (MIT)\n" +
	                "\n" +
	                "Copyright (c) 2013 Purdue University\n" +
	                "\n" +
	                "Permission is hereby granted, free of charge, to any person obtaining a copy " +
	                "of this software and associated documentation files (the \"Software\"), to deal " +
	                "in the Software without restriction, including without limitation the rights " +
	                "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
	                "copies of the Software, and to permit persons to whom the Software is " +
	                "furnished to do so, subject to the following conditions:" +
	                "\n" +
	                "The above copyright notice and this permission notice shall be included in " +
	                "all copies or substantial portions of the Software.\n" +
	                "\n" +
	                "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR " +
	                "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, " +
	                "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE " +
	                "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER " +
	                "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, " +
	                "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN " +
	                "THE SOFTWARE.\n";
			new AlertDialog.Builder(this)
				.setTitle("Legal")
				.setMessage(licence)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton("Close", null).show();
		}
		return true;
	}
}
