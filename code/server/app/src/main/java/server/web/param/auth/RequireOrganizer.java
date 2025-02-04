package server.web.param.auth;

import server.web.route.ClientError;
import server.web.route.Request;

public class RequireOrganizer extends RequireSession {

    @Override
    public UserSession construct(Request request) throws Exception {
        var auth = super.construct(request);
        if(auth.organizer_id==null)
            throw new ClientError.Unauthorized("Organizer account needed");
        return auth;
    }
}
