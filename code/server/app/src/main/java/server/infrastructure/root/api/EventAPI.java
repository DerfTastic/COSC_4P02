package server.infrastructure.root.api;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import framework.web.annotations.*;
import framework.web.error.BadRequest;
import framework.web.request.Request;
import framework.web.route.RouteParameter;
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
import java.util.Optional;

@SuppressWarnings("unused")
@Routes
public class EventAPI {
    @Route
    public static long create_event(@FromRequest(RequireOrganizer.class) UserSession session, RwTransaction trans) throws SQLException {
        long result;
        try(var stmt = trans.namedPreparedStatement("insert into events values(null, :owner_id, '', '', null, null, '', '', null, null, null, true, null, null, null) returning id")){
            stmt.setLong(":owner_id", session.user_id);
            result = stmt.executeQuery().getLong("id");
        }
        trans.commit();
        return result;
    }

    public static class Event{
            public long id;
            public long owner_id;
            public String name;
            public String description;
            public long start;
            public long duration;
            public String category;
            public String type;
            public long picture;
            public JSONObject metadata;
            public int available_total_tickets;
            public boolean draft;

            public String location_name;
            public double location_lat;
            public double location_long;
    }

    public static class EventTag{
        public String tag;

        public EventTag(){}

        public EventTag(String tag) {
            this.tag = tag;
        }
    }

    public record AllEvent(
            Event event,
            List<EventTag> tags
    ){}

    @Route("/get_event/<id>")
    public static @Json AllEvent get_event(@FromRequest(OptionalAuth.class) UserSession session, RoTransaction trans, @Path long id) throws SQLException, BadRequest {
        Event event;
        try(var stmt = trans.namedPreparedStatement("select * from events where (id=:id AND draft=false) OR (id=:id AND owner_id=:owner_id)")){
            stmt.setLong(":id", id);
            if(session!=null)
                stmt.setLong(":owner_id", session.user_id);
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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class UpdateEvent{

        long id;
        Optional<String> name;
        Optional<String> description;
        Optional<JSONObject> metadata;

        Optional<String> type;
        Optional<String> category;

        Optional<Long> start;
        Optional<Long> duration;

        Optional<Long> available_total_tickets;

        Optional<String> location_name;
        Optional<Double> location_lat;
        Optional<Double> location_long;

        private static long requireInt64(JSONReader reader){
            if(reader.isNumber())
                return reader.readInt64Value();
            else
                throw new RuntimeException("Expected int64 value");
        }

        private static Optional<String> optionalString(JSONReader reader){
            if(reader.nextIfNull())
                return Optional.empty();
            if(reader.isString())
                return Optional.of(reader.readString());
            else
                throw new RuntimeException("Expected String value");
        }

        private static Optional<Long> optionalLong(JSONReader reader){
            if(reader.nextIfNull())
                return Optional.empty();
            if(reader.isNumber())
                return Optional.of(reader.readInt64());
            else
                throw new RuntimeException("Expected int64 value");
        }

        private static Optional<Double> optionalDouble(JSONReader reader){
            if(reader.nextIfNull())
                return Optional.empty();
            if(reader.isNumber())
                return Optional.of(reader.readDouble());
            else
                throw new RuntimeException("Expected double value");
        }

        private static Optional<JSONObject> optionalObject(JSONReader reader){
            if(reader.nextIfNull())
                return Optional.empty();
            if(reader.isObject())
                return Optional.of(reader.readJSONObject());
            else
                throw new RuntimeException("Expected object value");
        }

        public UpdateEvent(JSONReader reader) throws BadRequest {
            if(!reader.nextIfObjectStart())
                throw new BadRequest("Expected an object");
            while(!reader.nextIfObjectEnd()){
                if(!reader.isString())
                    throw new BadRequest("Expected field");
                var field_name = reader.readString();
                if(!reader.nextIfMatch(':'))
                    throw new BadRequest("Expected colon");
                switch(field_name){
                    case "id" -> this.id = requireInt64(reader);
                    case "name" -> this.name = optionalString(reader);
                    case "description" -> this.description = optionalString(reader);
                    case "metadata" -> this.metadata = optionalObject(reader);
                    case "type" -> this.type = optionalString(reader);
                    case "category" -> this.category = optionalString(reader);
                    case "start" -> this.start = optionalLong(reader);
                    case "duration" -> this.duration = optionalLong(reader);
                    case "available_total_tickets" -> this.available_total_tickets = optionalLong(reader);
                    case "location_name" -> this.location_name = optionalString(reader);
                    case "location_lat" -> this.location_lat = optionalDouble(reader);
                    case "location_long" -> this.location_long = optionalDouble(reader);
                    default -> throw new BadRequest("Unknown field: " + field_name);
                }
            }
        }
    }

    public static class UpdateEventFromRequest implements RouteParameter<UpdateEvent>{
        @Override
        public UpdateEvent construct(Request request) throws Exception {
            try(var reader = JSONReader.of(request.exchange.getRequestBody().readAllBytes())){
                return new UpdateEvent(reader);
            }
        }
    }


    @SuppressWarnings("OptionalAssignedToNull")
    @Route
    public static void update_event(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @FromRequest(UpdateEventFromRequest.class) UpdateEvent update) throws SQLException, BadRequest {
        var str = new StringBuilder().append("update events set ");

        if(update.name!=null)
            str.append("name=:name,");
        if(update.description!=null)
            str.append("description=:description,");
        if(update.category!=null)
            str.append("category=:category,");
        if(update.type!=null)
            str.append("type=:type,");
        if(update.start!=null)
            str.append("start=:start,");
        if(update.duration!=null)
            str.append("duration=:duration,");
        if(update.metadata!=null)
            str.append("metadata=:metadata,");
        if(update.available_total_tickets!=null)
            str.append("available_total_tickets=:available_total_tickets,");
        if(update.location_name!=null)
            str.append("location_name=:location_name,");
        if(update.location_lat!=null)
            str.append("location_lat=:location_lat,");
        if(update.location_long!=null)
            str.append("location_long=:location_long,");

        if(str.charAt(str.length()-1)!=',') return;
        str.deleteCharAt(str.length()-1);
        str.append(" where id=:id AND owner_id=:owner_id");

        try(var stmt = trans.namedPreparedStatement(str.toString())){
            stmt.setLong(":id", update.id);
            stmt.setLong(":owner_id", session.user_id);

            if(update.name!=null&&update.name.isPresent())
                stmt.setString(":name", update.name.get());
            if(update.description!=null&&update.description.isPresent())
                stmt.setString(":description", update.description.get());
            if(update.category!=null&&update.category.isPresent())
                stmt.setString(":category", update.category.get());
            if(update.type!=null&&update.type.isPresent())
                stmt.setString(":type", update.type.get());
            if(update.start!=null&&update.start.isPresent())
                stmt.setLong(":start", update.start.get());
            if(update.duration!=null&&update.duration.isPresent())
                stmt.setLong(":duration", update.duration.get());
            if(update.metadata!=null&&update.metadata.isPresent())
                stmt.setString(":metadata", update.metadata.get().toJSONString());
            if(update.available_total_tickets!=null&&update.available_total_tickets.isPresent())
                stmt.setLong(":available_total_tickets", update.available_total_tickets.get());
            if(update.location_name!=null&&update.location_name.isPresent())
                stmt.setString(":location_name", update.location_name.get());
            if(update.location_lat!=null&&update.location_lat.isPresent())
                stmt.setDouble(":location_lat", update.location_lat.get());
            if(update.location_long!=null&&update.location_long.isPresent())
                stmt.setDouble(":location_long", update.location_long.get());

            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to update event");
        }
        trans.commit();
    }

    @Route("/delete_event/<id>")
    public static void delete_event(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long id, DynamicMediaHandler media) throws SQLException, BadRequest {
        long picture;
        try(var stmt = trans.namedPreparedStatement("delete from events where id=:id AND owner_id=:owner_id returning picture")){
            stmt.setLong(":id", id);
            stmt.setLong(":owner_id", session.user_id);
            var rs = stmt.executeQuery();
            if(!rs.next())
                throw new BadRequest("Could not delete event. Event doesn't exist or you don't own event");
            picture = rs.getLong("picture");
            if(rs.next())
                throw new BadRequest("Multiple events with the same id????? uhhh");
        }
        trans.commit();

        if(picture!=0)
            media.delete(picture);
    }


    @Route("/add_event_tag/<id>/<tag>/<category>")
    public static void add_event_tag(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long id, @Path String tag, @Path boolean category) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("insert into event_tags values(:tag, :category, :id)")){
            stmt.setLong(":id", id);
            stmt.setString(":tag", tag);
            stmt.setBoolean(":category", category);
            try{
                if(stmt.executeUpdate()!=1)
                    throw new BadRequest("Could not add tag. Tag already exists or you do now own event");
            }catch (SQLException e){
                throw new BadRequest("Could not add tag. Tag already exists or you do now own event");
            }
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
        try(var stmt = trans.namedPreparedStatement("update events set draft=:draft where id=:id AND owner_id=:owner_id")){
            stmt.setLong(":id", id);
            stmt.setLong(":owner_id", session.user_id);
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

        var media_id = handler.add(data);
        long old_media = 0;
        try{
            try(var stmt = trans.namedPreparedStatement("select picture from events where id=:id AND owner_id=:owner_id")){
                stmt.setLong(":id", id);
                stmt.setLong(":owner_id", session.user_id);
                old_media = stmt.executeQuery().getLong(1);
            }

            try(var stmt = trans.namedPreparedStatement("update events set picture=:picture where id=:id AND owner_id=:owner_id")){
                stmt.setLong(":id", id);
                stmt.setLong(":owner_id", session.user_id);
                stmt.setLong(":picture", media_id);
                if(stmt.executeUpdate()!=1)
                    throw new BadRequest("Failed to add picture to event. Event doesn't exit or you don't own event");
            }
            trans.commit();
        }catch (Exception e){
            trans.commit();
            if(media_id!=0){
                handler.delete(media_id);
            }
        }
        if(old_media!=0){
            handler.delete(old_media);
        }

        return media_id;
    }
}
