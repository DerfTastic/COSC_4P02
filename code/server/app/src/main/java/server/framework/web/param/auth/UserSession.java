package server.framework.web.param.auth;

import server.framework.db.RoConn;
import server.framework.web.route.ClientError;

import java.sql.SQLException;

public class UserSession {
    public long user_id;
    public long session_id;
    public String email;

    public Long organizer_id;
    public Boolean has_analytics;

    public boolean admin;

    private UserSession(){}

    public static UserSession create(String token, RoConn conn) throws SQLException, ClientError.Unauthorized {
        try (var stmt = conn.namedPreparedStatement("select * from sessions left join users on sessions.user_id=users.id left join organizers on users.organizer_id=organizers.id where sessions.token=:token")) {
            stmt.setString(":token", token);
            var result = stmt.executeQuery();
            if (result == null || !result.next())
                throw new ClientError.Unauthorized("No valid session");

            var auth = new UserSession();

            auth.session_id = result.getLong("id");
            auth.user_id = result.getLong("user_id");
            auth.email = result.getString("email");
            auth.admin = result.getBoolean("admin");

            auth.organizer_id = result.getString("organizer_id")==null?null:result.getLong("organizer_id");
            auth.has_analytics = result.getString("has_analytics")==null?null:result.getBoolean("has_analytics");

            return auth;
        }
    }

    public static UserSession optional(String token, RoConn conn) throws SQLException {
        if (token == null) return null;
        if(token.isEmpty()) return null;
        try (var stmt = conn.namedPreparedStatement("select * from sessions left join users on sessions.user_id=users.id left join organizers on users.organizer_id=organizers.id where sessions.token=:token")) {
            stmt.setString(":token", token);
            var result = stmt.executeQuery();
            if (result == null || !result.next()) return null;

            var auth = new UserSession();

            auth.session_id = result.getInt("id");
            auth.user_id = result.getInt("user_id");
            auth.email = result.getString("email");
            auth.admin = result.getBoolean("admin");

            auth.organizer_id = result.getString("organizer_id")==null?null:result.getLong("organizer_id");
            auth.has_analytics = result.getString("has_analytics")==null?null:result.getBoolean("has_analytics");
            return auth;
        }
    }
}
