package com.openatk.trello.shared;

import java.util.Date;

public class TrelloObject {

	public TrelloObject(){
		
	}
	
	public Object[] toObjectArray(){	
		return null;
	}
	
	public static Long DateToUnix(Date date){
		return date.getTime();
	}
	
	public static Date UnixToDate(Long unix){
		return new Date(unix);
	}
}
