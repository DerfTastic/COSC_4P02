package server.infrastructure.param.auth;

import framework.web.error.Unauthorized;
import framework.web.request.Request;

public class RequireOrganizer extends RequireSession {

    @Override
    public UserSession construct(Request request) throws Exception {
        var auth = super.construct(request);
        if(auth.organizer_id==null)
            throw new Unauthorized("Organizer account needed");
        return auth;
    }
}
