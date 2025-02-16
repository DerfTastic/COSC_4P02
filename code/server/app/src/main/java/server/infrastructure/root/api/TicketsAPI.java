package server.infrastructure.root.api;

import server.framework.db.RoTransaction;
import server.framework.db.RwTransaction;
import server.framework.web.annotations.FromRequest;
import server.framework.web.annotations.Json;
import server.framework.web.annotations.Route;
import server.framework.web.annotations.Routes;
import server.framework.web.annotations.url.Path;
import server.framework.web.error.Unauthorized;
import server.infrastructure.param.auth.OptionalAuth;
import server.infrastructure.param.auth.RequireOrganizer;
import server.infrastructure.param.auth.UserSession;

import java.sql.SQLException;

@SuppressWarnings("unused")
@Routes
public class TicketsAPI {

    @Route("/create_ticket/<event_id>")
    public static int create_ticket(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long event_id) throws SQLException, Unauthorized {
        try(var stmt = trans.namedPreparedStatement("select organizer_id from events where id=:id")){
            stmt.setLong("id", event_id);
            var rs = stmt.executeQuery();
            if(!rs.next()||rs.getLong("organizer_id")!=session.organizer_id)
                throw new Unauthorized("Cannot modify specified event, it either doesn't exist or you do not have ownership of it");
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
