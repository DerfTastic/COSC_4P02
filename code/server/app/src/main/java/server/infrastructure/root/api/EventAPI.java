package server.infrastructure.root.api;

import com.google.gson.JsonObject;
import framework.web.annotations.*;
import framework.web.error.BadRequest;
import server.infrastructure.DynamicMediaHandler;
import framework.db.RoTransaction;
import framework.db.RwTransaction;
import framework.web.annotations.url.Path;
import server.infrastructure.param.auth.OptionalAuth;
import server.infrastructure.param.auth.RequireOrganizer;
import server.infrastructure.param.auth.UserSession;
import framework.util.SqlSerde;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@Routes
public class EventAPI {
    @Route
    public static long create_event(@FromRequest(RequireOrganizer.class) UserSession session, RwTransaction trans) throws SQLException {
        long result;
        try(var stmt = trans.namedPreparedStatement("insert into events values(null, :organizer_id, '', '', null, null, null, null, null, true, null, null, null) returning id")){
            stmt.setLong(":organizer_id", session.organizer_id);
            result = stmt.executeQuery().getLong("id");
        }
        trans.commit();
        return result;
    }

    public static class Event{
            public long id;
            public long organizer_id;
            public String name;
            public String description;
            public long start;
            public long duration;
            public long picture;
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

        public EventTag(){}

        public EventTag(String tag, boolean category) {
            this.tag = tag;
            this.category = category;
        }
    }

    public record AllEvent(
            Event event,
            List<EventTag> tags
    ){}

    @Route("/get_event/<id>")
    public static @Json AllEvent get_event(@FromRequest(OptionalAuth.class) UserSession session, RoTransaction trans, @Path long id) throws SQLException, BadRequest {
        Event event;
        try(var stmt = trans.namedPreparedStatement("select * from events where id=:id AND (draft=false OR organizer_id=:organizer_id)")){
            stmt.setLong(":id", id);
            if(session!=null&&session.organizer_id!=null)
                stmt.setLong(":organizer_id", session.organizer_id);
            var result = SqlSerde.sqlList(stmt.executeQuery(), Event.class);
            if(result.isEmpty())
                throw new BadRequest("Event doesn't exist or you do not have permission to view it");
            if(result.size()>1)
                throw new SQLException("Expected single value found multiple");
            event = result.get(0);
        }

        List<EventTag> tags;
        try(var stmt = trans.namedPreparedStatement("select * from event_tags where event_id=:event_id")){
            stmt.setLong(":event_id", event.id);
            tags = SqlSerde.sqlList(stmt.executeQuery(), EventTag.class);
        }
        trans.commit();

        return new AllEvent(event, tags);
    }

    public record UpdateEvent(
            long id,
            String name,
            String description,
            long start,
            long duration,
            JsonObject metadata,
            int available_total_tickets,

            String location_name,
            double location_lat,
            double location_long
    ){}

    @Route
    public static void update_event(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Body @Json UpdateEvent update) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("update events set name=:name, description=:description, start=:start, duration=:duration, metadata=:metadata, available_total_tickets=:available_total_tickets, location_name=:location_name, location_lat=:location_lat, location_long=:location_long where id=:id AND organizer_id=:organizer_id")){
            stmt.setLong(":id", update.id);
            stmt.setLong(":organizer_id", session.organizer_id);

            stmt.setString(":name", update.name);
            stmt.setString(":description", update.description);
            stmt.setLong(":start", update.start);
            stmt.setLong(":duration", update.duration);
            stmt.setString(":metadata", update.metadata.toString());
            stmt.setInt(":available_total_tickets", update.available_total_tickets);
            stmt.setString(":location_name", update.location_name);
            stmt.setDouble(":location_lat", update.location_lat);
            stmt.setDouble(":location_long", update.location_long);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to update event");
        }
        trans.commit();
    }

    @Route("/delete_event/<id>")
    public static void delete_event(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long id) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("delete from events where id=:id AND organizer_id=:organizer_id")){
            stmt.setLong(":id", id);
            stmt.setLong(":organizer_id", session.organizer_id);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Could not delete event. Event doesn't exist or you don't own event");
        }
        trans.commit();
    }


    @Route("/add_event_tag/<id>/<tag>/<category>")
    public static void add_event_tag(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long id, @Path String tag, @Path boolean category) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("insert into event_tags values(:tag, :category, :id)")){
            stmt.setLong(":id", id);
            stmt.setString(":tag", tag);
            stmt.setBoolean(":category", category);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Could not add tag. Tag already exists or you do now own event");
        }
        trans.commit();
    }

    @Route("/remove_event_tag/<id>/<tag>/<category>")
    public static void remove_event_tag(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long id, @Path String tag, @Path boolean category) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("delete from event_tags where tag=:tag AND event_id=:event_id AND category=:category")){
            stmt.setLong(":id", id);
            stmt.setString(":tag", tag);
            stmt.setBoolean(":category", category);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Could not remove tag. Tag does not exist or you do now own event");
        }
        trans.commit();
    }

    @Route("/set_draft/<id>/<draft>")
    public static void set_draft(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long id, @Path boolean draft) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("update events set draft=:draft where id=:id AND organizer_id=:organizer_id")){
            stmt.setLong(":id", id);
            stmt.setLong(":organizer_id", session.organizer_id);
            stmt.setBoolean(":draft", draft);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Couldn't update the specified event. Either the event doesn't exist, or you don't control this event");
        }
        trans.commit();
    }

    @Route("/set_picture/<id>")
    public static long set_picture(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, DynamicMediaHandler handler, @Path long id, @Body byte[] data) throws SQLException, BadRequest {
        // 10 MiB
        if(data.length > (1<<20)*10){
            throw new BadRequest("File too large, maximum file size is 10 MiB");
        }

        try(var stmt = trans.namedPreparedStatement("select picture from events where id=:id AND organizer_id=:organizer_id")){
            stmt.setLong(":id", id);
            stmt.setLong(":organizer_id", session.organizer_id);
            var media = stmt.executeQuery().getLong(1);
            if(media!=0){
                Logger.getGlobal().log(Level.WARNING, "Picture deleted " + media);
                handler.delete(media);
            }
        }

        //TODO the database is locked here.. splitting the transaction into two may yield much better performance
        var media_id = handler.add(data);

        try(var stmt = trans.namedPreparedStatement("update events set picture=:picture where id=:id AND organizer_id=:organizer_id")){
            stmt.setLong(":id", id);
            stmt.setLong(":organizer_id", session.organizer_id);
            stmt.setLong(":picture", media_id);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to add picture to event. Event doesn't exit or you don't own event");
        }catch (Exception e){
            handler.delete(media_id);
            throw e;
        }
        trans.commit();
        return media_id;
    }
}
