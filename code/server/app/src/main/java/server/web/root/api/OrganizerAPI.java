package server.web.root.api;

import server.db.RwTransaction;
import server.web.annotations.*;
import server.web.param.auth.RequireSession;
import server.web.param.auth.UserSession;
import server.web.route.ClientError;

import java.sql.SQLException;

@SuppressWarnings("unused")
@Routes
public class OrganizerAPI {

    @Route
    public static void convert_to_organizer_account(@FromRequest(RequireSession.class)UserSession session, RwTransaction trans) throws SQLException, ClientError.BadRequest {
        try(var stmt = trans.namedPreparedStatement("select organizer_id from users where id=:user_id")){
            stmt.setLong(":user_id", session.user_id);
            if(stmt.executeQuery().getInt("organizer_id")!=0)
                throw new ClientError.BadRequest("Account already an organizer");
        }

        long organizer_id;
        try(var stmt = trans.namedPreparedStatement("insert into organizers values(null, false) returning id")){
            organizer_id = stmt.executeQuery().getInt("id");
        }

        try(var stmt = trans.namedPreparedStatement("update users set organizer_id=:organizer_id where id=:user_id")){
            stmt.setLong(":organizer_id", organizer_id);
            stmt.setLong(":user_id", session.user_id);
            if(stmt.executeUpdate()!=1)
                throw new ClientError.BadRequest("Failed to make account an organizer");
        }
    }
}
