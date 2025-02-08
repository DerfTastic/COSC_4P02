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

import java.sql.SQLException;

@SuppressWarnings("unused")
@Routes
public class TicketsAPI {

    @Route("/create_ticket/<event_id>")
    public static int create_ticket(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path int event_id) throws SQLException {
//        try(var stmt = trans.namedPreparedStatement("select"))

        try(var stmt = trans.namedPreparedStatement("insert into tickets values (null, :event_id, '', 0, null) returning id")){
            stmt.executeUpdate();
        }
        return -1;
    }

    public static class Ticket{
        public int id;
        public int event_id;
        public String name;
        public int price;
        public Integer limit;
    }

    public static @Json Ticket[] get_tickets(@FromRequest(OptionalAuth.class)UserSession session, RoTransaction trans){
        return new Ticket[]{};
    }

    @Route
    public static int update_ticket(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans){
        return -1;
    }
}
