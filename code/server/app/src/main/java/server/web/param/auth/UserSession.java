package server.web.param.auth;

public class UserSession {
    public long user_id;
    public long session_id;
    public String email;

    public Long organizer_id;
    public Boolean has_analytics;

    public boolean admin;
}
