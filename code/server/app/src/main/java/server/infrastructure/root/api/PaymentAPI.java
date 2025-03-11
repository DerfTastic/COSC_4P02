package server.infrastructure.root.api;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.annotation.JSONType;
import framework.db.RwTransaction;
import framework.util.SqlSerde;
import framework.web.WebServer;
import framework.web.annotations.*;
import framework.web.annotations.http.Get;
import framework.web.error.BadRequest;
import server.infrastructure.param.auth.RequireSession;
import server.infrastructure.param.auth.UserSession;

import java.sql.SQLException;
import java.util.List;


@SuppressWarnings("unused")
@Routes
public class PaymentAPI {

    public sealed interface OrderItem permits TicketItem, AccountUpgrade{}
    public record TicketItem(String name, long ticket_id, long price) implements OrderItem{}
    public record AccountUpgrade(long user_id, long price) implements OrderItem{}
    public record Order(
            List<OrderItem> items,
            long sub_total,
            long usage_fee,
            long tax,
            long total
    ){}

    private static class PaymentState{}

    @OnMount
    public static void init(WebServer server){
        server.addManagedState(new PaymentState());
    }

    @JSONType(
            typeKey = "type",
            seeAlso = {AccountOrganizerUpgrade.class, Ticket.class}
    )
    public sealed interface Item permits AccountOrganizerUpgrade, Ticket{}
    @JSONType(typeName = "AccountOrganizerUpgrade")
    public record AccountOrganizerUpgrade() implements Item {}
    @JSONType(typeName = "Ticket")
    public record Ticket(long id) implements Item {}

    public record ticket(long id, long price, long event_id, String name, long available_tickets){}

    public static List<Ticket> verify_purchase_viability(UserSession session, RwTransaction trans, Item[] items) throws SQLException, BadRequest {
        final boolean[] upgrade = {false};
        JSONArray ticket_ids = new JSONArray();
        for(var item : items){
            switch(item){
                case AccountOrganizerUpgrade ignore when upgrade[0] -> throw new BadRequest("Cannot purchase account organizer upgrade twice");
                case AccountOrganizerUpgrade ignore -> upgrade[0] = true;
                case Ticket(long id) -> ticket_ids.add(id);
            }
        }
        if(session.organizer&&upgrade[0])
            throw new BadRequest("Cannot purchase account organizer upgrade, account is already organizer");

        List<Ticket> tickets;
        try(var stmt = trans.namedPreparedStatement("select * from users join json_each(:tickets) on users.id=json_each.value")){
            stmt.setString(":tickets", ticket_ids.toJSONString());
            tickets = SqlSerde.sqlList(stmt.executeQuery(), Ticket.class);
        }

        return tickets;
    }


    @Route
    public static String createOrder(@FromRequest(RequireSession.class)UserSession session, RwTransaction trans, @Body @Json Item[] items) throws SQLException, BadRequest {

        verify_purchase_viability(session, trans, items);


        trans.commit();

        PretendPaypal.createOrder();
        return "";
    }


    @Route
    public static void finishOrder(@FromRequest (RequireSession.class)UserSession session, RwTransaction trans, String token){
//        verify_purchase_viability(session, trans, items);

        PretendPaypal.captureOrder();
    }


    /**
     * @implNote <br>
     * createOrder: Client -> US -(token)> Client <br>
     * ~showOrderDetails Client -(token)> paymentAPI -> Client <br>
     * confirmOrder Client -(token)> paymentAPI <br>
     * captureOrder Client -(token)> US <br>
     * |---capturePayment US -(token)> paymentAPI <br>
     */
    @Routes
    public static class PretendPaypal{
        /**
         * Creates an order from a list of tickets the user wants to purchase
         * Note: this doesn't reserve the tickets and they can be purchased after this
         *
         * @implNote This is normally responsible for creating the order on the payment API then relaying the token back to the user
         */
        public static void createOrder(){}

        /**
         * Shows the order detains
         * @implNote This is normally done on the client side through the payment API but this is an example endpoint
         */
        @Route
        @Get
        public static void showOrderDetails(@FromRequest (RequireSession.class)UserSession session, String token){

        }

        /**
         * This confirms the client is willing to pay the order
         *
         * @implNote  This is normally done on the client side through the payment API but this is an example endpoint
         */
        @Route
        public static void confirmOrder(String token){
            // this is basically just
        }

        /**
         * Once the client reviews and confirms the order this is where the payment will actually take place
         * The tickets are set aside and verified they still exist, only after that will we proceed with the transaction
         * and move the funds
         *
         * @implNote This would verify through the payment API that the transaction was successful
         */
        public static void captureOrder(){

        }
    }
}
