package server.infrastructure.param.auth;

import framework.web.error.Unauthorized;
import framework.web.request.Request;

import java.sql.SQLException;

public class RequireOrganizer extends RequireSession {

    @Override
    public UserSession construct(Request request) throws SQLException, Unauthorized {
        var auth = super.construct(request);
        if(!auth.organizer)
            throw new Unauthorized("Organizer account needed");
        return auth;
    }
}
