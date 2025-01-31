package server.web.auth;

public class UserSession {
    public int user_id;
    public int session_id;
    public String email;

    public Integer organizer_id;
    public Integer max_events;
    public Boolean has_analytics;

    public boolean admin;
}
