package server.infrastructure.session;

public interface UserSession {
    long user_id();
    long session_id();
    String email();
    boolean organizer();
    boolean admin();
}
