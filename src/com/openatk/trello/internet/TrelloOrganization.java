package com.openatk.trello.internet;

import android.graphics.Bitmap;


public class TrelloOrganization {
	private String id = null;
	private String name = null;
	private String displayName = null;
	private String desc = null;
	private Bitmap icon = null;

	
	public TrelloOrganization() {
	
	}
	
	public TrelloOrganization(String ID, String Name, String DisplayName, String Description, Bitmap icon) {
		if(ID != null) setId(ID);
		if(Name != null) setName(Name);
		if(DisplayName != null) setDisplayName(DisplayName);
		if(Description != null) setDesc(Description);
		if(icon != null) setIcon(icon);
	}
	
	public void setId(String newId){
		id = newId;
	}
	public String getId(){
		return id;
	}
	
	public void setName(String name){
		this.name = name;
	}
	public String getName(){
		return name;
	}
	
	public void setDisplayName(String displayName){
		this.displayName = displayName;
	}
	public String getDisplayName(){
		if(displayName == null){
			return "No Name";
		} else {
			return displayName;
		}
	}
	
	public void setDesc(String desc){
		this.desc = desc;
	}
	public String getDesc(){
		return desc;
	}
	
	public void setIcon(Bitmap icon){
		this.icon = icon;
	}
	public Bitmap getIcon(){
		return icon;
	}

}
