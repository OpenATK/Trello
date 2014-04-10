package com.openatk.trello.internet;

public class Login {
    
    Long id = null;
    String name = null;
    String username = null;
    String secret = null;
    String token = null;
    String apiKey = null;
    Boolean active = null;
    String organizationId = null;
    
    public Login(){
    	
    }
    
	public Login(Long id, String name, String username, String secret,
			String token, String apiKey, Boolean active, String organizationId) {
		super();
		this.id = id;
		this.name = name;
		this.username = username;
		this.secret = secret;
		this.token = token;
		this.apiKey = apiKey;
		this.active = active;
		this.organizationId = organizationId;
	}
	
	public Long getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getUsername() {
		return username;
	}
	public String getSecret() {
		return secret;
	}
	public String getToken() {
		return token;
	}
	public String getApiKey() {
		return apiKey;
	}
	public Boolean getActive() {
		return active;
	}
	public String getOrganizationId() {
		return organizationId;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public void setActive(Boolean active) {
		this.active = active;
	}
	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}
}
