package server.infrastructure.param.auth;

import framework.db.DbManager;
import framework.web.request.Request;
import framework.web.route.RouteParameter;

public class OptionalAuth implements RouteParameter<UserSession> {
        @Override
        public UserSession construct(Request request) throws Exception {
            var token = request.exchange.getRequestHeaders().getFirst("X-UserAPIToken");
            if (token == null) return null;
            if(token.isEmpty()) return null;
            try (var conn = request.getServer().getManagedState(DbManager.class).ro_conn(request.mountedPath())) {
                return UserSession.optional(token, conn, request.getServer().getManagedState(SessionCache.class));
            }
        }
    }