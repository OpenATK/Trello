package com.openatk.trello.authenticator;

import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

public class OAuthHelper {
	private OAuthConsumer mConsumer;
	private OAuthProvider mProvider;
	private String mCallbackUrl;
	private Context context;
	
	public OAuthHelper(String consumerKey, String consumerSecret, String scope, String callbackUrl, Context context)
			throws UnsupportedEncodingException {
	    mConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
	    mProvider = new CommonsHttpOAuthProvider("https://trello.com/1/OAuthGetRequestToken",
	    "https://trello.com/1/OAuthGetAccessToken", "https://trello.com/1/OAuthAuthorizeToken?name=OpenATK+Trello&expiration=never&scope=read,write,account");
	    mProvider.setOAuth10a(true);
	    mCallbackUrl = (callbackUrl == null ? OAuth.OUT_OF_BAND : callbackUrl);
	    this.context = context;
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
	    if(prefs.contains("reqToken")){
	    	mConsumer.setTokenWithSecret(prefs.getString("reqToken", ""), prefs.getString("reqTokenSecret", ""));
	    }
	}
	
	public String getRequestToken() throws OAuthMessageSignerException, OAuthNotAuthorizedException,
			OAuthExpectationFailedException, OAuthCommunicationException {
		
	    String authUrl = mProvider.retrieveRequestToken(mConsumer, mCallbackUrl);
	    
	    Log.d("Saving request token", "Saved");
	    Log.d("authURL:", authUrl);
	    Log.d("reqToken:", mConsumer.getToken());
	    Log.d("reqTokenSecret:", mConsumer.getTokenSecret());

	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("reqToken", mConsumer.getToken());
		editor.putString("reqTokenSecret",mConsumer.getTokenSecret()); 
		editor.commit();
	    
	    return authUrl;
	}
	
	public String[] getAccessToken(String verifier)
			throws OAuthMessageSignerException, OAuthNotAuthorizedException,
			OAuthExpectationFailedException, OAuthCommunicationException {
		Log.d("Verifier:", verifier);
		
	    mProvider.retrieveAccessToken(mConsumer, verifier);
		Log.d("Secret:", mConsumer.getToken());
		Log.d("Token:", mConsumer.getTokenSecret());
		
	    return new String[] {
	        mConsumer.getToken(), mConsumer.getTokenSecret()
	    };
	}
}
