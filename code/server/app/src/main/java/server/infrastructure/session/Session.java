package server.infrastructure.session;

import framework.db.DbManager;
import framework.db.RoConn;
import framework.web.error.Unauthorized;
import framework.web.request.Request;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Session implements UserSession{
    private final long user_id;
    private final long session_id;
    private final String email;
    private final boolean organizer;
    private final boolean admin;

    private static final class Regular extends Session{
        private Regular(long user_id, long session_id, String email, boolean organizer, boolean admin) {
            super(user_id, session_id, email, organizer, admin);
        }
    }
    private static final class Admin extends Session implements AdminSession{
        private Admin(long user_id, long session_id, String email, boolean organizer, boolean admin) {
            super(user_id, session_id, email, organizer, admin);
        }
    }
    private static final class Organizer extends Session implements AdminSession, OrganizerSession{
        private Organizer(long user_id, long session_id, String email, boolean organizer, boolean admin) {
            super(user_id, session_id, email, organizer, admin);
        }
    }
    private static final class AdminOrganizer extends Session implements AdminSession, OrganizerSession{
        private AdminOrganizer(long user_id, long session_id, String email, boolean organizer, boolean admin) {
            super(user_id, session_id, email, organizer, admin);
        }
    }

    private Session(long user_id, long session_id, String email, boolean organizer, boolean admin) {
        this.user_id = user_id;
        this.session_id = session_id;
        this.email = email;
        this.organizer = organizer;
        this.admin = admin;
    }

    private static UserSession make(long user_id, long session_id, String email, boolean organizer, boolean admin){
        if(admin&&organizer)
            return new AdminOrganizer(
                   user_id,
                    session_id,
                    email,
                    organizer,
                    admin
            );
        if(admin)
            return new Admin(
                    user_id,
                    session_id,
                    email,
                    organizer,
                    admin
            );
        if(organizer)
            return new Organizer(
                    user_id,
                    session_id,
                    email,
                    organizer,
                    admin
            );
        return new Regular(
                user_id,
                session_id,
                email,
                organizer,
                admin
        );
    }

    public static UserSession create(String token, RoConn conn, SessionCache cache) throws SQLException, Unauthorized {
        Logger.getGlobal().log(Level.FINER, "Authenticating with sessions: " + token);

        if (cache != null) {
            var res = cache.validate(token);
            if (res != null) return res;
        }
        try (var stmt = conn.namedPreparedStatement("select * from sessions left join users on sessions.user_id=users.id where sessions.token=:token")) {
            stmt.setString(":token", token);
            try (var result = stmt.executeQuery()) {
                if (result == null || !result.next())
                    throw new Unauthorized("No valid session");

                var session = make(
                        result.getLong("user_id"),
                        result.getLong("id"),
                        result.getString("email"),
                        result.getBoolean("organizer"),
                        result.getBoolean("admin")
                );
                if (cache != null) cache.add(token, session);
                return session;
            }
        }
    }

    public static UserSession require_session(Request request, boolean optional) throws Unauthorized, SQLException {
        var token = request.exchange.getRequestHeaders().getFirst("X-UserAPIToken");
        if (token == null&&optional) return null;
        if (token == null) throw new Unauthorized("No valid session");
        if(token.isEmpty()&&optional) return null;
        try (var conn = request.getServer().getManagedState(DbManager.class).ro_conn(request.mountedPath())) {
            return Session.create(token, conn, request.getServer().getManagedState(SessionCache.class));
        }
    }

    public static AdminSession require_admin_session(Request request, boolean optional) throws Unauthorized, SQLException {
        var auth = require_session(request, optional);
        if(auth==null)return null;
        if(!auth.admin())
            throw new Unauthorized("Admin level access is needed");
        return (AdminSession) auth;
    }

    public static OrganizerSession require_organizer_session(Request request, boolean optional) throws Unauthorized, SQLException {
        var auth = require_session(request, optional);
        if(auth==null)return null;
        if(!auth.organizer())
            throw new Unauthorized("Organizer account needed");
        return (OrganizerSession) auth;
    }

    public static UserSession optional(String token, RoConn conn, SessionCache cache) throws SQLException {
        Logger.getGlobal().log(Level.FINER, "Authenticating with sessions: " + token);

        if (token == null) return null;
        if (token.isEmpty()) return null;
        if (cache != null) {
            var res = cache.validate(token);
            if (res != null) return res;
        }
        try (var stmt = conn.namedPreparedStatement("select * from sessions left join users on sessions.user_id=users.id where sessions.token=:token")) {
            stmt.setString(":token", token);
            try (var result = stmt.executeQuery()) {
                if (result == null || !result.next()) return null;

                var session = make(
                        result.getLong("user_id"),
                        result.getLong("id"),
                        result.getString("email"),
                        result.getBoolean("organizer"),
                        result.getBoolean("admin")
                );
                if (cache != null) cache.add(token, session);
                return session;
            }
        }
    }

    public long user_id() {
        return user_id;
    }

    public long session_id() {
        return session_id;
    }

    public String email() {
        return email;
    }

    public boolean organizer() {
        return organizer;
    }

    public boolean admin() {
        return admin;
    }
}
