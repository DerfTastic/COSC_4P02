package server.web;

import server.db.DbConnection;
import server.db.DbManager;
import server.db.Transaction;
import server.web.annotations.Body;
import server.web.annotations.FromRequest;
import server.web.annotations.Json;
import server.web.annotations.Route;
import server.web.annotations.url.Path;
import server.web.route.ClientError;
import server.web.route.RouteImpl;
import server.web.route.RouteParameter;
import util.SqlSerde;

import java.sql.SQLException;

@SuppressWarnings("unused")
public class DbAPI {

    public static final class Person {
        public int id;
        public String name;
    }

    @Route("/person/<id>")
    public static @Json Person person(Transaction trans, @Path int id) throws SQLException {
        try (var stmt = trans.conn.namedPreparedStatement("select * from person where id=:id")){
            stmt.setInt(":id",id);
            return SqlSerde.sqlSingle(stmt.executeQuery(), Person.class);
        }
    }

    @Route
    public static String sql(DbConnection connection, @Body String sql) throws SQLException {
        try(var stmt = connection.conn.createStatement()){
            if(!stmt.execute(sql))return "";
            String list = "";
            var rs = stmt.getResultSet();
            while(rs.next()){
                list += "(";
                for(int i = 1; ; i++){
                    try{
                        var res = rs.getString(i);
                        if(i!=1) list += ", ";
                        list += res;
                    }catch (SQLException ignore){break;}
                }
                list += ")\n";
            }
            return list;
        }
    }

    @Route
    public static @Json UserAuth test(@FromRequest(UserAuthFromRequest.class) UserAuth auth){
        return auth;
    }

    public static class UserAuth{
        public int id;
        public String email;
        public String pass;

        public int organizer_id;
        public int max_events;
        public boolean has_analytics;
    }

    public static class UserAuthFromRequest implements RouteParameter<UserAuth>{

        @Override
        public UserAuth construct(RouteImpl.Request request) throws Exception {
            var token = request.exchange.getRequestHeaders().getFirst("X-UserAPIToken");
            if(token==null)throw new ClientError.Unauthorized("No valid session");
            try(var conn = request.getServer().getManagedResource(DbManager.class).conn()){
                try(var stmt = conn.namedPreparedStatement("select * from users left join organizers on users.organizer_id=organizers.id where users.email=:email ")){
                    stmt.setString(":email", token);
                    System.out.println(token);
                    var result = stmt.executeQuery();
                    if(result==null||!result.next())throw new ClientError.Unauthorized("No valid session");

                    var auth = new UserAuth();
                    auth.id = result.getInt("id");
                    auth.email = result.getString("email");
                    auth.pass = result.getString("pass");

                    auth.organizer_id = result.getInt("organizer_id");
                    auth.max_events = result.getInt("max_events");
                    auth.has_analytics = result.getBoolean("has_analytics");
                    return auth;
                }
            }
        }
    }
}
