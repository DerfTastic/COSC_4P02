package server.infrastructure.root.api;

import framework.db.RwTransaction;
import framework.util.SqlSerde;
import framework.web.annotations.*;
import framework.web.annotations.url.Nullable;
import framework.web.error.BadRequest;
import server.infrastructure.param.auth.RequireOrganizer;
import server.infrastructure.param.auth.RequireSession;
import server.infrastructure.param.auth.SessionCache;
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

    @Route
    public static @Json List<PreviousScan> scan_ticket(@FromRequest(RequireOrganizer.class)UserSession auth, RwTransaction trans, @Body@Json Scan scan) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("select coalesce((select ticket_id from purchased_tickets where id=:purchased_ticket_id AND salt=:salt) IN (select id from tickets where event_id=:event_id AND owner_id=:user_id),false)")){
            stmt.setLong(":purchased_ticket_id", scan.id.id());
            stmt.setString(":salt", scan.id.salt());
            stmt.setLong(":event_id", scan.event);
            stmt.setLong(":user_id", auth.user_id);
            if(!SqlSerde.sqlSingle(stmt.executeQuery(), rs -> rs.getBoolean(1)))
                throw new BadRequest();
        }

        try(var stmt = trans.namedPreparedStatement("insert into scanned_tickets values(:purchased_ticket, :time_scanned)")){
            stmt.setLong(":purchased_ticket", scan.id.id());
            stmt.setLong(":time_scanned", System.currentTimeMillis());
            stmt.execute();
        }

        List<PreviousScan> scans;
        try(var stmt = trans.namedPreparedStatement("select time_scanned from scanned_tickets where purchased_ticket=:purchased_ticket")){
            stmt.setLong(":purchased_ticket", scan.id.id());
            scans = SqlSerde.sqlList(stmt.executeQuery(), rs -> new PreviousScan(rs.getLong(1)));
        }

        trans.commit();
        return scans;
    }
}
