package server.framework.web.param.auth;

import server.framework.db.DbManager;
import server.framework.web.route.ClientError;
import server.framework.web.route.Request;
import server.framework.web.route.RouteParameter;

public class RequireSession implements RouteParameter<UserSession> {

    @Override
    public UserSession construct(Request request) throws Exception {
        var token = request.exchange.getRequestHeaders().getFirst("X-UserAPIToken");
        if (token == null) throw new ClientError.Unauthorized("No valid session");
        try (var conn = request.getServer().getManagedResource(DbManager.class).ro_conn()) {
            return UserSession.create(token, conn);
        }
    }
}
