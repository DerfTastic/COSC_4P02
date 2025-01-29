package server.web.auth;

public class UserSession {
    public int user_id;
    public int session_id;
    public String email;

    public int organizer_id;
    public int max_events;
    public boolean has_analytics;

    public boolean admin;
}
