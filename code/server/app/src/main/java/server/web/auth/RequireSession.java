package server.web.auth;

import server.db.DbManager;
import server.web.route.ClientError;
import server.web.route.Request;
import server.web.route.RouteParameter;

public class RequireSession implements RouteParameter<UserSession> {

    @Override
    public UserSession construct(Request request) throws Exception {
        var token = request.exchange.getRequestHeaders().getFirst("X-UserAPIToken");
        if (token == null) throw new ClientError.Unauthorized("No valid session");
        try (var conn = request.getServer().getManagedResource(DbManager.class).conn()) {
            try (var stmt = conn.namedPreparedStatement("select * from sessions left join users on sessions.user_id=users.id left join organizers on users.organizer_id=organizers.id where sessions.token=:token")) {
                stmt.setString(":token", token);
                var result = stmt.executeQuery();
                if (result == null || !result.next()) throw new ClientError.Unauthorized("No valid session");

                var auth = new UserSession();
                auth.session_id = result.getInt("id");
                auth.user_id = result.getInt("user_id");
                auth.email = result.getString("email");
                auth.admin = result.getBoolean("admin");

                auth.organizer_id = result.getInt("organizer_id");
                auth.max_events = result.getInt("max_events");
                auth.has_analytics = result.getBoolean("has_analytics");
                return auth;
            }
        }
    }
}
