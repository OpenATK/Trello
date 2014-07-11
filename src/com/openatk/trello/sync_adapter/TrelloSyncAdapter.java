/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.openatk.trello.sync_adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.openatk.libtrello.TrelloBoard;
import com.openatk.libtrello.TrelloCard;
import com.openatk.libtrello.TrelloList;
import com.openatk.libtrello.TrelloSyncInfo;
import com.openatk.trello.AppsList;
import com.openatk.trello.authenticator.AccountGeneral;
import com.openatk.trello.authenticator.TrelloMember;
import com.openatk.trello.response.ActionCombiner;
import com.openatk.trello.response.BoardResponse;
import com.openatk.trello.response.CardResponse;
import com.openatk.trello.response.ListResponse;


/**
 * TvShowsSyncAdapter implementation for syncing sample TvShowsSyncAdapter contacts to the
 * platform ContactOperations provider.  This sample shows a basic 2-way
 * sync between the client and a sample server.  It also contains an
 * example of how to update the contacts' status messages, which
 * would be useful for a messaging or social networking client.
 */
public class TrelloSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "TrelloSyncAdapter";
    private static final String AUTHORITY = "com.openatk.trello";
    private final AccountManager mAccountManager;

    private String authToken = null;
    private TrelloServerREST trelloServer = null;
    private String activeOrgo = null;
    
    private TrelloMember activeMember = null;
    
    public TrelloSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
    	//Check if no account provided
		SharedPreferences prefs = this.getContext().getSharedPreferences(AUTHORITY, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    	if(account ==  null){
			String accountName = prefs.getString("accountName", null);
			account = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);
    	}
    	
    	activeMember = new TrelloMember();
    	activeMember.setEmail(prefs.getString("accountEmail", null));
    	activeMember.setFullName(prefs.getString("accountFullName", null));
    	activeMember.setId(prefs.getString("accountId", null));
    	activeMember.setUsername(prefs.getString("accountUsername", null));
 
		Log.d("SyncApp - onPerformSync", "accountEmail" + activeMember.getEmail());
		Log.d("SyncApp - onPerformSync", "accountId" + activeMember.getId());

        // Building a print of the extras we got
        StringBuilder sb = new StringBuilder();
        if (extras != null) {
            for (String key : extras.keySet()) {
                sb.append(key + "[" + extras.get(key) + "] ");
            }
        }
        
        String appPackage = null;
        Long appId = null;
        if(extras.containsKey("appPackage")){
        	appPackage = extras.getString("appPackage");
        }
        if(extras.containsKey("appId")){
        	appId = extras.getLong("appId");
        }
        boolean isAutoSyncRequest = false;
        if(extras.containsKey("isAutoSyncRequest")){
        	isAutoSyncRequest = extras.getBoolean("isAutoSyncRequest", false);
        }

        Log.d("udinic", TAG + "> onPerformSync for account[" + account.name + "]. Extras: "+sb.toString());

    	//Check if autosync is on, if so then make sure the app is in foreground, otherwise turn off periodic interval
        Uri uri = Uri.parse("content://" + appPackage + ".trello.provider/get_sync_info");
    	Cursor cursor2 = null;
    	boolean failed = false;
    	try {
    		cursor2 = this.getContext().getContentResolver().query(uri, null, null, null, null);
    	} catch(Exception e) {
    		failed = true;
    	}
    	
		TrelloSyncInfo syncInfo = null;
    	if(failed == false){
	    	Gson gson = new Gson();
	    	if(cursor2 != null){
	    		while(cursor2.moveToNext()){
	    			//Only 1 item for now
	    			if(cursor2.getColumnCount() > 0 && cursor2.getColumnIndex("json") != -1){
		    			String json = cursor2.getString(cursor2.getColumnIndex("json"));
		    			try {
		    				syncInfo = gson.fromJson(json, TrelloSyncInfo.class);
		    			} catch (Exception e){
		    				Log.d("Failed to convert json to info:", json);
		    			}
	    			}
	    		}
	    		cursor2.close();
	    	}
	    	
	    	Bundle bundle = new Bundle();
	        bundle.putString("appPackage", appPackage);
	        bundle.putBoolean("isAutoSyncRequest", true);
	        
	        boolean turnOff = false;
	    	if(syncInfo == null || syncInfo.getAutoSync() != null && syncInfo.getAutoSync() == false){
	    		//Turn off periodic sync
		        turnOff = true;
	    	}
	    		    	
	    	//Check that app is in foreground if its an autosync request
	    	if(isAutoSyncRequest == true && turnOff == false){
	    		turnOff = true;
				ActivityManager activityManager = (ActivityManager) this.getContext().getSystemService(Context.ACTIVITY_SERVICE);
    	        List<RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
    	        for(int i = 0; i < procInfos.size(); i++) {
    	            if(procInfos.get(i).processName.equals(appPackage) && procInfos.get(i).importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
        	        	Log.d("TrelloSyncAdapter", "App:" + appPackage + " info:" + procInfos.get(i).processName);
    	            	turnOff = false;
    	            	break;
    	            }
    	        }
	    	}
	    	
	    	if(turnOff == false){
	    		Integer interval = 30;
	    		if(syncInfo.getInterval() != null) interval = syncInfo.getInterval();
	    		Log.d("TrelloSyncAdapter", "add autosync, interval:" + Integer.toString(interval));
		        ContentResolver.addPeriodicSync(account, "com.openatk.trello.provider", bundle, interval);
	    	} else {
	    		Log.d("TrelloSyncAdapter", "turn off autosync");
		        ContentResolver.removePeriodicSync(account, "com.openatk.trello.provider", bundle);
	    	}
	    	
	    	//If autosync and not in foreground don't sync
	    	if(isAutoSyncRequest && turnOff) return;
    	}
    	
    	if(isAutoSyncRequest && syncInfo != null && syncInfo.getAutoSync() != null && syncInfo.getAutoSync() == false) return;
    	if(syncInfo != null && syncInfo.getSync() != null && syncInfo.getSync() == false) return;
    	    	
        try {
            // Get the auth token for the current account and
            // the userObjectId, needed for creating items on Parse.com account
            authToken = mAccountManager.blockingGetAuthToken(account, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, true);
            //String userObjectId = mAccountManager.getUserData(account,AccountGeneral.USERDATA_USER_OBJ_ID);
            trelloServer = new TrelloServerREST(authToken);
            
	    	activeOrgo = prefs.getString("organizationId", "");
            Log.d("Active Organization Id:",activeOrgo);
            SyncApp(appPackage, provider);
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            syncResult.stats.numAuthExceptions++;
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public void SyncApp(String app, ContentProviderClient provider) throws Exception{
		//Sync a specific app
		Date newLastSyncDate = new Date(); //TODO pull from internet
		
		//Get list of boards this app uses
    	List<TrelloBoard> localBoards = getLocalBoards(app);
    	Log.d("SyncApp", "Got local boards");
    	
    	//Check if all boards have trelloId's
    	List<TrelloBoard> localBoardsWithIds = new ArrayList<TrelloBoard>();
    	Boolean allHaveTrelloId = true;
    	for(int i=0; i<localBoards.size(); i++){
    		if(localBoards.get(i).getId().length() == 0){
    			allHaveTrelloId = false;
    		} else {
    			localBoardsWithIds.add(localBoards.get(i));
    		}
    	}
    	
    	Log.d("SyncApp", "All have trello id? " + Boolean.toString(allHaveTrelloId));
    	if(allHaveTrelloId == false){
    		//Look for new boards on trello that don't exist locally
    		List<BoardResponse> remoteBoards = trelloServer.getAllBoards(activeOrgo);
        	Log.d("SyncApp", "Download all trello boards");

    		for(int i=0; i<remoteBoards.size(); i++){
				BoardResponse remoteBoard = remoteBoards.get(i);
				//Set all these as open
				remoteBoard.closed = false;
				
				Boolean found = false;
				for(int j=0; j<localBoardsWithIds.size(); j++){
	    			TrelloBoard localBoard = localBoardsWithIds.get(j);
					if(localBoard.getId().contentEquals(remoteBoard.id)){
						found = true;
						break;
					}
				}
				if(found == false){
		        	Log.d("SyncApp", "Sending trello board to local db");
					TrelloBoard newBoard = new TrelloBoard();
					newBoard.setId(remoteBoard.id);
					newBoard.setOrganizationId(remoteBoard.idOrganization);
					newBoard.setName(remoteBoard.name);
					newBoard.setDesc(remoteBoard.desc);
					newBoard.setClosed(remoteBoard.closed);
					newBoard.setLastTrelloActionDate(TrelloServerREST.dateToTrelloDate(new Date(0)));
					newBoard.setLastSyncDate(TrelloServerREST.dateToTrelloDate(new Date(0)));
					//Send board to local as insert
					this.insertLocalBoard(app, newBoard);
				}
    		}
				
			//Get local boards again
    		List<TrelloBoard> newLocalBoards = getLocalBoards(app);
	    	Log.d("SyncApp", "Got local boards AGAIN");
	    	
    		List<TrelloBoard> toAddUserTo = new ArrayList<TrelloBoard>();
    		
	    	Log.d("SyncApp", "Find if we need to add this account to any boards.");
	    	//Find boards that were accepted by the app
	    	for(int i=0; i<localBoards.size(); i++){
	    		TrelloBoard oldBoard = localBoards.get(i);
	    		for(int j=0; j<newLocalBoards.size(); j++){
		    		TrelloBoard newBoard = newLocalBoards.get(j);
		    		if(oldBoard.getLocalId().contentEquals(newBoard.getLocalId())){
		    			//This is our board now see if the app accepted it
		    			if(oldBoard.getId().contentEquals(newBoard.getId()) == false){
		    				//App updated the trello id, so it accepted the inserted board
		    				//Add user to the board on trello
		    				toAddUserTo.add(newBoard);
			    	    	Log.d("SyncApp", "AddUser this board needs account added.");
			    			break;
		    			}
		    		}
	    		}
	    	}
	    	
	    	Log.d("SyncApp", "Adding account to " + Integer.toString(toAddUserTo.size()) + " boards.");
	    	
			//Add user to boards on trello
	    	for(int i=0; i<toAddUserTo.size(); i++){
        		trelloServer.addUserToBoard(toAddUserTo.get(i), activeMember);
	    	}
	    	
	    	localBoards = newLocalBoards;
	    	
	    	//For all w/o trello id
	    	List<TrelloBoard> localBoardsWithoutIds = new ArrayList<TrelloBoard>();
	    	for(int i=0; i<localBoards.size(); i++){
	    		if(localBoards.get(i).getId().length() == 0){
	    			localBoardsWithoutIds.add(localBoards.get(i));
	    		}
	    	}
	    	
	    	Log.d("SyncApp", "Boards Without TrelloId:" + Integer.toString(localBoardsWithoutIds.size()));
	    	//Insert on trello and update local TrelloId
	    	//Do inserts to trello
    		for(int i=0; i<localBoardsWithoutIds.size(); i++){
    			TrelloBoard toAdd = localBoardsWithoutIds.get(i);
    			toAdd.setOrganizationId(activeOrgo);
        		String trelloId = trelloServer.putBoard(toAdd);
    	    	Log.d("SyncApp", "Insert board on trello");
        		if(trelloId != null){
	        		TrelloBoard updateBoard = new TrelloBoard();
					updateBoard.setSource(toAdd);
					updateBoard.setId(trelloId);
					updateBoard.setOrganizationId(activeOrgo);
	        		//Update local database
        	    	Log.d("SyncApp", "Sending new trello board to local db, trello id:" + trelloId);
					this.updateLocalBoard(app, updateBoard);
			    	Log.d("SyncApp", "Getting local boards again");
        		} else {
        			//TODO correct?
        			//Do nothing, just deal with only trelloId boards after this point
        	    	Log.d("SyncApp", "No trello board id returned... ERROR?");
        		}
    		}
    		
    		//Get local boards again
	    	localBoards = getLocalBoards(app);
	    	//Remove any without trelloId, should be like this already unless we had Internet error or concurrency problem
	    	Iterator<TrelloBoard> iter = localBoards.iterator();
	    	while(iter.hasNext()) {
	    		TrelloBoard lBoard = iter.next(); // must be called before you can call i.remove()
	    		if(lBoard.getId().length() == 0){
	    			iter.remove();
	    		}
	    	}
    	}
    	
    	Log.d("SyncApp", "Boards with TrelloIds:" + Integer.toString(localBoards.size()));
    	
    	//For each board with trelloId ie. (localBoards)
		for(int i=0; i<localBoards.size(); i++) {
			TrelloBoard lboard = localBoards.get(i);
			//Get last sync date
			
			List<TrelloList> localLists = getLocalLists(app, lboard.getId());
	    	Log.d("SyncApp", "Number of local lists:" + Integer.toString(localLists.size()));
			
			//First sync?
			//Get last action date
	    	Date lastSyncDate = new Date(0);
	    	Date lastActionDate = new Date(0);
	    	if(lboard.getLastSyncDate() != null){
	    		lastSyncDate = TrelloServerREST.trelloDateToDate(lboard.getLastSyncDate());
	    	}
	    	if(lboard.getLastTrelloActionDate() != null){
	    		lastActionDate = TrelloServerREST.trelloDateToDate(lboard.getLastTrelloActionDate());
	    	}
	    	Date newLastActionDate = lastActionDate;
	    	Log.d("Found App:", "LastSyncDate:" + TrelloServerREST.dateToTrelloDate(lastSyncDate));
    		Log.d("Found App:", "LastActionDate:" + TrelloServerREST.dateToTrelloDate(lastActionDate));
			
            List<TrelloList> trelloLists = new ArrayList<TrelloList>();
            List<TrelloCard> trelloCards = new ArrayList<TrelloCard>();
            TrelloBoard trelloBoard = null;
			if(lastSyncDate.compareTo(new Date(0)) == 0){
				//First sync
		    	Log.d("SyncApp", "First sync");

				//Download all lists/cards from trello
				BoardResponse board = trelloServer.getEverything(lboard.getId());

				//Get newLastActionDate from the action
				if(board.actions.size() == 0){
			    	Log.d("SyncApp", "ERROR ******** Should always have 1 action if syncing everything *********");
				} else {
					newLastActionDate = TrelloServerREST.trelloDateToDate(board.actions.get(0).date);
				}
				
				//Convert to TrelloLists
				List<ListResponse> lists = board.lists;
				for(int j=0; j<lists.size(); j++){
					ListResponse list = lists.get(j);
					TrelloList newList = new TrelloList();
					newList.setId(list.id);
					newList.setBoardId(list.idBoard);
					newList.setClosed(false);
					newList.setName(list.name);
					newList.setPos(Integer.parseInt(list.pos));
					
					//Set all change events to sometime because trello always has change date if it has data
					newList.setBoardId_changed(new Date(0));
					newList.setClosed_changed(new Date(0));
					newList.setName_changed(new Date(0));
					newList.setPos_changed(new Date(0));

					trelloLists.add(newList);
				}
		    	Log.d("SyncApp", Integer.toString(trelloLists.size()) + " lists found on trello");

				
				//Convert to TrelloCards
				List<CardResponse> cards = board.cards;
				for(int j=0; j<cards.size(); j++){
					CardResponse card = cards.get(j);
					TrelloCard newCard = new TrelloCard();
					newCard.setId(card.id);
					newCard.setListId(card.idList);
					newCard.setClosed(false);
					newCard.setName(card.name);
					newCard.setDesc(card.desc);
					newCard.setPos(Integer.parseInt(card.pos));
					//TODO labels
					//TODO due
					//TODO attachments
					
					//Set all dates to sometime because trello always has change date if it has data
					newCard.setListId_changed(new Date(0));
					newCard.setClosed_changed(new Date(0));
					newCard.setName_changed(new Date(0));
					newCard.setDesc_changed(new Date(0));
					newCard.setPos_changed(new Date(0));

					trelloCards.add(newCard);
				}
				
				//Convert to TrelloBoard
				trelloBoard = new TrelloBoard(null);
				trelloBoard.setName(board.name);
				trelloBoard.setDesc(board.desc);
				trelloBoard.setClosed(board.closed);
				trelloBoard.setOrganizationId(board.idOrganization);
				//TODO labels
		    	Log.d("SyncApp", Integer.toString(trelloCards.size()) + " cards found on trello");
			} else {
		    	Log.d("SyncApp", "Not first sync, getting actions");

				//Download all actions since the lastActionDate
				BoardResponse board = trelloServer.getBoardActions(lboard.getId(), lastActionDate);
				ActionCombiner combiner = new ActionCombiner();
				
				//Convert to TrelloLists
				trelloLists = combiner.getLists(board.actions);
		    	Log.d("SyncApp", Integer.toString(trelloLists.size()) + " lists found on trello");
		    	//Convert to TrelloCards
				trelloCards = combiner.getCards(board.actions);
		    	Log.d("SyncApp", Integer.toString(trelloCards.size()) + " cards found on trello");
		    	if(board.actions.size() > 0){
		    		//New actions on trello, update this
		    		newLastActionDate = TrelloServerREST.trelloDateToDate(board.actions.get(0).date); //First action is newest
		    	}
		    	
				//Convert to TrelloBoard
		    	List<TrelloBoard> boards = combiner.getBoards(board.actions);
		    	if(boards.size() > 0) trelloBoard = boards.get(0);
			}
			
			//Has our board changed?
			if(trelloBoard != null){
				Log.d("SyncApp", "Our board has changed or first sync");
				//We have info from trello
				//See if anything is conflicting
				TrelloBoard updateTrello = new TrelloBoard(null);
				TrelloBoard updateLocal = new TrelloBoard(null);
				updateTrello.setId(lboard.getId());
				updateLocal.setSource(lboard);
				if(trelloBoard.getName() != null){
					if(lboard.getName().contentEquals(trelloBoard.getName()) == false){
						//Trello different than local, who is newer?
						if(lboard.getName_changed().after(trelloBoard.getName_changed())){
							//Update trello
							updateTrello.setName(lboard.getName());
						} else {
							//Update local
							updateLocal.setName(trelloBoard.getName());
						}
					}
				}
				if(trelloBoard.getDesc() != null){
					if(lboard.getDesc().contentEquals(trelloBoard.getDesc()) == false){
						//Trello different than local, who is newer?
						if(lboard.getDesc_changed().after(trelloBoard.getDesc_changed())){
							//Update trello
							updateTrello.setDesc(lboard.getDesc());
						} else {
							//Update local
							updateLocal.setDesc(trelloBoard.getDesc());
						}
					}
				}
				if(trelloBoard.getClosed() != null){
					if(lboard.getClosed() != trelloBoard.getClosed()){
						//Trello different than local, who is newer?
						if(lboard.getClosed_changed().after(trelloBoard.getClosed_changed())){
							//Update trello
							updateTrello.setClosed(lboard.getClosed());
						} else {
							//Update local
							updateLocal.setClosed(trelloBoard.getClosed());
						}
					}
				}
				if(trelloBoard.getOrganizationId() != null){
					if(activeOrgo.contentEquals(trelloBoard.getOrganizationId()) == false){
						//Trello different than the one we are using, who is newer? TODO ********
						updateLocal.setOrganizationId(trelloBoard.getOrganizationId());

						/*if(lboard.getOrganizationId_changed().after(trelloBoard.getOrganizationId_changed())){
							//Update trello
							updateTrello.setOrganizationId(lboard.getOrganizationId());
						} else {
							//Update local
							updateLocal.setOrganizationId(trelloBoard.getOrganizationId());
						}*/
					}
				}
				//TODO labels
				this.updateTrelloBoard(updateTrello);
				this.updateLocalBoard(app, updateLocal);
			}
			
			//Look for new lists on trello that don't exist locally
			Boolean hadNewLists = false;
			for(int j=0; j<trelloLists.size(); j++){
				TrelloList tList = trelloLists.get(j);
				Log.d("TrelloSyncAdpater - new trello list?", "tlist id:" + tList.getId());
				Boolean found = false;
				for(int k=0; k<localLists.size(); k++){
					Log.d("TrelloSyncAdpater - new trello list?","local list id:" + localLists.get(k).getId());
					if(tList.getId().contentEquals(localLists.get(k).getId())){
						found = true;
						break;
					}
				}
				//Insert new lists into local database
				if(found == false) {
			    	Log.d("SyncApp", "Sending new trello list to local '" + tList.getName() + "'");
					this.insertLocalList(app, tList);
					hadNewLists = true;
				}
			}			
			
			//Get local lists again
			if(hadNewLists){
				localLists = getLocalLists(app, lboard.getId());
				Log.d("SyncApp", "New number of local lists: " + Integer.toString(localLists.size()));
			}
			
	    	//For each local list with trello id
	    	for(int j=0; j<localLists.size(); j++){
				TrelloList lList = localLists.get(j);
				if(lList.getId().length() > 0){
			    	Log.d("SyncApp", "Local list with trello id");
					//Has Trello Id
			    	//Compare dates with matching trello list and update accordingly
					Integer where = this.findListWithId(trelloLists, lList.getId());
					if(where == null){
						//On just local
				    	Log.d("SyncApp", "Not found on trello");
						//Update trello if since >= lastSyncDate
						//There were no actions on trello for this list but it changed locally
						//Update trello with any changes that have occured after the last sync
						TrelloList updateTrello = new TrelloList(null);
						updateTrello.setId(lList.getId());
						
						//Update Trello if change is after or equal to last sync date
						if(lList.getName_changed() != null && lList.getName_changed().compareTo(lastSyncDate) >= 0) {
					    	Log.d("SyncApp", "Updating trello name");
							updateTrello.setName(lList.getName());
						}
						if(lList.getPos_changed() != null && lList.getPos_changed().compareTo(lastSyncDate) >= 0) {
					    	Log.d("SyncApp", "Updating trello pos");
							updateTrello.setPos(lList.getPos());
						}
						if(lList.getBoardId_changed() != null && lList.getBoardId_changed().compareTo(lastSyncDate) >= 0) {
					    	Log.d("SyncApp", "Updating trello boardId");
							updateTrello.setBoardId(lList.getBoardId());
						}
						if(lList.getBoardId_changed() != null && lList.getClosed_changed().compareTo(lastSyncDate) >= 0) {
					    	Log.d("SyncApp", "Updating trello closed");
							updateTrello.setClosed(lList.getClosed());
						}
						this.updateTrelloList(updateTrello); //TODO delete local after successful close update to trello?
					} else {
						//On both trello and local
				    	Log.d("SyncApp", "Found on trello and on local");
						TrelloList tList = trelloLists.get(where);
						TrelloList updateTrello = new TrelloList(null);
						TrelloList updateLocal = new TrelloList(null);
						updateTrello.setId(lList.getId());
						updateLocal.setSource(lList);
						//Name
				    	Log.d("SyncApp", "Local List name_changed:" + TrelloServerREST.dateToTrelloDate(lList.getName_changed()));
						if(lList.getName() == null && tList.getName() == null){
							Log.d("SyncApp", "Neither have name");
						} else if(tList.getName() == null) {
							Log.d("SyncApp", "Only local name exists");
							if(lList.getName_changed() != null && lList.getName_changed().compareTo(lastSyncDate) >= 0){
						    	Log.d("SyncApp", "Updating trello name");
								updateTrello.setName(lList.getName());
							}
						} else {
							//Compare dates, values and update accordingly
							if(lList.getName().contentEquals(tList.getName()) == false){
						    	Log.d("SyncApp", "Trello List name_changed:" + TrelloServerREST.dateToTrelloDate(tList.getName_changed()));
								if(lList.getName_changed() == null) Log.w("SyncApp", "Local list getName_changed field is null.");
								if(lList.getName_changed() != null && lList.getName_changed().after(tList.getName_changed())) {
							    	Log.d("SyncApp", "Updating trello name");
									updateTrello.setName(lList.getName());
								} else {
							    	Log.d("SyncApp", "Updating local name");
									updateLocal.setName(tList.getName());
								}
							} else {
						    	Log.d("SyncApp", "Names same, not updating.");
							}
						}
						//Pos
				    	Log.d("SyncApp", "Local List pos_changed:" + TrelloServerREST.dateToTrelloDate(lList.getPos_changed()));
				    	if(lList.getPos() == null && tList.getPos() == null){
							Log.d("SyncApp", "Neither have pos");
						} else if(tList.getPos() == null) {
							Log.d("SyncApp", "Only local pos exists");
							if(lList.getPos_changed() != null && lList.getPos_changed().compareTo(lastSyncDate) >= 0) updateTrello.setPos(lList.getPos());
						} else {
							//Compare dates and update accordingly
							if(lList.getPos() != tList.getPos()){
						    	Log.d("SyncApp", "Trello List pos_changed:" + TrelloServerREST.dateToTrelloDate(tList.getPos_changed()));
								if(lList.getPos_changed() == null) Log.w("SyncApp", "Local list getPos_changed field is null.");
								if(lList.getPos_changed() != null && lList.getPos_changed().after(tList.getPos_changed())) {
							    	Log.d("SyncApp", "Updating trello pos");
									updateTrello.setPos(lList.getPos());
								} else {
							    	Log.d("SyncApp", "Updating local pos");
									updateLocal.setPos(tList.getPos());
								}
							} else {
						    	Log.d("SyncApp", "Pos same, not updating.");
							}
						}
						//BoardId
				    	Log.d("SyncApp", "Local List boardId_changed:" + TrelloServerREST.dateToTrelloDate(lList.getBoardId_changed()));
				    	if(lList.getBoardId() == null && tList.getBoardId() == null){
							Log.d("SyncApp", "Neither have board id");
						} else if(tList.getBoardId() == null) {
							Log.d("SyncApp", "Only local boardId exists");
							if(lList.getBoardId_changed() != null && lList.getBoardId_changed().compareTo(lastSyncDate) >= 0) updateTrello.setBoardId(lList.getBoardId());
						} else {
							//Compare dates and update accordingly
							if(lList.getBoardId().contentEquals(tList.getBoardId()) == false){
						    	Log.d("SyncApp", "Trello List boardId_changed:" + TrelloServerREST.dateToTrelloDate(tList.getBoardId_changed()));
								if(lList.getBoardId_changed() == null) Log.w("SyncApp", "Local list getBoardId_changed field is null.");
								if(lList.getBoardId_changed() != null && lList.getBoardId_changed().after(tList.getBoardId_changed())) {
							    	Log.d("SyncApp", "Updating trello boardId");
									updateTrello.setBoardId(lList.getBoardId());
								} else {
							    	Log.d("SyncApp", "Updating local boardId");
									updateLocal.setBoardId(tList.getBoardId());
								}
							} else {
						    	Log.d("SyncApp", "BoardId same, not updating.");
							}
						}
						//Closed
				    	Log.d("SyncApp", "Local List closed_changed:" + TrelloServerREST.dateToTrelloDate(lList.getClosed_changed()));
				    	if(lList.getClosed() == null && tList.getClosed() == null){
							Log.d("SyncApp", "Neither have closed");
						} else if(tList.getClosed() == null){
							Log.d("SyncApp", "Only local closed exists");
							if(lList.getClosed_changed() != null && lList.getClosed_changed().compareTo(lastSyncDate) >= 0) updateTrello.setClosed(lList.getClosed());
						} else {
							if(lList.getClosed() != tList.getClosed()){
						    	Log.d("SyncApp", "Trello List closed_changed:" + TrelloServerREST.dateToTrelloDate(tList.getClosed_changed()));
						    	//Compare dates and update accordingly
								if(lList.getClosed_changed() == null) Log.w("SyncApp", "Local list getClosed_changed field is null.");
								if(lList.getClosed_changed() != null && lList.getClosed_changed().after(tList.getClosed_changed())) {
							    	Log.d("SyncApp", "Updating remote closed");
									updateTrello.setClosed(lList.getClosed());
								} else {
							    	Log.d("SyncApp", "Updating local closed");
									updateLocal.setClosed(tList.getClosed());
								}
							} else {
						    	Log.d("SyncApp", "Closed same, not updating.");
							}
						}
						this.updateTrelloList(updateTrello);
						this.updateLocalList(app, updateLocal);
					}
				}
	    	}
	    	
	    	//For each list w/o trello id
	    	for(int j=0; j<localLists.size(); j++){
				TrelloList lList = localLists.get(j);
				if(lList.getId().length() == 0){
			    	Log.d("SyncApp", "Local list without trello id, inserting on trello");
					String trelloId = trelloServer.putList(lList);
	        		if(trelloId != null){
		        		TrelloList updateList = new TrelloList(null);
		        		updateList.setSource(lList);
		        		updateList.setId(trelloId);
		        		//Update local database
						this.updateLocalList(app, updateList);
	        		} else {
	        			//TODO
	        			//Need to set error flag
				    	Log.d("SyncApp", "Failed to insert on trello ERROR? ************");
	        		}
				}
				//TODO if update need to wait a few seconds for it to occur by other process????
	    	}
	    	
	    	//Get local cards
	    	List<TrelloCard> localCards = getLocalCards(app, lboard.getId());
	    	Log.d("SyncApp", "Get local cards, # of cards:" + Integer.toString(localCards.size()));

	    	//Look for new cards on trello that don't exist locally
	    	Boolean hadNewCards = false;
			for(int j=0; j<trelloCards.size(); j++){
				TrelloCard tCard = trelloCards.get(j);
				Boolean found = false;
				for(int k=0; k<localCards.size(); k++){
					if(tCard.getId().contentEquals(localCards.get(k).getId())){
						found = true;
						break;
					}
				}
				//Insert new cards into local database
				if(found == false){
					hadNewCards = true;
					this.insertLocalCard(app, tCard);
			    	Log.d("SyncApp", "Found trello card, inserting into local: " + tCard.getName());
				}
			}			
			
			if(hadNewCards){
				//Get local cards again
				localCards = getLocalCards(app, lboard.getId());
		    	Log.d("SyncApp", "Get local cards AGAIN, # of cards: " + Integer.toString(localCards.size()));
			}

			//Remove any cards that aren't attached to a trello list
			Iterator<TrelloCard> iter = localCards.iterator();
	    	while(iter.hasNext()) {
	    		TrelloCard lCard = iter.next(); // must be called before you can call i.remove()
	    		if(lCard.getListId() == null || lCard.getListId().length() == 0){
			    	Log.d("SyncApp", "Removing card without trello id.");
	    			iter.remove();
	    		}
	    	}
			
	    	//For each local card
	    	for(int j=0; j<localCards.size(); j++){
	    		TrelloCard lCard = localCards.get(j);
				if(lCard.getId().length() > 0){
					//Has Trello Id
			    	//Compare dates with matching trello card and update accordingly
			    	Log.d("SyncApp", "Local card has Trello Id");
					Integer where = this.findCardWithId(trelloCards, lCard.getId());
					if(where == null){
						//On just local
				    	Log.d("SyncApp", "Did not find local card on trello, update trello if neccessary");
						//Update trello
						//There were no actions on trello for this card but it changed locally
						//Update trello with any changes that have occurred after the lastSyncDate 
						TrelloCard updateTrello = new TrelloCard(null);
						updateTrello.setId(lCard.getId());
						
						if(lCard.getName_changed() != null && lCard.getName_changed().compareTo(lastSyncDate) >= 0) {
							Log.d("SyncApp", "Updating Trello Card Name");
							updateTrello.setName(lCard.getName());
						}
						if(lCard.getDesc_changed() != null && lCard.getDesc_changed().compareTo(lastSyncDate) >= 0){
							Log.d("SyncApp", "Updating Trello Card Desc");
							updateTrello.setDesc(lCard.getDesc());
						}
						if(lCard.getPos_changed() != null && lCard.getPos_changed().compareTo(lastSyncDate) >= 0){
							Log.d("SyncApp", "Updating Trello Card Pos");
							updateTrello.setPos(lCard.getPos());
						}
						if(lCard.getListId_changed() != null && lCard.getListId_changed().compareTo(lastSyncDate) >= 0) {
							Log.d("SyncApp", "Updating Trello Card ListId");
							updateTrello.setListId(lCard.getListId());
						}
						if(lCard.getBoardId_changed() != null && lCard.getBoardId_changed().compareTo(lastSyncDate) >= 0){
							Log.d("SyncApp", "Updating Trello Card BoardId");
							updateTrello.setBoardId(lCard.getBoardId());
						}
						if(lCard.getClosed_changed() != null && lCard.getClosed_changed().compareTo(lastSyncDate) >= 0){
							Log.d("SyncApp", "Updating Trello Card Closed");
							updateTrello.setClosed(lCard.getClosed());
						}
						//TODO labels, due
						this.updateTrelloCard(updateTrello); //TODO delete local after successful close update to trello?
					} else {
						//On both local and trello
				    	Log.d("SyncApp", "Card found on both trello and local, comparing values and dates");
						TrelloCard tCard = trelloCards.get(where);
						TrelloCard updateTrello = new TrelloCard(null);
						TrelloCard updateLocal = new TrelloCard(null);
						updateTrello.setId(lCard.getId());
						updateLocal.setSource(lCard);
						//Name
						if(lCard.getName() == null && tCard.getName() == null){
					    	Log.d("SyncApp", "Neither have name.");
						} else if(tCard.getName() == null) {
					    	Log.d("SyncApp", "Only local name exists.");
							if(lCard.getName_changed() != null && lCard.getName_changed().compareTo(lastSyncDate) >= 0) updateTrello.setName(lCard.getName());
						} else {
							//Compare dates and update accordingly
							if(lCard.getName().contentEquals(tCard.getName()) == false){
								Log.d("SyncApp", "Names are different ie. local:" + lCard.getName()+ " trello:" + tCard.getName());
								if(lCard.getName_changed() == null) Log.w("SyncApp", "Local card's name_changed field is null.");
								if(lCard.getName_changed() != null && lCard.getName_changed().after(tCard.getName_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setName(lCard.getName());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setName(tCard.getName());
								}
							}
						}
						//Desc
						if(lCard.getDesc() == null && tCard.getDesc() == null){
					    	Log.d("SyncApp", "Neither have desc.");
						} else if(tCard.getDesc() == null) {
					    	Log.d("SyncApp", "Only local desc exists.");
							if(lCard.getDesc_changed() != null && lCard.getDesc_changed().compareTo(lastSyncDate) >= 0) updateTrello.setDesc(lCard.getDesc());
						} else {
							//Compare dates and update accordingly, if local is null then that app doesn't care about this field
							if(lCard.getDesc() != null && lCard.getDesc().contentEquals(tCard.getDesc()) == false){
								Log.d("SyncApp", "Descs are different");
								if(lCard.getDesc_changed() == null) Log.w("SyncApp", "Local card's desc_changed field is null.");
								if(lCard.getDesc_changed() != null && lCard.getDesc_changed().after(tCard.getDesc_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setDesc(lCard.getDesc());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setDesc(tCard.getDesc());
								}
							}
						}
						//Pos
						if(lCard.getPos() == null && tCard.getPos() == null){
					    	Log.d("SyncApp", "Neither have pos.");
						} else if(tCard.getPos() == null) {
							Log.d("SyncApp", "Only local pos exists.");
							if(lCard.getPos_changed() != null && lCard.getPos_changed().compareTo(lastSyncDate) >= 0) updateTrello.setPos(lCard.getPos());
						} else {
							//Compare dates and update accordingly, if local is null we that app doesn't care about this field
							if(lCard.getPos() != null && lCard.getPos() != tCard.getPos()){
								Log.d("SyncApp", "Pos are different");
								if(lCard.getPos_changed() == null) Log.w("SyncApp", "Local card's pos_changed field is null.");
								if(lCard.getPos_changed() != null && lCard.getPos_changed().after(tCard.getPos_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setPos(lCard.getPos());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setPos(tCard.getPos());
								}
							}
						}
						//ListId
						if(lCard.getListId() == null && tCard.getListId() == null){
					    	Log.d("SyncApp", "Neither have listid.");
						} else if(tCard.getListId() == null) {
							Log.d("SyncApp", "Only local listid exists.");
							if(lCard.getListId_changed() != null && lCard.getListId_changed().compareTo(lastSyncDate) >= 0) updateTrello.setListId(lCard.getListId());
						} else {
							//Compare dates and update accordingly
							if(lCard.getListId().contentEquals(tCard.getListId()) == false){
								Log.d("SyncApp", "ListId's are different");
								if(lCard.getListId_changed() == null) Log.w("SyncApp", "Local card's listid_changed field is null.");
								if(lCard.getListId_changed() != null && lCard.getListId_changed().after(tCard.getListId_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setListId(lCard.getListId());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setListId(tCard.getListId());
								}
							}
						}
						//BoardId
						if(lCard.getBoardId() == null && tCard.getBoardId() == null){
					    	Log.d("SyncApp", "Neither have boardid.");
						} else if(tCard.getBoardId() == null) {
							Log.d("SyncApp", "Only local boardid exists.");
							if(lCard.getBoardId_changed() != null && lCard.getBoardId_changed().compareTo(lastSyncDate) >= 0) updateTrello.setBoardId(lCard.getBoardId());
						} else {
							//Compare dates and update accordingly
							if(lCard.getBoardId().contentEquals(tCard.getBoardId()) == false){
								Log.d("SyncApp", "BoardId's are different");
								if(lCard.getBoardId_changed() == null) Log.w("SyncApp", "Local card's boardid_changed field is null.");
								if(lCard.getBoardId_changed() != null && lCard.getBoardId_changed().after(tCard.getBoardId_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setBoardId(lCard.getBoardId());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setBoardId(tCard.getBoardId());
								}
							}
						}
						//Closed
						if(lCard.getClosed() == null && tCard.getClosed() == null){
					    	Log.d("SyncApp", "Neither have closed.");
						} else if(tCard.getClosed() == null) {
							Log.d("SyncApp", "Only local closed exists.");
							if(lCard.getClosed_changed() != null && lCard.getClosed_changed().compareTo(lastSyncDate) >= 0) updateTrello.setClosed(lCard.getClosed());
						} else {
							//Compare dates and update accordingly
							if(lCard.getClosed() != tCard.getClosed()){
								Log.d("SyncApp", "Closeds are different");
								if(lCard.getClosed_changed() == null) Log.w("SyncApp", "Local card's closed_changed field is null.");
								if(lCard.getClosed_changed() != null && lCard.getClosed_changed().after(tCard.getClosed_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setClosed(lCard.getClosed());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setClosed(tCard.getClosed());
								}
							}
						}
						this.updateTrelloCard(updateTrello);
						this.updateLocalCard(app, updateLocal);
					}
				} else {
					//Notice this is done async this time...
					//Local doesn't have a trello id, insert to trello
			    	Log.d("SyncApp", "Inserting local card into trello");
					String trelloId = trelloServer.putCard(lCard);
	        		if(trelloId != null){
		        		TrelloCard updateCard = new TrelloCard(null);
		        		updateCard.setSource(lCard);
		        		updateCard.setId(trelloId);
		        		//Update local database
						this.updateLocalCard(app, updateCard);
				    	Log.d("SyncApp", "Inserting of card worked, trelloId:" + trelloId);
	        		} else {
	        			//TODO
	        			//Set error flag to redo sync, ie. not update lastSyncDate and lastActionDate
				    	Log.d("SyncApp", "Insert of card failed.... ERROR? ******************");
	        		}
				}
	    	}
			
	    	//TODO don't update if sync failed at any point
	    	//Update lastTrelloActionDate on local for this board
			//Update lastSyncDate on local for this board
	    	TrelloBoard updateBoard = new TrelloBoard();
	    	updateBoard.setSource(lboard);
	    	updateBoard.setLastTrelloActionDate(TrelloServerREST.dateToTrelloDate(newLastActionDate));
	    	updateBoard.setLastSyncDate(TrelloServerREST.dateToTrelloDate(newLastSyncDate));
			this.updateLocalBoard(app, updateBoard);
			this.updateLocalSyncDate(app, newLastSyncDate);
		} //End local boards loop
	}
    
    
    
    private boolean updateTrelloBoard(TrelloBoard tBoard){
    	return trelloServer.updateBoard(tBoard);
    }
    
    private boolean updateTrelloList(TrelloList tList){
    	return trelloServer.updateList(tList);
    }
    
    private boolean updateTrelloCard(TrelloCard tCard){
    	return trelloServer.updateCard(tCard);
    }
    
    private List<TrelloBoard> getLocalBoards(String app){
    	Uri trello_boards = Uri.parse("content://" + app + ".trello.provider/boards");
    	Cursor cursor = this.getContext().getContentResolver().query(trello_boards, null, null, null, null);
    	Gson gson = new Gson();
		List<TrelloBoard> localBoards = new ArrayList<TrelloBoard>();
    	if(cursor != null){
    		while(cursor.moveToNext()){
    			String json = cursor.getString(cursor.getColumnIndex("json"));
    			try {
    				TrelloBoard board = gson.fromJson(json, TrelloBoard.class);
    				localBoards.add(board);
    			} catch (Exception e){
    				Log.d("Failed to convert json to board:", json);
    			}
    		}
    		cursor.close();
    	}
    	return localBoards;
    }
    private List<TrelloList> getLocalLists(String app, String boardTrelloId){
    	Uri uri = Uri.parse("content://" + app + ".trello.provider/lists");
    	Cursor cursor;
    	cursor = this.getContext().getContentResolver().query(uri, null, boardTrelloId, null, null);
    	Gson gson = new Gson();
		List<TrelloList> localObjects = new ArrayList<TrelloList>();
    	if(cursor != null){
    		while(cursor.moveToNext()){
    			String json = cursor.getString(cursor.getColumnIndex("json"));
    			try {
    				TrelloList object = gson.fromJson(json, TrelloList.class);
    				localObjects.add(object);
    			} catch (Exception e){
    				Log.d("Failed to convert json to list:", json);
    			}
    		}
    		cursor.close();
    	}
    	return localObjects;
    }
    
    private List<TrelloCard> getLocalCards(String app, String boardTrelloId){
    	Uri uri = Uri.parse("content://" + app + ".trello.provider/cards");
    	
    	Cursor cursor = this.getContext().getContentResolver().query(uri, null, boardTrelloId, null, null);
    	Gson gson = new Gson();
		List<TrelloCard> localObjects = new ArrayList<TrelloCard>();
    	if(cursor != null){
    		while(cursor.moveToNext()){
    			String json = cursor.getString(cursor.getColumnIndex("json"));
    			try {
    				TrelloCard object = gson.fromJson(json, TrelloCard.class);
    				localObjects.add(object);
    			} catch (Exception e){
    				Log.d("Failed to convert json to card:", json);
    			}
    		}
    		cursor.close();
    	}
    	return localObjects;
    }
    
    private void updateLocalBoard(String app, TrelloBoard board){
    	//TODO only do if not all null?
    	ContentValues values = new ContentValues();
		Gson gson = new Gson();
		String json = gson.toJson(board);
    	values.put("json", json);
		Uri uri = Uri.parse("content://" + app + ".trello.provider/board");
    	this.getContext().getContentResolver().update(uri, values, null, null);  
    }
    
    private void updateLocalSyncDate(String app, Date date){
    	ContentValues values = new ContentValues();
		Gson gson = new Gson();
		TrelloSyncInfo newInfo = new TrelloSyncInfo();
		newInfo.setLastSync(date);
		String json = gson.toJson(newInfo);
    	values.put("json", json);
		Uri uri = Uri.parse("content://" + app + ".trello.provider/set_sync_info");
    	this.getContext().getContentResolver().update(uri, values, null, null);  
    }
    
    private void updateLocalList(String app, TrelloList list){
    	//TODO only do if not all null? Why are the date of changes not null? Somehow created for update when shouldn't be there
		ContentValues values = new ContentValues();
		Gson gson = new Gson();
		String json = gson.toJson(list);
		if(json == null){
			Log.d("updateLocalList", "json list is null");
		}
		Log.d("updateLocalList", "json list:" + json);
    	values.put("json", json);
		Uri uri = Uri.parse("content://" + app + ".trello.provider/list");
    	this.getContext().getContentResolver().update(uri, values, null, null);  
    }
    
    private void updateLocalCard(String app, TrelloCard card){
    	if(card.getClosed() != null || card.getBoardId() != null || card.getDesc() != null || card.getLabels() != null || card.getListId() != null || card.getId() != null || card.getName() != null || card.getPos() != null){
			ContentValues values = new ContentValues();
			Gson gson = new Gson();
			String json = gson.toJson(card);
	    	values.put("json", json);
			Uri uri = Uri.parse("content://" + app + ".trello.provider/card");
	    	this.getContext().getContentResolver().update(uri, values, null, null);   
    	} else {
    		Log.d("updateLocalCard", "nothing changed, not updating.");
    	}
    }
    
    private void insertLocalBoard(String app, TrelloBoard board){
    	ContentValues values = new ContentValues();
		Gson gson = new Gson();
		String json = gson.toJson(board);
    	values.put("json", json);
		Uri uri = Uri.parse("content://" + app + ".trello.provider/board");
    	this.getContext().getContentResolver().insert(uri, values);  
    }
    
    private void insertLocalList(String app, TrelloList list){
		ContentValues values = new ContentValues();
		Gson gson = new Gson();
		String json = gson.toJson(list);
    	values.put("json", json);
		Uri uri = Uri.parse("content://" + app + ".trello.provider/list");
    	this.getContext().getContentResolver().insert(uri, values);
    }
    
    private void insertLocalCard(String app, TrelloCard card){
		ContentValues values = new ContentValues();
		Gson gson = new Gson();
		String json = gson.toJson(card);
    	values.put("json", json);
		Uri uri = Uri.parse("content://" + app + ".trello.provider/card");
    	this.getContext().getContentResolver().insert(uri, values);        	
    }
    
    
    
    //TODO Duplicate from ActionCombiner
    private Integer findBoardWithId(List<TrelloBoard> objects, String id){
		for(int i=0; i<objects.size(); i++){
			if(objects.get(i).getId().contentEquals(id)){
				return i;
			}
		}
		return null;
	}
   //TODO Duplicate from ActionCombiner
	private Integer findListWithId(List<TrelloList> objects, String id){
		for(int i=0; i<objects.size(); i++){
			if(objects.get(i).getId().contentEquals(id)){
				return i;
			}
		}
		return null;
	}
	//TODO Duplicate from ActionCombiner
	private Integer findCardWithId(List<TrelloCard> objects, String id){
		for(int i=0; i<objects.size(); i++){
			if(objects.get(i).getId().contentEquals(id)){
				return i;
			}
		}
		return null;
	}
}

