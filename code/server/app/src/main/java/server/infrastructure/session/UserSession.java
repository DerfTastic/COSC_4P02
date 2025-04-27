package server.infrastructure.session;

/**
 * A common interface across any class representing a session of a user. <br>
 * Used in the API classes (in server/app/arc/main/java/server/infrastructure/root/api/) and other places.
 */
public interface UserSession {
    long user_id();
    long session_id();
    String email();
    boolean organizer();
    boolean admin();
}
