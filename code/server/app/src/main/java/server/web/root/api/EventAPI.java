package server.web.root.api;

import com.google.gson.JsonObject;
import server.DynamicMediaHandler;
import server.db.DbManager;
import server.db.RoTransaction;
import server.db.RwTransaction;
import server.web.annotations.*;
import server.web.annotations.url.Path;
import server.web.auth.RequireOrganizer;
import server.web.auth.UserSession;
import server.web.route.ClientError;
import server.web.route.Request;
import server.web.route.RouteParameter;
import util.SqlSerde;

import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("unused")
@Routes
public class EventAPI {
    @Route
    public static int create_event(@FromRequest(RequireOrganizer.class) UserSession session, RwTransaction trans) throws SQLException {
        try(var stmt = trans.namedPreparedStatement("insert into events values(null, :organizer_id, '', '', null, null, null, true, null, null, null) returning id")){
            stmt.setInt(":organizer_id", session.organizer_id);
            return stmt.executeQuery().getInt("id");
        }
    }

    public static class Event{
            public int id;
            public int organizer_id;
            public String name;
            public String description;
            public int picture;
            public JsonObject metadata;
            public int available_total_tickets;
            public boolean draft;

            public String location_name;
            public double location_lat;
            public double location_long;
    }

    public static class EventTag{
        public String tag;
        public boolean category;
    }

    public record AllEvent(
            Event event,
            List<EventTag> tags
    ){}

    public static class OptionalAuth implements RouteParameter<Integer>{
        @Override
        public Integer construct(Request request) throws Exception {
            var token = request.exchange.getRequestHeaders().getFirst("X-UserAPIToken");
            if (token == null) return null;
            if(token.isEmpty()) return null;
            try (var conn = request.getServer().getManagedResource(DbManager.class).ro_conn()) {
                try (var stmt = conn.namedPreparedStatement("select organizer_id from sessions left join users on sessions.user_id=users.id left join organizers on users.organizer_id=organizers.id where sessions.token=:token")) {
                    stmt.setString(":token", token);
                    var result = stmt.executeQuery();
                    if (result == null || !result.next()) return null;
                    return result.getInt("organizer_id");
                }
            }
        }
    }

    @Route("/get_event/<id>")
    public static @Json AllEvent get_event(@FromRequest(OptionalAuth.class) Integer organizer_id, RoTransaction trans, @Path int id) throws SQLException, ClientError.BadRequest {
        Event event;
        try(var stmt = trans.namedPreparedStatement("select * from events where id=:id AND (draft=false OR organizer_id=:organizer_id)")){
            stmt.setInt(":id", id);
            if(organizer_id!=null)
                stmt.setInt(":organizer_id", organizer_id);
            var result = SqlSerde.sqlList(stmt.executeQuery(), Event.class);
            if(result.isEmpty())
                throw new ClientError.BadRequest("Event doesn't exist or you do not have permission to view it");
            if(result.size()>1)
                throw new SQLException("Expected single value found multiple");
            event = result.get(0);
        }

        List<EventTag> tags;
        try(var stmt = trans.namedPreparedStatement("select * from event_tags where event_id=:event_id")){
            stmt.setInt(":event_id", event.id);
            tags = SqlSerde.sqlList(stmt.executeQuery(), EventTag.class);
        }

        return new AllEvent(event, tags);
    }

    public record UpdateEvent(
            int id,
            String name,
            String description,
            JsonObject metadata,
            int available_total_tickets,

            String location_name,
            double location_lat,
            double location_long
    ){}

    @Route
    public static void update_event(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Body @Json UpdateEvent update) throws SQLException, ClientError.BadRequest {
        try(var stmt = trans.namedPreparedStatement("update events set name=:name, description=:description, metadata=:metadata, available_total_tickets=:available_total_tickets, location_name=:location_name, location_lat=:location_lat, location_long=:location_long where id=:id AND organizer_id=:organizer_id")){
            stmt.setInt(":id", update.id);
            stmt.setInt(":organizer_id", session.organizer_id);

            stmt.setString(":name", update.name);
            stmt.setString(":description", update.description);
            stmt.setString(":metadata", update.metadata.toString());
            stmt.setInt(":available_total_tickets", update.available_total_tickets);
            stmt.setString(":location_name", update.location_name);
            stmt.setDouble(":location_lat", update.location_lat);
            stmt.setDouble(":location_long", update.location_long);
            if(stmt.executeUpdate()!=1)
                throw new ClientError.BadRequest("Failed to update event");
        }
    }

    @Route("/delete_event/<id>")
    public static void delete_event(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path int id) throws SQLException, ClientError.BadRequest {
        try(var stmt = trans.namedPreparedStatement("delete from events where id=:id AND organizer_id=:organizer_id")){
            stmt.setInt(":id", id);
            stmt.setInt(":organizer_id", session.organizer_id);
            if(stmt.executeUpdate()!=1)
                throw new ClientError.BadRequest("Could not delete event. Event doesn't exist or you don't own event");
        }
    }


    @Route("/add_event_tag/<id>/<tag>/<category>")
    public static void add_event_tag(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path int id, @Path String tag, @Path boolean category) throws SQLException, ClientError.BadRequest {
        try(var stmt = trans.namedPreparedStatement("insert into event_tags values(:tag, :category, :id)")){
            stmt.setInt(":id", id);
            stmt.setString(":tag", tag);
            stmt.setBoolean(":category", category);
            if(stmt.executeUpdate()!=1)
                throw new ClientError.BadRequest("Could not add tag. Tag already exists or you do now own event");
        }
    }

    @Route("/remove_event_tag/<id>/<tag>/<category>")
    public static void remove_event_tag(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path int id, @Path String tag, @Path boolean category) throws SQLException, ClientError.BadRequest {
        try(var stmt = trans.namedPreparedStatement("delete from event_tags where tag=:tag AND event_id=:event_id AND category=:category")){
            stmt.setInt(":id", id);
            stmt.setString(":tag", tag);
            stmt.setBoolean(":category", category);
            if(stmt.executeUpdate()!=1)
                throw new ClientError.BadRequest("Could not remove tag. Tag does not exist or you do now own event");
        }
    }

    @Route("/set_draft/<id>/<draft>")
    public static void set_draft(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path int id, @Path boolean draft) throws SQLException, ClientError.BadRequest {
        try(var stmt = trans.namedPreparedStatement("update events set draft=:draft where id=:id AND organizer_id=:organizer_id")){
            stmt.setInt(":id", id);
            stmt.setInt(":organizer_id", session.organizer_id);
            stmt.setBoolean(":draft", draft);
            if(stmt.executeUpdate()!=1)
                throw new ClientError.BadRequest("Couldn't update the specified event. Either the event doesn't exist, or you don't control this event");
        }
    }

    @Route("/set_picture/<id>")
    public static int set_picture(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, DynamicMediaHandler handler, @Path int id, @Body byte[] data) throws SQLException, ClientError.BadRequest {
        try(var stmt = trans.namedPreparedStatement("select picture from events where id=:id AND organizer_id=:organizer_id")){
            stmt.setInt(":id", id);
            stmt.setInt(":organizer_id", session.organizer_id);
            var media = stmt.executeQuery().getInt(1);
            if(media!=0)
                handler.delete(media);
        }

        var media_id = handler.add(data);

        try(var stmt = trans.namedPreparedStatement("update events set picture=:picture where id=:id AND organizer_id=:organizer_id")){
            stmt.setInt(":id", id);
            stmt.setInt(":organizer_id", session.organizer_id);
            stmt.setInt(":picture", media_id);
            if(stmt.executeUpdate()!=1)
                throw new ClientError.BadRequest("Failed to add picture to event. Event doesn't exit or you don't own event");
        }catch (Exception e){
            handler.delete(media_id);
            throw e;
        }
        return media_id;
    }
}
