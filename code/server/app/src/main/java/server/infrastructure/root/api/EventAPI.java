package server.infrastructure.root.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import framework.web.annotations.*;
import framework.web.annotations.url.QueryFlag;
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
@SuppressWarnings("unused")
@Routes
public class EventAPI {
    @Route
    public static long create_event(@FromRequest(RequireOrganizer.class) UserSession session, RwTransaction trans) throws SQLException {
        long result;
        try(var stmt = trans.namedPreparedStatement("insert into events values(null, :owner_id, '', '', null, null, null, '', '', null, null, true, null, null, null) returning id")){
            stmt.setLong(":owner_id", session.user_id);
            result = stmt.executeQuery().getLong("id");
        }
        trans.commit();
        return result;
    }

    public static class Event{
            public final long id;
            public final long owner_id;
            public final String name;
            public final String description;
            public final long start;
            public final long duration;
            public final long release_time;
            public final String category;
            public final String type;
            public final long picture;
            public final JSONObject metadata;
            public final boolean draft;

            public final String location_name;
            public final double location_lat;
            public final double location_long;

            public final List<String> tags;
            public final AccountAPI.PublicUserInfo owner;

            public Event(ResultSet rs, boolean read_user) throws SQLException {
                this.id = rs.getLong("id");
                this.owner_id = rs.getLong("owner_id");
                this.name = rs.getString("name");
                this.description = rs.getString("description");
                this.start = rs.getLong("start");
                this.duration = rs.getLong("duration");
                this.category = rs.getString("category");
                this.type = rs.getString("type");
                this.picture = rs.getLong("picture");
                this.metadata = Optional.ofNullable(rs.getString("metadata")).map(JSON::parseObject).orElse(null);
                this.release_time = rs.getLong("release_time");
                this.draft = rs.getBoolean("draft");
                this.location_name = rs.getString("location_name");
                this.location_lat = rs.getDouble("location_lat");
                this.location_long = rs.getDouble("location_long");
                this.tags = new ArrayList<>();
                this.owner = read_user? AccountAPI.PublicUserInfo.make(rs):null;
            }
    }

    @Route("/get_event/<event_id>")
    public static @Json Event get_event(@FromRequest(OptionalAuth.class) UserSession session, RoTransaction trans, @Path long event_id, @QueryFlag boolean with_owner) throws SQLException, BadRequest {
        Event event;
        var sql = "select * from events";
        if(with_owner) sql += " inner join users on users.id=events.owner_id";
        sql += " where (events.id=:event_id AND events.draft=false) OR (events.id=:event_id AND events.owner_id=:owner_id)";
        try(var stmt = trans.namedPreparedStatement(sql)){
            stmt.setLong(":event_id", event_id);
            stmt.setLong(":owner_id", session!=null?session.user_id:0);
            var result = SqlSerde.sqlList(stmt.executeQuery(), rs -> new Event(rs, with_owner));
            if(result.isEmpty())
                throw new BadRequest("Event doesn't exist or you do not have permission to view it");
            if(result.size()>1)
                throw new SQLException("Expected single value found multiple");
            event = result.getFirst();
        }

        try(var stmt = trans.namedPreparedStatement("select * from event_tags where event_id=:event_id")){
            stmt.setLong(":event_id", event.id);
            SqlSerde.sqlForEach(stmt.executeQuery(), rs -> event.tags.add(rs.getString("tag")));
        }
        trans.commit();

        return event;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class UpdateEvent{
        public Optional<String> name;
        public Optional<String> description;
        public Optional<JSONObject> metadata;

        public Optional<String> type;
        public Optional<String> category;

        public Optional<Long> start;
        public Optional<Long> duration;
        public Optional<Long> release_time;

        public Optional<String> location_name;
        public Optional<Double> location_lat;
        public Optional<Double> location_long;

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

        public UpdateEvent(){}

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
                    case "name" -> this.name = optionalString(reader);
                    case "description" -> this.description = optionalString(reader);
                    case "metadata" -> this.metadata = optionalObject(reader);
                    case "type" -> this.type = optionalString(reader);
                    case "category" -> this.category = optionalString(reader);
                    case "start" -> this.start = optionalLong(reader);
                    case "duration" -> this.duration = optionalLong(reader);
                    case "release_time" -> this.release_time = optionalLong(reader);
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
    @Route("/update_event/<id>")
    public static void update_event(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long id, @FromRequest(UpdateEventFromRequest.class) UpdateEvent update) throws SQLException, BadRequest {
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
        if(update.release_time!=null)
            str.append("release_time=:release_time,");
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
            stmt.setLong(":id", id);
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
            if(update.release_time!=null&&update.release_time.isPresent())
                stmt.setLong(":release_time", update.release_time.get());
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


    @Route("/add_event_tag/<event_id>/<tag>")
    public static void add_event_tag(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long event_id, @Path String tag) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("select owner_id from events where id=:event_id")){
            stmt.setLong(":event_id", event_id);
            if(stmt.executeQuery().getLong("owner_id")!=session.user_id)
                throw new BadRequest("Could not add tag. Tag already exists or you do now own event");
        }
        try(var stmt = trans.namedPreparedStatement("insert into event_tags values(:tag, :event_id)")){
            stmt.setLong(":event_id", event_id);
            stmt.setString(":tag", tag);
            try{
                if(stmt.executeUpdate()!=1)
                    throw new BadRequest("Could not add tag. Tag already exists or you do now own event");
            }catch (SQLException e){
                throw new BadRequest("Could not add tag. Tag already exists or you do now own event");
            }
        }
        trans.commit();
    }

    @Route("/delete_event_tag/<event_id>/<tag>")
    public static void delete_event_tag(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, @Path long event_id, @Path String tag) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("select owner_id from events where id=:event_id")){
            stmt.setLong(":event_id", event_id);
            if(stmt.executeQuery().getLong("owner_id")!=session.user_id)
                throw new BadRequest("Could not add tag. Tag already exists or you do now own event");
        }
        try(var stmt = trans.namedPreparedStatement("delete from event_tags where tag=:tag AND event_id=:event_id")){
            stmt.setLong(":event_id", event_id);
            stmt.setString(":tag", tag);
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

    @Route("/set_event_picture/<id>")
    public static long set_event_picture(@FromRequest(RequireOrganizer.class)UserSession session, RwTransaction trans, DynamicMediaHandler handler, @Path long id, @Body byte[] data) throws SQLException, BadRequest {
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
            throw e;
        }
        if(old_media!=0){
            handler.delete(old_media);
        }

        return media_id;
    }
}
