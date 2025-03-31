package server.infrastructure.root.api;

import framework.db.RwTransaction;
import framework.util.SqlSerde;
import framework.web.annotations.*;
import framework.web.error.BadRequest;
import server.infrastructure.param.auth.RequireOrganizer;
import server.infrastructure.param.auth.UserSession;

import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("unused")
@Routes
public class OrganizerAPI {

    public record PreviousScan(long date){}

    public record Scan(
            long event,
            String name,
            PaymentAPI.PurchasedTicketId id
    ){}

    public record ScanResult(
            AccountAPI.PublicUserInfo userinfo,
            EventAPI.Event event,
            TicketsAPI.Ticket ticket,
            boolean purchase_matches,
            List<PreviousScan> scans
    ){}

    @Route
    public static @Json ScanResult scan_ticket(@FromRequest(RequireOrganizer.class)UserSession auth, RwTransaction trans, @Body@Json Scan scan) throws SQLException, BadRequest {
        AccountAPI.PublicUserInfo info;
        try(var stmt = trans.namedPreparedStatement("select * from users where id=(select user_id from purchased_tickets where id=:pid)")){
            stmt.setLong(":pid", scan.id.pid());
            info = SqlSerde.sqlSingle(stmt.executeQuery(), AccountAPI.PublicUserInfo::make);
        }

        TicketsAPI.Ticket ticket;
        try(var stmt = trans.namedPreparedStatement("select * from tickets where id=(select ticket_id from purchased_tickets where id=:pid)")){
            stmt.setLong(":pid", scan.id.pid());
            ticket = SqlSerde.sqlSingle(stmt.executeQuery(), rs -> new TicketsAPI.Ticket(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getLong("price"),
                    SqlSerde.nullableLong(rs, "total_tickets")
            ));
        }

        EventAPI.Event event;
        try(var stmt = trans.namedPreparedStatement("select * from events where id=(select event_id from tickets where id=:ticket_id)")){
            stmt.setLong(":ticket_id", ticket.id());
            event = SqlSerde.sqlSingle(stmt.executeQuery(), rs -> new EventAPI.Event(rs, false));
        }

        boolean matches;
        try(var stmt = trans.namedPreparedStatement(
                "select coalesce(" +
                        "(select ticket_id from purchased_tickets where id=:id AND salt=:salt) " +
                        "IN " +
                        "(select id from tickets " +
                            "where event_id=:event_id AND " +
                                ":user_id IN (select owner_id from events E where E.id=event_id)" +
                        ")," +
                    "false)")){
            stmt.setLong(":id", scan.id.pid());
            stmt.setString(":salt", scan.id.salt());
            stmt.setLong(":event_id", scan.event);
            stmt.setLong(":user_id", auth.user_id);
            matches = SqlSerde.sqlSingle(stmt.executeQuery(), rs -> rs.getBoolean(1));
        }

        List<PreviousScan> scans = null;
        if(matches){
            try(var stmt = trans.namedPreparedStatement("insert into scanned_tickets values(:id, :time_scanned)")){
                stmt.setLong(":id", scan.id.pid());
                stmt.setLong(":time_scanned", System.currentTimeMillis());
                stmt.execute();
            }

            try(var stmt = trans.namedPreparedStatement("select time_scanned from scanned_tickets where purchased_ticket_id=:pid")){
                stmt.setLong(":pid", scan.id.pid());
                scans = SqlSerde.sqlList(stmt.executeQuery(), rs -> new PreviousScan(rs.getLong(1)));
            }
        }

        trans.commit();
        return new ScanResult(
                info,
                event,
                ticket,
                matches,
                scans
        );
    }
}
