package server.web.root.api;

import server.db.RoTransaction;
import server.web.annotations.*;
import server.web.param.auth.OptionalAuth;
import server.web.param.auth.UserSession;
import util.SqlSerde;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Routes
public class SearchAPI {

    public record Search(
            Long date_start,
            Long date_end,
            Long max_duration,
            Long min_duration,
            List<EventAPI.EventTag> tags,
            String organizer_fuzzy,
            String name_fizzy,
            Integer organizer_exact,
            Double distance,
            Double location_lat,
            Double location_long,
            String location,
            Integer offset,
            Integer limit
    ){}

    @Route
    public static @Json List<EventAPI.AllEvent> search_events(@FromRequest(OptionalAuth.class) UserSession session, RoTransaction trans, @Body @Json Search search) throws SQLException {
        String whereClause = "(draft=false OR organizer_id=:organizer_id)";
        if(search.distance!=null && search.location_lat!=null && search.location_long!=null){
            whereClause += " AND (location_lat BETWEEN " + (search.location_lat-search.distance) + "AND" + (search.location_lat+search.distance) + ")";
            whereClause += " AND (location_long BETWEEN " + (search.location_long-search.distance) + "AND" + (search.location_long+search.distance) + ")";
        }
        if(search.organizer_exact!=null){
            whereClause += " AND (organizer_id="+search.organizer_exact+")";
        }
        if(search.name_fizzy!=null){
            whereClause += " AND (name LIKE '"+search.name_fizzy.replace("'", "\\'")+"')";
        }
        String order = "id ASC";

        String clauses = "where " + whereClause + " order by " + order;
        var limit = search.limit==null?256:Math.max(256, search.limit);
        var offset = search.offset==null?0:search.offset;
        clauses += " limit " + limit + "," + offset;

        List<EventAPI.Event> events_partial;
        try(var stmt = trans.namedPreparedStatement("select * from events " + clauses)){
            if(session!=null&&session.organizer_id!=null)
                stmt.setInt(":organizer_id", session.organizer_id);
            events_partial = SqlSerde.sqlList(stmt.executeQuery(), EventAPI.Event.class);
        }
        List<EventAPI.AllEvent> events = new ArrayList<>(events_partial.size());
        for (EventAPI.Event event : events_partial) {
            events.add(new EventAPI.AllEvent(event, new ArrayList<>()));
        }

        try(var stmt = trans.namedPreparedStatement("select events.id, event_tags.tag, event_tags.category from events left join event_tags on events.id=event_tags.event_id " + clauses)){
            if(session!=null&&session.organizer_id!=null)
                stmt.setInt(":organizer_id", session.organizer_id);
            var rs = stmt.getResultSet();

            int index = 0;
            while(rs.next()){
                var id = rs.getInt("id");
                var tag = rs.getString("tag");
                var category = rs.getBoolean("category");
                while(events.get(index).event().id!=id)index++;
                events.get(index).tags().add(new EventAPI.EventTag(tag, category));
            }
        }

        return events;
    }
}
