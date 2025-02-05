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
        StringBuilder whereClause;
        if(session!=null&&session.organizer_id!=null)
            whereClause = new StringBuilder("(draft=false OR organizer_id=" + session.organizer_id + ")");
        else
            whereClause = new StringBuilder("draft=false");

        if(search.tags!=null)
            for(var item : search.tags){
                whereClause.append(" AND id IN (select event_id from event_tags where category=")
                        .append(item.category)
                        .append(" AND tag='")
                        .append(item.tag.replace("'", "\\'"))
                        .append("')");
            }
        if(search.distance!=null && search.location_lat!=null && search.location_long!=null){
            whereClause.append(" AND (location_lat BETWEEN ")
                    .append(search.location_lat - search.distance)
                    .append(" AND ")
                    .append(search.location_lat + search.distance)
                    .append(")");
            whereClause.append(" AND (location_long BETWEEN ")
                    .append(search.location_long - search.distance)
                    .append(" AND ")
                    .append(search.location_long + search.distance)
                    .append(")");
        }
        if(search.organizer_exact!=null){
            whereClause.append(" AND (organizer_id=").append(search.organizer_exact).append(")");
        }
        if(search.organizer_fuzzy!=null){
            whereClause.append(" AND organizer_id IN (select users.organizer_id from users where events.organizer_id=users.organizer_id AND users.name LIKE '").append(search.organizer_fuzzy.replace("'", "\\'")).append("')");
        }
        if(search.name_fizzy!=null){
            whereClause.append(" AND (name LIKE '").append(search.name_fizzy.replace("'", "\\'")).append("')");
        }
        if(search.location!=null){
            whereClause.append(" AND (location LIKE '").append(search.location.replace("'", "\\'")).append("')");
        }
        String order = "id ASC";

        String clauses = "where " + whereClause + " order by " + order;
        var limit = search.limit==null?256:Math.max(256, search.limit);
        var offset = search.offset==null?0:search.offset;
        clauses += " limit " + offset + "," + limit;

        List<EventAPI.Event> events_partial;
        try(var stmt = trans.createStatement()){
            var rs = stmt.executeQuery("select * from events " + clauses);
            events_partial = SqlSerde.sqlList(rs, EventAPI.Event.class);
        }
        List<EventAPI.AllEvent> events = new ArrayList<>(events_partial.size());
        for (EventAPI.Event event : events_partial) {
            events.add(new EventAPI.AllEvent(event, new ArrayList<>()));
        }

        try(var stmt = trans.createStatement()){
            var rs = stmt.executeQuery("select id, tag, category from events left join event_tags on id=event_id " + clauses);

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
