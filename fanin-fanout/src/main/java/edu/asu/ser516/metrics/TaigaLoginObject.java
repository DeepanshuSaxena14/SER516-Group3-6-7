package edu.asu.ser516.metrics;

// stores the users login info and the token we get back from taiga
public class TaigaLoginObject {

    private String username;
    private String password;
    private String authToken;
    private int userId;

    public TaigaLoginObject(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // pull credentials from env vars so we dont hardcode them
    public static TaigaLoginObject fromEnv() {
        String user = System.getenv("TAIGA_USERNAME");
        String pass = System.getenv("TAIGA_PASSWORD");
        if (user == null || pass == null) {
            throw new IllegalStateException("TAIGA_USERNAME and TAIGA_PASSWORD must be set");
        }
        return new TaigaLoginObject(user, pass);
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getAuthToken() { return authToken; }
    public int getUserId() { return userId; }

    // these get set after a successful login
    public void setAuthToken(String authToken) { this.authToken = authToken; }
    public void setUserId(int userId) { this.userId = userId; }
}
