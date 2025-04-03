package server.infrastructure.root.api;

import framework.db.RoTransaction;
import framework.db.RwTransaction;
import framework.util.SqlSerde;
import framework.web.annotations.*;
import framework.web.annotations.url.Path;
import framework.web.error.BadRequest;
import framework.web.error.Unauthorized;
import server.infrastructure.param.NotRequired;
import server.infrastructure.session.OrganizerSession;
import server.infrastructure.session.UserSession;

import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("unused")
@Routes
public class TicketsAPI {

    public record Ticket(
            long id,
            String name,
            long price,
            Long total_tickets
    ){}

    @Route("/create_ticket/<event_id>")
    public static long create_ticket(OrganizerSession session, RwTransaction trans, @Path long event_id) throws SQLException, Unauthorized {
        try (var stmt = trans.namedPreparedStatement("select owner_id from events where id=:id")) {
            stmt.setLong(":id", event_id);
            var rs = stmt.executeQuery();
            if (!rs.next() || rs.getLong("owner_id") != session.user_id())
                throw new Unauthorized("Cannot find specified event, it either doesn't exist or you do not have ownership of it");
        }
        long result;
        try (var stmt = trans.namedPreparedStatement("insert into tickets values (null, :event_id, '', 0, null) returning id")) {
            stmt.setLong(":event_id", event_id);
            result = stmt.executeQuery().getLong("id");
        }
        trans.commit();
        return result;
    }

    @Route("/update_ticket/<ticket_id>")
    public static void update_ticket(OrganizerSession session, @Json @Body Ticket ticket, @Path long ticket_id, RwTransaction trans) throws SQLException, Unauthorized, BadRequest {
        try (var stmt = trans.namedPreparedStatement("select owner_id from tickets inner join events on tickets.event_id=events.id where tickets.id=:ticket_id")) {
            stmt.setLong(":ticket_id", ticket_id);
            var rs = stmt.executeQuery();
            if (!rs.next())
                throw new Unauthorized("Cannot find specified event, it either doesn't exist or you do not have ownership of it");
            var og_id = rs.getLong("owner_id");
            if(og_id != session.user_id())
                throw new Unauthorized("Cannot find specified event, it either doesn't exist or you do not have ownership of it");
        }
        long result;
        try (var stmt = trans.namedPreparedStatement("update tickets set name=:name, price=:price, total_tickets=:total_tickets where id=:id")) {
            stmt.setString(":name", ticket.name);
            stmt.setLong(":price", ticket.price);
            if(ticket.total_tickets!=null)
                stmt.setLong(":total_tickets", ticket.total_tickets);
            stmt.setLong(":id", ticket_id);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to update ticket");
        }
        trans.commit();
    }

    @Route("/get_tickets/<event_id>")
    public static @Json List<Ticket> get_tickets(@NotRequired UserSession session, RoTransaction trans, @Path long event_id) throws SQLException, Unauthorized, BadRequest {
        try (var stmt = trans.namedPreparedStatement("select draft, owner_id from events where id=:id")) {
            stmt.setLong(":id", event_id);
            var rs = stmt.executeQuery();

            if (!rs.next())
                throw new Unauthorized("Cannot find specified event, because it doesn't exist");
            var draft = rs.getBoolean("draft");
            var og_id = rs.getLong("owner_id");
            if(draft&&og_id!=(session==null?0: session.user_id()))
                throw new Unauthorized("Cannot find specified event, because it's a draft and you do not have ownership over it");
        }
        List<Ticket> result;
        try (var stmt = trans.namedPreparedStatement("select * from tickets where event_id=:event_id")) {
            stmt.setLong(":event_id", event_id);
            result = SqlSerde.sqlList(stmt.executeQuery(), rs -> new Ticket(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getLong("price"),
                    SqlSerde.nullableLong(rs, "total_tickets")
            ));
        }
        trans.commit();
        return result;
    }

    @Route("/delete_ticket/<ticket_id>")
    public static void delete_ticket(OrganizerSession session, RwTransaction trans, @Path long ticket_id) throws SQLException, Unauthorized, BadRequest {
        try (var stmt = trans.namedPreparedStatement("select owner_id from tickets inner join events on tickets.event_id=events.id where tickets.id=:ticket_id")) {
            stmt.setLong(":ticket_id", ticket_id);
            var rs = stmt.executeQuery();
            if (!rs.next() || rs.getLong("owner_id") != session.user_id())
                throw new Unauthorized("Cannot modify specified event, it either doesn't exist or you do not have ownership of it");
        }
        long result;
        try (var stmt = trans.namedPreparedStatement("delete from tickets where id=:id")) {
            stmt.setLong(":id", ticket_id);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to update ticket");
        }
        trans.commit();
    }

}
