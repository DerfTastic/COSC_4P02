package server.infrastructure.param.auth;

import framework.web.error.Unauthorized;
import framework.web.request.Request;

import java.sql.SQLException;

public class RequireAdmin extends RequireSession {

    @Override
    public UserSession construct(Request request) throws SQLException, Unauthorized {
        var auth = super.construct(request);
        if(!auth.admin())
            throw new Unauthorized("Admin level access is needed");
        return auth;
    }
}
