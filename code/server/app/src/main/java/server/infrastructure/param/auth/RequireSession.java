package server.infrastructure.param.auth;

import framework.db.DbManager;
import framework.web.error.Unauthorized;
import framework.web.request.Request;
import framework.web.route.RouteParameter;

public class RequireSession implements RouteParameter<UserSession> {

    @Override
    public UserSession construct(Request request) throws Exception {
        var token = request.exchange.getRequestHeaders().getFirst("X-UserAPIToken");
        if (token == null) throw new Unauthorized("No valid session");
        try (var conn = request.getServer().getManagedState(DbManager.class).ro_conn(request.mountedPath())) {
            return UserSession.create(token, conn, request.getServer().getManagedState(SessionCache.class));
        }
    }
}
