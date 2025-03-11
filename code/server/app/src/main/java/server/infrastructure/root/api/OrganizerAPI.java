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
    public static void convert_to_organizer_account(@FromRequest(RequireSession.class)UserSession auth, RwTransaction trans) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("select organizer from users where id=:user_id")){
            stmt.setLong(":user_id", auth.user_id);
            if(stmt.executeQuery().getBoolean("organizer"))
                throw new BadRequest("Account already an organizer");
        }
        try(var stmt = trans.namedPreparedStatement("update users set organizer=true where id=:user_id")){
            stmt.setLong(":user_id", auth.user_id);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to set user organizer");
        }
        // this is a stupid hack to make caching function
        try(var stmt = trans.namedPreparedStatement("update sessions set id=id where user_id=:id")){
            stmt.setLong(":id", auth.user_id);
            stmt.execute();
        }
        trans.commit();
    }
}
