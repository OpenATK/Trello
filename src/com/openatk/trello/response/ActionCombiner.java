package com.openatk.trello.response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.util.Log;

import com.openatk.trello.shared.TrelloBoard;
import com.openatk.trello.shared.TrelloCard;
import com.openatk.trello.shared.TrelloList;
import com.openatk.trello.sync_adapter.TrelloServerREST;

public class ActionCombiner {
	//TODO possible multiple things changing on one action? Test this by a post
	
	public ActionCombiner(){
		
	}
	
	public List<TrelloCard> getCards(List<ActionResponse> actions){
		List<TrelloCard> cards = new ArrayList<TrelloCard>();
		if(actions.size() != 0){
			for(int i=actions.size()-1; i>=0; i--){ //Loop through actions backwards
				ActionResponse action = actions.get(i);
				Log.d("ActionCombiner - getCards", "Type:" + action.type);
				if(this.isCardType(action.type)){
					Log.d("ActionCombiner - getCards", "Is card type");
					ActionResponse.ActionData.ActionDataCard dataCard = action.data.card;
					Integer where = findCardWithId(cards, dataCard.id);
					TrelloCard theCard;
					if(where != null){
						//We have this object so update it
						theCard = cards.get(where);
					} else {
						//We don't have this object yet, add it
						cards.add(0, new TrelloCard(null));
						theCard = cards.get(0);
						theCard.setId(dataCard.id);
					}
					//Add
					if(action.type.contentEquals("createCard")) {
						theCard.setName(dataCard.name);
						theCard.setName_changed(TrelloServerREST.trelloDateToDate(action.date));
						theCard.setListId(action.data.list.id);
						theCard.setListId_changed(TrelloServerREST.trelloDateToDate(action.date));
						theCard.setBoardId(action.data.board.id);
						theCard.setBoardId_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					//Change name
					if(action.data.old != null && action.data.old.name != null) {
						theCard.setName(dataCard.name);
						theCard.setName_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					//Change desc
					if(action.data.old != null && action.data.old.desc != null){
						theCard.setDesc(dataCard.desc);
						theCard.setDesc_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					//Change list
					if(action.data.old != null && action.data.old.idList != null){
						theCard.setListId(action.data.listAfter.id);
						theCard.setListId_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					//Archived or Unarchived
					if(action.data.old != null && action.data.old.closed != null){
						theCard.setClosed(dataCard.closed);
						theCard.setClosed_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					
					//TODO deleted, labels, pos, boardId
				}
			}	
		}
		
		//REMOVE
		for(int i=0; i<cards.size(); i++){
			if(cards.get(i).getName() == null){
				Log.d("ActionCombiner", "Null card name! CardId:" + cards.get(i).getId());
			}
		}
		
		return cards;
	}
	
	public List<TrelloList> getLists(List<ActionResponse> actions){
		List<TrelloList> lists = new ArrayList<TrelloList>();
		if(actions.size() != 0){
			for(int i=actions.size()-1; i>=0; i--){ //Loop through actions backwards
				ActionResponse action = actions.get(i);
				Log.d("ActionCombiner - getLists", "Type:" + action.type);
				if(this.isListType(action.type)) {
					Log.d("ActionCombiner - getLists", "Is list type");
					ActionResponse.ActionData.ActionDataList dataList = action.data.list;
					Integer where = findListWithId(lists, dataList.id);
					TrelloList theList;
					if(where != null){
						//We have this object so update it
						theList = lists.get(where);
					} else {
						//We don't have this object yet, add it
						lists.add(0, new TrelloList(null));
						theList = lists.get(0);
						theList.setId(dataList.id);
					}
					//Add
					if(action.type.contentEquals("createList")){
						theList.setName(dataList.name);
						theList.setName_changed(TrelloServerREST.trelloDateToDate(action.date));
						
					}
					//Change name
					if(action.data.old != null && action.data.old.name != null){
						theList.setName(dataList.name);
						theList.setName_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					//Archived or Unarchived
					if(action.data.old != null && action.data.old.closed != null){
						theList.setClosed(dataList.closed);
						theList.setClosed_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					
					//TODO pos, boardId
				} 
			}		
		}
		return lists;
	}
	
	public List<TrelloBoard> getBoards(List<ActionResponse> actions){
		List<TrelloBoard> boards = new ArrayList<TrelloBoard>();
		if(actions.size() != 0){
			for(int i=actions.size()-1; i>=0; i--){ //Loop through actions backwards
				ActionResponse action = actions.get(i);
				if(this.isBoardType(action.type)) {
					ActionResponse.ActionData.ActionDataBoard dataBoard = action.data.board;
					Integer where = findBoardWithId(boards, dataBoard.id);
					TrelloBoard theBoard;
					if(where != null){
						//We have this object so update it
						theBoard = boards.get(where);
					} else {
						//We don't have this object yet, add it
						boards.add(0, new TrelloBoard(null));
						theBoard = boards.get(0);
						theBoard.setId(dataBoard.id);
					}
					//Add
					if(action.type.contentEquals("createBoard")){
						theBoard.setName(dataBoard.name);
						theBoard.setName_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					//Change name
					if(action.data.old != null && action.data.old.name != null){
						theBoard.setName(dataBoard.name);
						theBoard.setName_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					//Change desc
					if(action.data.old != null && action.data.old.desc != null){
						theBoard.setDesc(dataBoard.desc);
						theBoard.setDesc_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					//Archived or Unarchived
					if(action.data.old != null && action.data.old.closed != null){
						theBoard.setClosed(dataBoard.closed);
						theBoard.setClosed_changed(TrelloServerREST.trelloDateToDate(action.date));
					}
					
					//TODO labels, organizationId
				} 
			}	
		}
		return boards;
	}
	
	private Integer findBoardWithId(List<TrelloBoard> objects, String id){
		for(int i=0; i<objects.size(); i++){
			if(objects.get(i).getId().contentEquals(id)){
				return i;
			}
		}
		return null;
	}
	
	private Integer findListWithId(List<TrelloList> objects, String id){
		for(int i=0; i<objects.size(); i++){
			if(objects.get(i).getId().contentEquals(id)){
				return i;
			}
		}
		return null;
	}
	
	private Integer findCardWithId(List<TrelloCard> objects, String id){
		for(int i=0; i<objects.size(); i++){
			if(objects.get(i).getId().contentEquals(id)){
				return i;
			}
		}
		return null;
	}
	
	public boolean isCardType(String type){
		List<String> types = Arrays.asList("createCard", "updateCard", "deleteCard");
		return types.contains(type);
	}
	public boolean isListType(String type){
		List<String> types = Arrays.asList("createList", "updateList");
		return types.contains(type);
	}
	public boolean isBoardType(String type){
		List<String> types = Arrays.asList("createBoard", "updateBoard");
		return types.contains(type);
	}
}
