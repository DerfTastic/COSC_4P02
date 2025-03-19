package server.infrastructure.root.api;

import framework.db.RoTransaction;
import framework.web.annotations.*;
import server.infrastructure.param.auth.OptionalAuth;
import server.infrastructure.param.auth.UserSession;
import framework.util.SqlSerde;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
@Routes
public class SearchAPI {

    public enum SortBy{
        MinPrice,
        MaxPrice,
        TicketsAvailable,
        Closest,
        StartTime,
        MinDuration,
        MaxDuration,
        Nothing,
    }

    public record Search(
            Long date_start,
            Long date_end,
            Long max_duration,
            Long min_duration,
            List<String> tags,
            String category_fuzzy,
            String type_fuzzy,
            String organizer_fuzzy,
            String name_fuzzy,
            Long organizer_exact,
            Double distance,
            Double location_lat,
            Double location_long,
            String location,
            Boolean owning,
            Boolean draft,
            Long offset,
            Long limit,
            SortBy sort_by
    ){}

    @Route
    public static @Json List<EventAPI.Event> search_events(@FromRequest(OptionalAuth.class) UserSession session, RoTransaction trans, @Body @Json Search search) throws SQLException {
        var long_map = new HashMap<String, Long>();
        var str_map = new HashMap<String, String>();
        var real_map = new HashMap<String, Double>();

        StringBuilder whereClause = new StringBuilder();
        if(search.draft==null|| !search.draft) {
            if (search.owning == null || !search.owning) {
                whereClause.append("draft=false");
            } else {
                whereClause.append("draft=false AND owner_id=:owner_id");
                long_map.put(":owner_id", session==null?0:session.user_id);
            }
        }else {
            whereClause.append("draft=true AND owner_id=:owner_id");
            long_map.put(":owner_id", session==null?0:session.user_id);
        }

        if(search.tags!=null) {
            int index = 0;
            for (var tag : search.tags) {
                String id = ":tag_id_" + index;
                whereClause.append(" AND (events.id IN (select event_id from event_tags where tag=").append(id).append("'))");
                str_map.put(id, tag);
                index ++;
            }
        }
        if(search.type_fuzzy!=null){
            whereClause.append(" AND type LIKE :type_fuzzy");
            str_map.put(":type_fuzzy", search.type_fuzzy);
        }
        if(search.category_fuzzy!=null){
            whereClause.append(" AND category LIKE :category_fuzzy");
            str_map.put(":category_fuzzy", search.category_fuzzy);
        }
        if(search.distance!=null && search.location_lat!=null && search.location_long!=null){
            whereClause.append(" AND (location_lat BETWEEN :lat_lower AND :lat_upper)");
            whereClause.append(" AND (location_long BETWEEN :long_lower AND :long_upper)");

            real_map.put(":lat_lower", search.location_lat - search.distance);
            real_map.put(":lat_upper", search.location_lat + search.distance);

            real_map.put(":long_lower", search.location_lat - search.distance);
            real_map.put(":long_upper", search.location_lat + search.distance);
        }
        if(search.date_start!=null){
            whereClause.append(" AND (start >= :date_start OR start IS NULL)");
            long_map.put(":date_start", search.date_start);
        }
        if(search.date_end!=null){
            whereClause.append(" AND (start <= :date_end OR start IS NULL)");
            long_map.put(":date_end", search.date_end);
        }
        if(search.max_duration!=null){
            whereClause.append(" AND (duration <= :max_duration OR duration IS NULL)");
            long_map.put(":max_duration", search.max_duration);
        }
        if(search.min_duration!=null){
            whereClause.append(" AND (duration >= :min_duration OR duration IS NULL)");
            long_map.put(":min_duration", search.min_duration);
        }
        if(search.organizer_exact!=null){
            whereClause.append(" AND (owner_id=:organizer_exact)");
            long_map.put(":organizer_exact", search.organizer_exact);
        }
        if(search.organizer_fuzzy!=null){
            whereClause.append(" AND owner_id IN (select users.id from users where events.owner_id=users.id AND users.name LIKE :organizer_name_fuzzy)");
            str_map.put(":organizer_name_fuzzy", search.organizer_fuzzy);
        }
        if(search.name_fuzzy!=null){
            whereClause.append(" AND (name LIKE :event_name_fuzzy)");
            str_map.put(":event_name_fuzzy", search.name_fuzzy);
        }
        if(search.location!=null){
            whereClause.append(" AND (location_name LIKE :location_fuzzy)");
            str_map.put(":location_fuzzy", search.location);
        }

        String order = switch(search.sort_by==null?SortBy.Nothing:search.sort_by){
            case MinPrice -> "(select min(price) from tickets where event_id=id) ASC";
            case MaxPrice -> "(select max(price) from tickets where event_id=id) DESC";
            case TicketsAvailable -> "(coalesce(coalesce((select sum(available_tickets) from tickets where tickets.event_id=events.id),events.available_total_tickets)-(select count(*) from purchased_tickets where purchased_tickets.ticket_id in (select tickets.id from tickets where tickets.event_id=events.id)),999999999)) DESC";
            case Closest -> {
                real_map.put(":location_lat", search.location_lat);
                real_map.put(":location_long", search.location_long);
                yield "abs(location_lat-:location_lat) ASC, abs(location_long-:location_long) ASC";
            }
            case StartTime -> "start ASC";
            case MinDuration -> "duration ASC";
            case MaxDuration -> "duration DESC";
            case Nothing -> "";
        };
        if(!order.isBlank()) order += ",";
        order += " id ASC";

        String clauses = "where " + whereClause + " order by " + order;
        var limit = search.limit==null?256:Math.max(256, search.limit);
        var offset = search.offset==null?0:search.offset;
        clauses += " limit :offset, :limit";
        long_map.put(":offset", offset);
        long_map.put(":limit", limit);

        List<EventAPI.Event> events;
        try(var stmt = trans.namedPreparedStatement("select * from events inner join users on users.id=events.owner_id " + clauses)){
            for(var es : str_map.entrySet()){
                stmt.setString(es.getKey(), es.getValue());
            }
            for(var es : long_map.entrySet()){
                stmt.setLong(es.getKey(), es.getValue());
            }
            for(var es : real_map.entrySet()){
                stmt.setDouble(es.getKey(), es.getValue());
            }
            var rs = stmt.executeQuery();
            events = SqlSerde.sqlList(rs, rs1 -> new EventAPI.Event(rs, false));
        }

        try(var stmt = trans.namedPreparedStatement("select id, tag, category from events left join event_tags on id=event_id " + clauses)){
            for(var es : str_map.entrySet()){
                stmt.setString(es.getKey(), es.getValue());
            }
            for(var es : long_map.entrySet()){
                stmt.setLong(es.getKey(), es.getValue());
            }
            for(var es : real_map.entrySet()){
                stmt.setDouble(es.getKey(), es.getValue());
            }
            var rs = stmt.executeQuery();

            int index = 0;
            while(rs.next()){
                var id = rs.getLong("id");
                var tag = rs.getString("tag");
                while(events.get(index).id!=id)index++;
                events.get(index).tags.add(tag);
            }
        }
        trans.commit();

        return events;
    }
}
