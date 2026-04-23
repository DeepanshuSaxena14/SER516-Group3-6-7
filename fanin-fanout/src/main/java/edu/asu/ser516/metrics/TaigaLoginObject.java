package edu.asu.ser516.metrics;

/**
 * Holds Taiga API credentials and the auth token returned after a successful
 * login call. Use {@link #fromEnv()} to construct an instance from environment
 * variables ({@code TAIGA_USERNAME} and {@code TAIGA_PASSWORD}).
 */
public final class TaigaLoginObject {

    private final String username;
    private final String password;
    private String authToken;
    private int userId;

    public TaigaLoginObject(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Constructs a {@code TaigaLoginObject} whose credentials are read from
     * the {@code TAIGA_USERNAME} and {@code TAIGA_PASSWORD} environment
     * variables. Returns an instance with blank/null fields if the variables
     * are absent; the caller is responsible for validating before use.
     */
    public static TaigaLoginObject fromEnv() {
        String username = System.getenv("TAIGA_USERNAME");
        String password = System.getenv("TAIGA_PASSWORD");
        return new TaigaLoginObject(
                username != null ? username : "",
                password != null ? password : "");
    }

    public String getUsername()  { return username;  }
    public String getPassword()  { return password;  }
    public String getAuthToken() { return authToken; }
    public int    getUserId()    { return userId;     }

    /** Set by {@link TaigaClient#login} after a successful authentication. */
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    /** Set by {@link TaigaClient#login} after a successful authentication. */
    public void setUserId(int userId) { this.userId = userId; }
}
