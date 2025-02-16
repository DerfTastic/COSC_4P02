package server.infrastructure.param.auth;

import server.framework.db.DbManager;
import server.framework.web.request.Request;
import server.framework.web.route.RouteParameter;

public class OptionalAuth implements RouteParameter<UserSession> {
        @Override
        public UserSession construct(Request request) throws Exception {
            var token = request.exchange.getRequestHeaders().getFirst("X-UserAPIToken");
            if (token == null) return null;
            if(token.isEmpty()) return null;
            try (var conn = request.getServer().getManagedResource(DbManager.class).ro_conn()) {
                return UserSession.optional(token, conn);
            }
        }
    }