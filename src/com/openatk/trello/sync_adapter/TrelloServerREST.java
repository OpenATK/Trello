package com.openatk.trello.sync_adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.openatk.trello.response.ActionResponse;
import com.openatk.trello.response.BoardResponse;
import com.openatk.trello.response.OrganizationResponse;
import com.openatk.trello.shared.TrelloBoard;
import com.openatk.trello.shared.TrelloCard;
import com.openatk.trello.shared.TrelloList;

public class TrelloServerREST {
	private static String devKey = "b1ae1192adda1b5b61563d30d7ab403b";
	private String token;
	
	private static SimpleDateFormat trelloDateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
	public TrelloServerREST(String token){
		this.token = token;
		trelloDateFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	
	public BoardResponse getBoardActions(String boardId, Date since){
		String strSince = TrelloServerREST.dateToTrelloDate(since);
		Log.d("getBoardActions", "Since: " + strSince);
		Log.d("getBoardActions", "Sampl: " + "2014-01-07T22:50:36.556Z");
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		String url = "https://api.trello.com/1/boards/"+boardId+"?fields=dateLastActivity&actions=all&actions_since=" + strSince
			+ "&key=" + TrelloServerREST.devKey + "&token=" + this.token;
        Log.d("getBoardActions", "Request Url:" + url);
		HttpGet httpGet = new HttpGet(url);
		try {
		    HttpResponse response = httpClient.execute(httpGet);
		    String responseString = EntityUtils.toString(response.getEntity());
		    Log.d("getBoardActions", "Response= " + responseString);
		    if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
		        //ParseComServer.ParseComError error = new Gson().fromJson(responseString, ParseComServer.ParseComError.class);
		       // throw new Exception("Error retrieving tv shows ["+error.code+"] - " + error.error);
		    }
		    BoardResponse board = new Gson().fromJson(responseString, BoardResponse.class);
		    return board;
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return new BoardResponse(); //TODO idk about this
	}
	
	public void getBoardCardsAndLists(String boardId){
		//Runs first time to get all the info for this board before running getActions
		
	}
	
	
	public void getOrganizationActions(String orgoId, Date since){
		
	}
	
	public BoardResponse getEverything(String boardId){
		DefaultHttpClient httpClient = new DefaultHttpClient();
		
		String url = "https://api.trello.com/1/boards/"+boardId+"?fields=name,desc,closed,idOrganization,labelNames,dateLastActivity&actions=all&actions_limit=1&cards=open&card_fields=name,desc,idBoard,idList,labels,due,pos&card_attachments=true&card_attachment_fields=bytes,date,name,url&lists=open&lists_fields=name,idBoard,pos&members=all&membersInvited=all"
			+"&key="+ TrelloServerREST.devKey + "&token=" + this.token;
		HttpGet httpGet = new HttpGet(url);
		try {
		    HttpResponse response = httpClient.execute(httpGet);
		    String responseString = EntityUtils.toString(response.getEntity());
		    Log.d("getEverything", "Response= " + responseString);
		    if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
		        //ParseComServer.ParseComError error = new Gson().fromJson(responseString, ParseComServer.ParseComError.class);
		       // throw new Exception("Error retrieving tv shows ["+error.code+"] - " + error.error);
		    }
		    BoardResponse board = new Gson().fromJson(responseString, BoardResponse.class);
		    return board;
		
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		return new BoardResponse(); //TODO idk about this
	}
	
	public List<BoardResponse> getAllBoards(String orgoId) throws Exception {
		//Runs first time to get all boards for organization
        DefaultHttpClient httpClient = new DefaultHttpClient();
        String url = "https://api.trello.com/1/organizations/"+orgoId+"?boards=open&board_fields=name,dateLastActivity"
        		+ "&key=" + TrelloServerREST.devKey + "&token=" + this.token;
        
        Log.d("getAllBoards", "Request Url:" + url);
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d("getAllBoards", "Response= " + responseString);

            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                //ParseComServer.ParseComError error = new Gson().fromJson(responseString, ParseComServer.ParseComError.class);
               // throw new Exception("Error retrieving tv shows ["+error.code+"] - " + error.error);
            }
            OrganizationResponse organization = new Gson().fromJson(responseString, OrganizationResponse.class);
            return organization.boards;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<BoardResponse>();
	}
	
	public String putBoard(TrelloBoard board){
		Log.d("TrelloServerREST - putBoard", "Called");
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("https://api.trello.com/1/boards");
		
		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		results.add(new BasicNameValuePair("key",TrelloServerREST.devKey));
		results.add(new BasicNameValuePair("token",this.token));
		results.add(new BasicNameValuePair("idOrganization",board.getOrganizationId()));
		
		if(board.getName() != null) results.add(new BasicNameValuePair("name", board.getName()));
		if(board.getDesc() != null) results.add(new BasicNameValuePair("desc", board.getDesc()));
		results.add(new BasicNameValuePair("prefs_permissionLevel", "org")); //TODO IDK ABOUT THIS
		results.add(new BasicNameValuePair("prefs_selfJoin", "true")); //TODO IDK added this

		
		String newId = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(results));
			
			try {
				Log.d("putBoard", "url: " + EntityUtils.toString(post.getEntity()));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (UnsupportedEncodingException e) {
			// Auto-generated catch block
			Log.e("putBoard","An error has occurred", e);
		}
		try {
			HttpResponse response = client.execute(post);
			String result = "";
			try {
				//TODO Error here if no Internet
				result = EntityUtils.toString(response.getEntity());
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Log.d("AddBoardToTrello", "Add Response:" + result);
			JSONObject json;
			try {
				json = new JSONObject(result);
				newId = json.getString("id");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			Log.e("AddBoardToTrello","client protocol exception", e);
		} catch (IOException e) {
			Log.e("AddBoardToTrello", "io exception", e);
		}
		
		
		//TODO handle this
		/*
		if(newId != null){
			//Add all members to this board
			SharedPreferences settings = AppContext.getSharedPreferences(PREFS_NAME, 0);
			Set<String> set = settings.getStringSet("organizationMembers", null);
			if(set != null){
				String[] setArray = (String[]) set.toArray(new String[set.size()]);
				for(int i=0; i< setArray.length; i++){
					addMemberToBoard(setArray[i], newId);
				}
			}
		}*/
		return newId; //TODO handle failure
	}
	
	public String putList(TrelloList list){
		Log.d("TrelloServerREST - putList", "Called");
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("https://api.trello.com/1/lists");
		
		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		results.add(new BasicNameValuePair("key",TrelloServerREST.devKey));
		results.add(new BasicNameValuePair("token",this.token));
		results.add(new BasicNameValuePair("idBoard",list.getBoardId()));
		results.add(new BasicNameValuePair("name",list.getName()));
		if(list.getPos() != null) results.add(new BasicNameValuePair("pos", Integer.toString(list.getPos())));

		String newId = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(results));
		} catch (UnsupportedEncodingException e) {
			// Auto-generated catch block
			Log.e("putList","An error has occurred", e);
		}
		try {
			HttpResponse response = client.execute(post);
			String result = "";
			try {
				//TODO Error here if no Internet
				result = EntityUtils.toString(response.getEntity());
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.d("putList", "Add Response:" + result);
			JSONObject json;
			try {
				json = new JSONObject(result);
				newId = json.getString("id");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			Log.e("putList","client protocol exception", e);
		} catch (IOException e) {
			Log.e("putList", "io exception", e);
		}
		return newId; //TODO handle failure
	}
	
	public String putCard(TrelloCard card){
		Log.d("TrelloServerREST - putCard", "Called");
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("https://api.trello.com/1/cards");
		
		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		results.add(new BasicNameValuePair("key",TrelloServerREST.devKey));
		results.add(new BasicNameValuePair("token",this.token));
		results.add(new BasicNameValuePair("idList",card.getListId()));
		results.add(new BasicNameValuePair("name",card.getName()));
		if(card.getDesc() != null) results.add(new BasicNameValuePair("desc",card.getDesc()));
		if(card.getPos() != null) results.add(new BasicNameValuePair("pos", Integer.toString(card.getPos())));
		//TODO labels, due, attachments
		
		String newId = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(results));
		} catch (UnsupportedEncodingException e) {
			// Auto-generated catch block
			Log.e("putCard","An error has occurred", e);
		}
		try {
			HttpResponse response = client.execute(post);
			String result = "";
			try {
				//TODO Error here if no Internet
				result = EntityUtils.toString(response.getEntity());
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.d("putCard", "Add Response:" + result);
			JSONObject json;
			try {
				json = new JSONObject(result);
				newId = json.getString("id");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			Log.e("putCard","client protocol exception", e);
		} catch (IOException e) {
			Log.e("putCard", "io exception", e);
		}
		return newId; //TODO handle failure
	}
	
	public boolean updateBoard(TrelloBoard theBoard){
		Log.d("TrelloServerREST - updateBoard", "Called");
		HttpClient client = new DefaultHttpClient();
		HttpPut put = new HttpPut("https://api.trello.com/1/boards/" + theBoard.getId());

		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		if(theBoard.getName() != null) results.add(new BasicNameValuePair("name", theBoard.getName()));
		if(theBoard.getDesc() != null) results.add(new BasicNameValuePair("desc", theBoard.getDesc()));
		if(theBoard.getOrganizationId() != null) results.add(new BasicNameValuePair("idOrganization", theBoard.getOrganizationId()));
		if(theBoard.getClosed() != null) results.add(new BasicNameValuePair("closed", Boolean.toString(theBoard.getClosed())));
		
		if(results.size() == 0) return true; //No updates to be made
		
		results.add(new BasicNameValuePair("key",TrelloServerREST.devKey));
		results.add(new BasicNameValuePair("token",this.token));
		
		try {
			String result = "";
			try {
				put.setEntity(new UrlEncodedFormEntity(results));
				HttpResponse response = client.execute(put);
				result = EntityUtils.toString(response.getEntity());
			} catch (IllegalStateException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			Log.d("updateBoardOnTrello", "Update Response:" + result);
		} catch (Exception e) {
			Log.e("updateBoardOnTrello","client protocol exception", e);
			return false;
		}
		return true;
	}
	
	public boolean updateList(TrelloList theList){
		Log.d("TrelloServerREST - updateList", "Called");
		HttpClient client = new DefaultHttpClient();
		HttpPut put = new HttpPut("https://api.trello.com/1/lists/" + theList.getId());

		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		if(theList.getName() != null) results.add(new BasicNameValuePair("name", theList.getName()));
		if(theList.getBoardId() != null) results.add(new BasicNameValuePair("idBoard", theList.getBoardId()));
		if(theList.getPos() != null) results.add(new BasicNameValuePair("pos", Integer.toString(theList.getPos())));
		if(theList.getClosed() != null) results.add(new BasicNameValuePair("closed", Boolean.toString(theList.getClosed())));
		
		if(results.size() == 0) return true; //No updates to be made
		
		results.add(new BasicNameValuePair("key",TrelloServerREST.devKey));
		results.add(new BasicNameValuePair("token",this.token));
		
		try {
			String result = "";
			try {
				put.setEntity(new UrlEncodedFormEntity(results));
				HttpResponse response = client.execute(put);
				result = EntityUtils.toString(response.getEntity());
			} catch (IllegalStateException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			Log.d("UpdateListOnTrello", "Update Response:" + result);
		} catch (Exception e) {
			Log.e("UpdateListOnTrello","client protocol exception", e);
			return false;
		}
		return true;
	}
	
	public boolean updateCard(TrelloCard theCard){
		Log.d("TrelloServerREST - updateCard", "Called");
		HttpClient client = new DefaultHttpClient();
		HttpPut put = new HttpPut("https://api.trello.com/1/cards/" + theCard.getId());

		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		
		if(theCard.getName() != null) results.add(new BasicNameValuePair("name", theCard.getName()));
		if(theCard.getDesc() != null) results.add(new BasicNameValuePair("desc", theCard.getDesc()));
		if(theCard.getListId() != null) results.add(new BasicNameValuePair("idList", theCard.getListId()));
		if(theCard.getPos() != null) results.add(new BasicNameValuePair("pos", Integer.toString(theCard.getPos())));
		if(theCard.getClosed() != null) results.add(new BasicNameValuePair("closed", Boolean.toString(theCard.getClosed())));
		//TODO labels, due, attachments
		
		if(results.size() == 0) return true; //No updates to be made

		results.add(new BasicNameValuePair("key",TrelloServerREST.devKey));
		results.add(new BasicNameValuePair("token",this.token));
				
		try {
			String result = "";
			try {
				put.setEntity(new UrlEncodedFormEntity(results));
				HttpResponse response = client.execute(put);
				result = EntityUtils.toString(response.getEntity());
			} catch (IllegalStateException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			Log.d("UpdateCardOnTrello", "Update Response:" + result);
		} catch (Exception e) {
			Log.e("UpdateCardOnTrello","client protocol exception", e);
			return false;
		}
		return true;
	}
	
	
	public static String dateToTrelloDate(Date date){
		if(date == null){
			return "null";
		}
		return TrelloServerREST.trelloDateFormater.format(date);
	}
	public static Date trelloDateToDate(String trelloDate){
		if(trelloDate == null){
			Log.d("TrelloServerREST", "Null trello date, unable to parse");
			return new Date(0);
		}
		Date d;
		try {
			d = TrelloServerREST.trelloDateFormater.parse(trelloDate);
		} catch (java.text.ParseException e) {
			Log.d("TrelloServerREST", "Invaild trello date, unable to parse");
			d = new Date(0);
		}
		return d;
	}
}
