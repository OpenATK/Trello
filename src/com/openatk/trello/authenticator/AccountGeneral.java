package com.openatk.trello.authenticator;

/**
 * Created with IntelliJ IDEA.
 * User: Udini
 * Date: 20/03/13
 * Time: 18:11
 */
public class AccountGeneral {

    /**
     * Account type id, needs to be the same as accountType in authenticator.xml
     */
    public static final String ACCOUNT_TYPE = "com.openatk.trello_sync";

    /**
     * Account name
     */
    public static final String ACCOUNT_NAME = "OpenATK - Trello";

    /**
     * Auth token types
     */
    public static final String AUTHTOKEN_TYPE_READ_ONLY = "Read only";
    public static final String AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to an Trello account";

    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to an Trello account";
    
    public static final String API_KEY = "b1ae1192adda1b5b61563d30d7ab403b";
    public static final String API_SECRET = "ab4d27e041c95c61285aa7392b74d3ebfaf4948edf48054295d2d7a1d421b8ff";

}
