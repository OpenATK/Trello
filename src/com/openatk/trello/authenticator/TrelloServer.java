package com.openatk.trello.authenticator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

public class TrelloServer  {
    
	private String scope = "";
	private String callbackUrl = "openatk://openatk.com";
	
	
    private OAuthHelper authHelper;
    
    public TrelloServer(Context context) throws UnsupportedEncodingException{
		authHelper = new OAuthHelper(AccountGeneral.API_KEY, AccountGeneral.API_SECRET, scope, callbackUrl, context);
    }

    public String beginOAuth() throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
    	//Sign up or sign in to Trello account for authentication
    	//Do OAuth
    	return authHelper.getRequestToken();
    }
    
    public String[] finishOAuth(Uri uri) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException{
    	String[] token = getVerifier(uri);
    	String[] accessToken = getVerifier(uri);
		if (token != null) {
			Log.d("Token not null", "Token not null");
				accessToken = authHelper.getAccessToken(token[1]);
				Log.d("Token1:", accessToken[0]);
				Log.d("Token2:", accessToken[1]);
				return accessToken;
		}
		return null;
    }
    
    private String[] getVerifier(Uri uri) {
	    // extract the token if it exists
	    if (uri == null) {
		    Log.d("Getting verifier1:", "URI Null");
	        return null;
	    }
	    Log.d("Getting verifier2:", uri.toString());
	    String token = uri.getQueryParameter("oauth_token");
	    String verifier = uri.getQueryParameter("oauth_verifier");
	    return new String[] { token, verifier };
	}
    
    public TrelloMember getTrelloAccountInfo(String token){
    	String url = "https://api.trello.com/1/members/me?key=" + AccountGeneral.API_KEY + "&token=" + token + "&fields=fullName,username,email";
    	HttpResponse response = getData(url);
    	String json = entityToString(response.getEntity());
    	Log.d("getTrelloAccountInfo:", json);
    	Gson gson = new Gson();
    	TrelloMember member = gson.fromJson(json, TrelloMember.class);
    	return member;
    }
    
    public static HttpResponse getData(String url) {
		HttpResponse response = null;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet();
			request.setURI(new URI(url));
			response = client.execute(request);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}
    public static String entityToString(HttpEntity entity) {
  	  InputStream is = null;
  		try {
  			is = entity.getContent();
  		} catch (IllegalStateException e1) {
  			// TODO Auto-generated catch block
  			e1.printStackTrace();
  		} catch (IOException e1) {
  			// TODO Auto-generated catch block
  			e1.printStackTrace();
  		}
  	  BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
  	  StringBuilder str = new StringBuilder();

  	  String line = null;
  	  try {
  	    while ((line = bufferedReader.readLine()) != null) {
  	      str.append(line + "\n");
  	    }
  	  } catch (IOException e) {
  	    throw new RuntimeException(e);
  	  } finally {
  	    try {
  	      is.close();
  	    } catch (IOException e) {
  	      //tough luck...
  	    }
  	  }
  	  return str.toString();
  	}
}
