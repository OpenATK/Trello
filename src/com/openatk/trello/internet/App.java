package com.openatk.trello.internet;

import android.graphics.drawable.Drawable;

public class App {
	private String packageName = null;
	private String name = null;
	private String desc = null;
	private Drawable icon = null;
	
	private Long id = null;
	private Boolean syncThisApp = false;
	private Boolean installed = false;
	
	private Integer autoSync = null;
	private String lastSync = null;
	private String boardName = null;

	public App() {
	
	}
	  	
  	public App(Long Id, Boolean AppIsSynced, Boolean Installed, String PackageName, String Name, String Description, Drawable Icon) {
		if(Id != null) setId(Id);
		if(AppIsSynced != null) setSyncApp(AppIsSynced);
		if(Installed != null) setInstalled(Installed);
  		if(PackageName != null) setPackage(PackageName);
		if(Name != null) setName(Name);
		if(Description != null) setDesc(Description);
		if(Icon != null) setIcon(Icon);
	}
	
	public App(String PackageName, String Name, String Description, Drawable Icon) {
		if(PackageName != null) setPackage(PackageName);
		if(Name != null) setName(Name);
		if(Description != null) setDesc(Description);
		if(Icon != null) setIcon(Icon);
	}
	
	public Integer getAutoSync() {
		return autoSync;
	}

	public String getLastSync() {
		return lastSync;
	}

	public String getBoardName() {
		return boardName;
	}

	public void setAutoSync(Integer autoSync) {
		this.autoSync = autoSync;
	}

	public void setLastSync(String lastSync) {
		this.lastSync = lastSync;
	}

	public void setBoardName(String boardName) {
		this.boardName = boardName;
	}
	
	public void setId(Long newId){
		id = newId;
	}
	public void setSyncApp(Boolean isAppSynced){
		syncThisApp = isAppSynced;
	}
	public void setInstalled(Boolean isInstalled){
		installed = isInstalled;
	}
	public void setPackage(String newPackage){
		packageName = newPackage;
	}
	public void setName(String newName){
		name = newName;
	}
	public void setDesc(String newDesc){
		desc = newDesc;
	}
	public void setIcon(Drawable newIcon){
		icon = newIcon;
	}
	
	
	public Long getId(){
		return id;
	}
	public Boolean getSyncApp(){
		return syncThisApp;
	}
	public Boolean getInstalled(){
		return installed;
	}
	public String getName(){
		if(name != null) return name;
		return "No Name";
	}
	public String getDesc(){
		if(desc != null) return desc;
		return "";
	}
	public String getPackageName(){
		return packageName;
	} 
	public Drawable getIcon(){
		return icon;
	} 
}
