package server.infrastructure.root.api;

import framework.db.RoTransaction;
import framework.web.annotations.*;
import server.infrastructure.param.auth.OptionalAuth;
import server.infrastructure.param.auth.UserSession;
import framework.util.SqlSerde;

import java.sql.SQLException;
import java.util.ArrayList;
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
            List<EventAPI.EventTag> tags,
            String organizer_fuzzy,
            String name_fuzzy,
            Long organizer_exact,
            Double distance,
            Double location_lat,
            Double location_long,
            String location,
            Long offset,
            Long limit,
            SortBy sort_by
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
                whereClause.append(" AND (events.id IN (select event_id from event_tags where category=")
                        .append(item.category)
                        .append(" AND tag='")
                        .append(item.tag.replace("'", "\\'"))
                        .append("'))");
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
        if(search.date_start!=null){
            whereClause.append(" AND (start >= ")
                    .append(search.date_start)
                    .append(" OR start IS NULL)");
        }
        if(search.date_end!=null){
            whereClause.append(" AND (start <= ")
                    .append(search.date_end)
                    .append(" OR start IS NULL)");
        }
        if(search.max_duration!=null){
            whereClause.append(" AND (duration <= ")
                    .append(search.max_duration)
                    .append(" OR duration IS NULL)");
        }
        if(search.min_duration!=null){
            whereClause.append(" AND (duration >= ")
                    .append(search.min_duration)
                    .append(" OR duration IS NULL)");
        }
        if(search.organizer_exact!=null){
            whereClause.append(" AND (organizer_id=").append(search.organizer_exact).append(")");
        }
        if(search.organizer_fuzzy!=null){
            whereClause.append(" AND organizer_id IN (select users.organizer_id from users where events.organizer_id=users.organizer_id AND users.name LIKE '").append(search.organizer_fuzzy.replace("'", "\\'")).append("')");
        }
        if(search.name_fuzzy!=null){
            whereClause.append(" AND (name LIKE '").append(search.name_fuzzy.replace("'", "\\'")).append("')");
        }
        if(search.location!=null){
            whereClause.append(" AND (location_name LIKE '").append(search.location.replace("'", "\\'")).append("')");
        }

        String order = switch(search.sort_by==null?SortBy.Nothing:search.sort_by){
            case MinPrice -> "(select min(price) from tickets where event_id=id) ASC";
            case MaxPrice -> "(select max(price) from tickets where event_id=id) DESC";
            case TicketsAvailable -> "(coalesce(coalesce((select sum(available_tickets) from tickets where tickets.event_id=events.id),events.available_total_tickets)-(select count(*) from purchased_tickets where purchased_tickets.ticket_id in (select tickets.id from tickets where tickets.event_id=events.id)),999999999)) DESC";
            case Closest -> "abs(location_lat-"+search.location_lat+") ASC abs(location_long-"+search.location_long+") ASC";
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
                var id = rs.getLong("id");
                var tag = rs.getString("tag");
                var category = rs.getBoolean("category");
                while(events.get(index).event().id!=id)index++;
                events.get(index).tags().add(new EventAPI.EventTag(tag, category));
            }
        }
        trans.commit();

        return events;
    }
}
