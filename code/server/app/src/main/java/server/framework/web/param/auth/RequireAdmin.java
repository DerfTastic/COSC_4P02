package server.framework.web.param.auth;

import server.framework.web.route.ClientError;
import server.framework.web.route.Request;

public class RequireAdmin extends RequireSession {

    @Override
    public UserSession construct(Request request) throws Exception {
        var auth = super.construct(request);
        if(!auth.admin)
            throw new ClientError.Unauthorized("Admin level access is needed");
        return auth;
    }
}
