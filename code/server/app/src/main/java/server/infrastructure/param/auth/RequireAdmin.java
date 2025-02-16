package server.infrastructure.param.auth;

import server.framework.web.error.Unauthorized;
import server.framework.web.request.Request;

public class RequireAdmin extends RequireSession {

    @Override
    public UserSession construct(Request request) throws Exception {
        var auth = super.construct(request);
        if(!auth.admin)
            throw new Unauthorized("Admin level access is needed");
        return auth;
    }
}
