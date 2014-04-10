package com.openatk.trello.response;

public class ActionResponse {
	public String idString;
	//todo data
	public String type;
	public String date;
	public ActionData data;
	
	static class ActionData {
		public ActionDataBoard board;
		public ActionDataList list;
		public ActionDataList listBefore;
		public ActionDataList listAfter;
		public ActionDataCard card;
		public ActionDataOld old;
		
		static class ActionDataBoard {
			public String id;
			public String name;
			public String desc;
			public Boolean closed;
		}
		static class ActionDataList {
			public String id;
			public String name;
			public Boolean closed;
		}
		static class ActionDataCard {
			public String id;
			public String name;
			public String desc;
			public Boolean closed;
		}
		static class ActionDataOld {
			public String name;
			public String desc;
			public String pos;
			public String idList;
			public Boolean closed;
		}
	}
}
