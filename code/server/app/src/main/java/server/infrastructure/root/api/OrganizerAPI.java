package server.infrastructure.root.api;

import framework.db.RwTransaction;
import framework.web.annotations.FromRequest;
import framework.web.annotations.Route;
import framework.web.annotations.Routes;
import framework.web.error.BadRequest;
import server.infrastructure.param.auth.RequireSession;
import server.infrastructure.param.auth.UserSession;

import java.sql.SQLException;

@SuppressWarnings("unused")
@Routes
public class OrganizerAPI {

    @Route
    public static void convert_to_organizer_account(@FromRequest(RequireSession.class)UserSession session, RwTransaction trans) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("select organizer_id from users where id=:user_id")){
            stmt.setLong(":user_id", session.user_id);
            if(stmt.executeQuery().getInt("organizer_id")!=0)
                throw new BadRequest("Account already an organizer");
        }

        long organizer_id;
        try(var stmt = trans.namedPreparedStatement("insert into organizers values(null, false) returning id")){
            organizer_id = stmt.executeQuery().getInt("id");
        }

        try(var stmt = trans.namedPreparedStatement("update users set organizer_id=:organizer_id where id=:user_id")){
            stmt.setLong(":organizer_id", organizer_id);
            stmt.setLong(":user_id", session.user_id);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to make account an organizer");
        }
    }
}
