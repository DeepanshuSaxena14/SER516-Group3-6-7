package org.taiga;

// stores the users login info and the token we get back from taiga
public class TaigaLoginObject {

    String username;
    String password;
    String authToken;
    int userId;

    public TaigaLoginObject(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getAuthToken() { return authToken; }
    public int getUserId() { return userId; }

    // these get set after a successful login
    public void setAuthToken(String authToken) { this.authToken = authToken; }
    public void setUserId(int userId) { this.userId = userId; }
}
