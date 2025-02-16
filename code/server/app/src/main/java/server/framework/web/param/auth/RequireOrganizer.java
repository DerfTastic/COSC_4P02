package server.framework.web.param.auth;

import server.framework.web.route.ClientError;
import server.framework.web.route.Request;

public class RequireOrganizer extends RequireSession {

    @Override
    public UserSession construct(Request request) throws Exception {
        var auth = super.construct(request);
        if(auth.organizer_id==null)
            throw new ClientError.Unauthorized("Organizer account needed");
        return auth;
    }
}
