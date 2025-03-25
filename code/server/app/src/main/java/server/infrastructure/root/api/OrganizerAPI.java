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
    public static @Json List<Scan> scan_ticket(@FromRequest(RequireOrganizer.class)UserSession auth, RwTransaction trans, @Body@Json Scan scan) throws SQLException {
        try(var stmt = trans.namedPreparedStatement("insert into ")){

        }
        return null;
    }
}
