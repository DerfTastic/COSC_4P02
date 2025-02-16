package server.infrastructure.param.auth;

import server.framework.web.error.Unauthorized;
import server.framework.web.request.Request;

public class RequireOrganizer extends RequireSession {

    @Override
    public UserSession construct(Request request) throws Exception {
        var auth = super.construct(request);
        if(auth.organizer_id==null)
            throw new Unauthorized("Organizer account needed");
        return auth;
    }
}
