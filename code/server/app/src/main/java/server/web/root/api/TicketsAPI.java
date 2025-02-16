package server.web.root.api;

import server.db.RoTransaction;
import server.db.RwTransaction;
import server.web.annotations.FromRequest;
import server.web.annotations.Json;
import server.web.annotations.Route;
import server.web.annotations.Routes;
import server.web.annotations.url.Path;
import server.web.param.auth.OptionalAuth;
import server.web.param.auth.RequireOrganizer;
import server.web.param.auth.UserSession;
import server.web.route.ClientError;

import java.sql.SQLException;

@SuppressWarnings("unused")
@Routes
public class TicketsAPI {

    @Route("/create_ticket/<event_id>")
    public static int create_ticket(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long event_id) throws SQLException, ClientError.Unauthorized {
        try(var stmt = trans.namedPreparedStatement("select organizer_id from events where id=:id")){
            stmt.setLong("id", event_id);
            var rs = stmt.executeQuery();
            if(!rs.next()||rs.getLong("organizer_id")!=session.organizer_id)
                throw new ClientError.Unauthorized("Cannot modify specified event, it either doesn't exist or you do not have ownership of it");
        }
        try(var stmt = trans.namedPreparedStatement("insert into tickets values (null, :event_id, '', 0, null) returning id")){
            return stmt.executeQuery().getInt("id");
        }
    }

    public static class Ticket{
        public long id;
        public long event_id;
        public String name;
        public long price;
        public Integer limit;
    }

    public static @Json Ticket[] get_tickets(@FromRequest(OptionalAuth.class)UserSession session, RoTransaction trans){
        return new Ticket[]{};
    }

    @Route
    public static long update_ticket(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans){
        return -1;
    }
}
