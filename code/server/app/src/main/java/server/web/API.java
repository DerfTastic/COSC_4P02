package server.web;

import server.db.Transaction;
import server.web.annotations.Json;
import server.web.annotations.Route;
import server.web.annotations.url.Path;
import util.SqlSerde;

import java.sql.SQLException;



public class API {

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
}
