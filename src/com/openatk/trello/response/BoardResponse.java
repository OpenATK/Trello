package com.openatk.trello.response;

import java.util.List;

public class BoardResponse {
	public String id;
	public String name;
	public String desc;
	public String dateLastActivity;
	public Boolean closed;
	public String idOrganization;
	public List<ListResponse> lists;
	public List<CardResponse> cards;
	public List<ActionResponse> actions;
	//TODO
	//Add lists
	//Add labelNames
	//Add members
	//Add membersInvited
	
	public BoardResponse(){
		
	}
}
