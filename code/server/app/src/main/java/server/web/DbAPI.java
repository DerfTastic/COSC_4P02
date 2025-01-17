package server.web;

import server.db.DbConnection;
import server.db.Transaction;
import server.web.annotations.Body;
import server.web.annotations.Json;
import server.web.annotations.Route;
import server.web.annotations.url.Path;
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
                        if(i!=1) list += ", ";
                        list += rs.getString(i);
                    }catch (SQLException ignore){break;}
                }
                list += ")\n";
            }
            return list;
        }
    }
}
