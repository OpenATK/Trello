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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.openatk.trello.authenticator.AccountGeneral;
import com.openatk.trello.database.AppsTable;
import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.provider.SyncProvider;
import com.openatk.trello.response.ActionCombiner;
import com.openatk.trello.response.BoardResponse;
import com.openatk.trello.response.CardResponse;
import com.openatk.trello.response.ListResponse;
import com.openatk.trello.shared.TrelloBoard;
import com.openatk.trello.shared.TrelloCard;
import com.openatk.trello.shared.TrelloList;

/**
 * TvShowsSyncAdapter implementation for syncing sample TvShowsSyncAdapter contacts to the
 * platform ContactOperations provider.  This sample shows a basic 2-way
 * sync between the client and a sample server.  It also contains an
 * example of how to update the contacts' status messages, which
 * would be useful for a messaging or social networking client.
 */
public class TrelloSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "TrelloSyncAdapter";

    private final AccountManager mAccountManager;

    private String authToken = null;
    private TrelloServerREST trelloServer = null;
    private String activeOrgo = null;
    
    public TrelloSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {

    	
    	//Check if no account provided
    	if(account ==  null){
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
			String accountName = prefs.getString("accountName", null);
			account = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);
    	}
    	
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

        Log.d("udinic", TAG + "> onPerformSync for account[" + account.name + "]. Extras: "+sb.toString());

        try {
            // Get the auth token for the current account and
            // the userObjectId, needed for creating items on Parse.com account
            authToken = mAccountManager.blockingGetAuthToken(account, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, true);
            //String userObjectId = mAccountManager.getUserData(account,AccountGeneral.USERDATA_USER_OBJ_ID);
            trelloServer = new TrelloServerREST(authToken);
            
             Cursor cursor = provider.query(SyncProvider.CONTENT_URI_ACTIVE_ORGANIZATION, null, null, null, null);
             if (cursor != null) {
            	 if(cursor.moveToFirst()){
                	 activeOrgo = cursor.getString(0);
                	 Log.d("Active Organization Id:",activeOrgo);
            	 }
                 cursor.close();
             }
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
		Uri appUri = Uri.parse(SyncProvider.CONTENT_URI.toString() + "/" + app);
		Cursor cursor = provider.query(appUri, null, null, null, null);
		
        if (cursor != null) {
        	 if(cursor.moveToFirst()){
        		 String appName = cursor.getString(cursor.getColumnIndex(AppsTable.COL_NAME));
        		 Log.d("Found App:", appName);
        	 }
             cursor.close();
        }

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
				Boolean found = false;
				for(int j=0; j<localBoardsWithIds.size(); j++){
	    			TrelloBoard localBoard = localBoards.get(j);
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
	    	localBoards = getLocalBoards(app);
	    	Log.d("SyncApp", "Got local boards AGAIN");

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
					updateBoard.setLocalId(toAdd.getLocalId());
					updateBoard.setId(trelloId);
					updateBoard.setOrganizationId(activeOrgo);
	        		//Update local database
        	    	Log.d("SyncApp", "New trello board id:" + trelloId);
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
				updateLocal.setId(lboard.getId());
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
				Boolean found = false;
				for(int k=0; k<localLists.size(); k++){
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
						if(lList.getName_changed().compareTo(lastSyncDate) >= 0) {
					    	Log.d("SyncApp", "Updating trello name");
							updateTrello.setName(lList.getName());
						}
						if(lList.getPos_changed().compareTo(lastSyncDate) >= 0) {
					    	Log.d("SyncApp", "Updating trello pos");
							updateTrello.setPos(lList.getPos());
						}
						if(lList.getBoardId_changed().compareTo(lastSyncDate) >= 0) {
					    	Log.d("SyncApp", "Updating trello boardId");
							updateTrello.setBoardId(lList.getBoardId());
						}
						if(lList.getBoardId_changed().compareTo(lastSyncDate) >= 0) {
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
						updateLocal.setId(lList.getId());
						//Name
				    	Log.d("SyncApp", "Local List name_changed:" + TrelloServerREST.dateToTrelloDate(lList.getName_changed()));
						if(tList.getName() == null) {
							Log.d("SyncApp", "Only local name exists");
							if(lList.getName_changed().compareTo(lastSyncDate) >= 0){
						    	Log.d("SyncApp", "Updating trello name");
								updateTrello.setName(lList.getName());
							}
						} else {
							//Compare dates, values and update accordingly
							if(lList.getName().contentEquals(tList.getName()) == false){
						    	Log.d("SyncApp", "Trello List name_changed:" + TrelloServerREST.dateToTrelloDate(tList.getName_changed()));
								if(lList.getName_changed().after(tList.getName_changed())) {
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
						if(tList.getPos() == null) {
							Log.d("SyncApp", "Only local pos exists");
							if(lList.getPos_changed().compareTo(lastSyncDate) >= 0) updateTrello.setPos(lList.getPos());
						} else {
							//Compare dates and update accordingly
							if(lList.getPos() != tList.getPos()){
						    	Log.d("SyncApp", "Trello List pos_changed:" + TrelloServerREST.dateToTrelloDate(tList.getPos_changed()));
								if(lList.getPos_changed().after(tList.getPos_changed())) {
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
						if(tList.getBoardId() == null) {
							Log.d("SyncApp", "Only local boardId exists");
							if(lList.getBoardId_changed().compareTo(lastSyncDate) >= 0) updateTrello.setBoardId(lList.getBoardId());
						} else {
							//Compare dates and update accordingly
							if(lList.getBoardId().contentEquals(tList.getBoardId()) == false){
						    	Log.d("SyncApp", "Trello List boardId_changed:" + TrelloServerREST.dateToTrelloDate(tList.getBoardId_changed()));
								if(lList.getBoardId_changed().after(tList.getBoardId_changed())) {
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
						if(tList.getClosed() == null){
							Log.d("SyncApp", "Only local closed exists");
							if(lList.getClosed_changed().compareTo(lastSyncDate) >= 0) updateTrello.setClosed(lList.getClosed());
						} else {
							if(lList.getClosed() != tList.getClosed()){
						    	Log.d("SyncApp", "Trello List closed_changed:" + TrelloServerREST.dateToTrelloDate(tList.getClosed_changed()));
						    	//Compare dates and update accordingly
								if(lList.getClosed_changed().after(tList.getClosed_changed())) {
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
		        		updateList.setLocalId(lList.getLocalId());
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
	    		if(lCard.getListId().length() == 0){
	    			iter.remove();
	    		}
	    	}
			
	    	//For each local card
	    	for(int j=0; j<localCards.size(); j++){
	    		TrelloCard lCard = localCards.get(j);
				if(lCard.getId().length() > 0){
					//Has Trello Id
			    	//Compare dates with matching trello card and update accordingly
					Integer where = this.findCardWithId(trelloCards, lCard.getId());
					if(where == null){
						//On just local
				    	Log.d("SyncApp", "Did not find local card on trello, update trello if neccessary");
						//Update trello
						//There were no actions on trello for this card but it changed locally
						//Update trello with any changes that have occurred after the lastSyncDate 
						TrelloCard updateTrello = new TrelloCard(null);
						updateTrello.setId(lCard.getId());
						
						if(lCard.getName_changed().compareTo(lastSyncDate) >= 0) {
							Log.d("SyncApp", "Updating Trello Card Name");
							updateTrello.setName(lCard.getName());
						}
						if(lCard.getDesc_changed().compareTo(lastSyncDate) >= 0){
							Log.d("SyncApp", "Updating Trello Card Desc");
							updateTrello.setDesc(lCard.getDesc());
						}
						if(lCard.getPos_changed().compareTo(lastSyncDate) >= 0){
							Log.d("SyncApp", "Updating Trello Card Pos");
							updateTrello.setPos(lCard.getPos());
						}
						if(lCard.getListId_changed().compareTo(lastSyncDate) >= 0) {
							Log.d("SyncApp", "Updating Trello Card ListId");
							updateTrello.setListId(lCard.getListId());
						}
						if(lCard.getBoardId_changed().compareTo(lastSyncDate) >= 0){
							Log.d("SyncApp", "Updating Trello Card BoardId");
							updateTrello.setBoardId(lCard.getBoardId());
						}
						if(lCard.getClosed_changed().compareTo(lastSyncDate) >= 0){
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
						updateLocal.setId(lCard.getId());
						//Name
						if(tCard.getName() == null) {
							if(lCard.getName_changed().compareTo(lastSyncDate) >= 0) updateTrello.setName(lCard.getName());
						} else {
							//Compare dates and update accordingly
							if(lCard.getName().contentEquals(tCard.getName()) == false){
								Log.d("SyncApp", "Names are different");
								if(lCard.getName_changed().after(tCard.getName_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setName(lCard.getName());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setName(tCard.getName());
								}
							}
						}
						//Desc
						if(tCard.getDesc() == null) {
							if(lCard.getDesc_changed().compareTo(lastSyncDate) >= 0) updateTrello.setDesc(lCard.getDesc());
						} else {
							//Compare dates and update accordingly, if local is null we that app doesn't care about this field
							if(lCard.getDesc() != null && lCard.getDesc().contentEquals(tCard.getDesc()) == false){
								Log.d("SyncApp", "Descs are different");
								if(lCard.getDesc_changed().after(tCard.getDesc_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setDesc(lCard.getDesc());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setDesc(tCard.getDesc());
								}
							}
						}
						//Pos
						if(tCard.getPos() == null) {
							if(lCard.getPos_changed().compareTo(lastSyncDate) >= 0) updateTrello.setPos(lCard.getPos());
						} else {
							//Compare dates and update accordingly, if local is null we that app doesn't care about this field
							if(lCard.getPos() != null && lCard.getPos() != tCard.getPos()){
								Log.d("SyncApp", "Pos are different");
								if(lCard.getPos_changed().after(tCard.getPos_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setPos(lCard.getPos());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setPos(tCard.getPos());
								}
							}
						}
						//ListId
						if(tCard.getListId() == null) {
							if(lCard.getListId_changed().compareTo(lastSyncDate) >= 0) updateTrello.setListId(lCard.getListId());
						} else {
							//Compare dates and update accordingly
							if(lCard.getListId().contentEquals(tCard.getListId()) == false){
								Log.d("SyncApp", "ListId's are different");
								if(lCard.getListId_changed().after(tCard.getListId_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setListId(lCard.getListId());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setListId(tCard.getListId());
								}
							}
						}
						//BoardId
						if(tCard.getBoardId() == null) {
							if(lCard.getBoardId_changed().compareTo(lastSyncDate) >= 0) updateTrello.setBoardId(lCard.getBoardId());
						} else {
							//Compare dates and update accordingly
							if(lCard.getBoardId().contentEquals(tCard.getBoardId()) == false){
								Log.d("SyncApp", "BoardId's are different");
								if(lCard.getBoardId_changed().after(tCard.getBoardId_changed())) {
									Log.d("SyncApp", "Trello needs updated");
									updateTrello.setBoardId(lCard.getBoardId());
								} else {
									Log.d("SyncApp", "Local needs updated");
									updateLocal.setBoardId(tCard.getBoardId());
								}
							}
						}
						//Closed
						if(tCard.getClosed() == null) {
							if(lCard.getClosed_changed().compareTo(lastSyncDate) >= 0) updateTrello.setClosed(lCard.getClosed());
						} else {
							//Compare dates and update accordingly
							if(lCard.getClosed() != tCard.getClosed()){
								Log.d("SyncApp", "Closeds are different");
								if(lCard.getClosed_changed().after(tCard.getClosed_changed())) {
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
		        		updateCard.setLocalId(lCard.getLocalId());
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
	    	updateBoard.setId(lboard.getId());
	    	updateBoard.setLastTrelloActionDate(TrelloServerREST.dateToTrelloDate(newLastActionDate));
	    	updateBoard.setLastSyncDate(TrelloServerREST.dateToTrelloDate(newLastSyncDate));
			this.updateLocalBoard(app, updateBoard);
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
    	ContentValues values = new ContentValues();
		Gson gson = new Gson();
		String json = gson.toJson(board);
    	values.put("json", json);
		Uri uri = Uri.parse("content://" + app + ".trello.provider/board");
    	this.getContext().getContentResolver().update(uri, values, null, null);  
    }
    
    private void updateLocalList(String app, TrelloList list){
		ContentValues values = new ContentValues();
		Gson gson = new Gson();
		String json = gson.toJson(list);
    	values.put("json", json);
		Uri uri = Uri.parse("content://" + app + ".trello.provider/list");
    	this.getContext().getContentResolver().update(uri, values, null, null);  
    }
    
    private void updateLocalCard(String app, TrelloCard card){
		ContentValues values = new ContentValues();
		Gson gson = new Gson();
		String json = gson.toJson(card);
    	values.put("json", json);
		Uri uri = Uri.parse("content://" + app + ".trello.provider/card");
    	this.getContext().getContentResolver().update(uri, values, null, null);        	
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
