package com.openatk.trello.internet;

import android.graphics.Bitmap;

public class TrelloMember {
	private String id = null;
	private String fullname = null;
	private String username = null;
	private String avatarHash = null;
	private Boolean inOrgo = false;
	private Bitmap avatar = null;

	public TrelloMember() {

	}

	public TrelloMember(String id, String fullname, String username, String avatarHash, Boolean inOrgo) {
		super();
		this.id = id;
		this.fullname = fullname;
		this.username = username;
		this.avatarHash = avatarHash;
		this.inOrgo = inOrgo;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAvatarHash() {
		return avatarHash;
	}

	public void setAvatarHash(String avatarHash) {
		this.avatarHash = avatarHash;
	}
	
	public Boolean getInOrgo() {
		return inOrgo;
	}

	public void setInOrgo(Boolean inOrgo) {
		this.inOrgo = inOrgo;
	}
	
	public Bitmap getAvatar(){
		return avatar;
	}
	
	public void setAvatar(Bitmap avatar){
		this.avatar = avatar;
	}
}
