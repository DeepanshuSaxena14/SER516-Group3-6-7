package edu.asu.ser516.taiga;

public final class TaigaLoginObject {

    private final String username;
    private final String password;
    private String authToken;
    private int userId;

    public TaigaLoginObject(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static TaigaLoginObject fromEnv() {
        String username = System.getenv("TAIGA_USERNAME");
        String password = System.getenv("TAIGA_PASSWORD");
        return new TaigaLoginObject(
                username != null ? username : "",
                password != null ? password : "");
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAuthToken() {
        return authToken;
    }

    public int getUserId() {
        return userId;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}