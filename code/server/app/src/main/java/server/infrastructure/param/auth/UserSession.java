package server.infrastructure.param.auth;

import framework.db.RoConn;
import framework.web.error.Unauthorized;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserSession{
    public final long user_id;
    public final long session_id;
    public final String email;
    public final boolean organizer;
    public final boolean admin;

    public UserSession(long userId, long sessionId, String email, boolean organizer, boolean admin) {
        this.user_id = userId;
        this.session_id = sessionId;
        this.email = email;
        this.organizer = organizer;
        this.admin = admin;
    }

    public static UserSession create(String token, RoConn conn, SessionCache cache) throws SQLException, Unauthorized {
        Logger.getGlobal().log(Level.FINER, "Authenticating with sessions: " + token);

        if(cache!=null){
            var res = cache.validate(token);
            if(res!=null)return res;
        }
        try (var stmt = conn.namedPreparedStatement("select * from sessions left join users on sessions.user_id=users.id where sessions.token=:token")) {
            stmt.setString(":token", token);
            try(var result = stmt.executeQuery()){
                if (result == null || !result.next())
                    throw new Unauthorized("No valid session");

                var session = new UserSession(
                        result.getLong("user_id"),
                        result.getLong("id"),
                        result.getString("email"),
                        result.getBoolean("organizer"),
                        result.getBoolean("admin")
                );
                if(cache!=null)cache.add(token, session);
                return session;
            }
        }
    }

    public static UserSession optional(String token, RoConn conn, SessionCache cache) throws SQLException {
        Logger.getGlobal().log(Level.FINER, "Authenticating with sessions: " + token);

        if (token == null) return null;
        if(token.isEmpty()) return null;
        if(cache!=null){
            var res = cache.validate(token);
            if(res!=null)return res;
        }
        try (var stmt = conn.namedPreparedStatement("select * from sessions left join users on sessions.user_id=users.id where sessions.token=:token")) {
            stmt.setString(":token", token);
            try(var result = stmt.executeQuery()){
                if (result == null || !result.next()) return null;

                var session = new UserSession(
                        result.getLong("user_id"),
                        result.getLong("id"),
                        result.getString("email"),
                        result.getBoolean("organizer"),
                        result.getBoolean("admin")
                );
                if(cache!=null)cache.add(token, session);
                return session;
            }
        }
    }
}
