package server.infrastructure.root.api;

import com.alibaba.fastjson2.annotation.JSONType;
import framework.db.RwTransaction;
import framework.util.SqlSerde;
import framework.web.annotations.*;
import server.infrastructure.param.auth.RequireSession;
import server.infrastructure.param.auth.UserSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("unused")
@Routes
public class PaymentAPI {

    public record PaymentInfo(
            String name,
            String billing,
            String card,
            String expiration,
            String code
    ){}
    @JSONType(
            typeKey = "type",
            seeAlso = {AccountOrganizerUpgrade.class, Ticket.class}
    )
    public sealed interface OrderItem permits AccountOrganizerUpgrade, Ticket{}
    @JSONType(typeName = "AccountOrganizerUpgrade")
    public record AccountOrganizerUpgrade() implements OrderItem {}
    @JSONType(typeName = "Ticket")
    public record Ticket(long id) implements OrderItem {}
    public record Order(
            List<OrderItem> items,
            PaymentInfo payment
    ){}

    public sealed interface ReceiptItem permits AccountOrganizerUpgradeReceipt, TicketReceipt{}
    @JSONType(typeName = "AccountOrganizerUpgrade")
    public record AccountOrganizerUpgradeReceipt(long purchase_price) implements ReceiptItem {}
    @JSONType(typeName = "Ticket")
    public record TicketReceipt(String name, long id, long purchase_price) implements ReceiptItem {}

    public record Receipt(
            List<ReceiptItem> items,
            long payment_id,
            long amount,
            long date
    ){

    }

    public static void verify_payment(PaymentInfo payment, long amount){
        //TODO
    }

    @Route
    public static void make_purchase(@FromRequest(RequireSession.class)UserSession auth, RwTransaction trans, @Body@Json Order order) throws SQLException {

        long date = System.currentTimeMillis();
        long payment_id;
        try(var stmt = trans.namedPreparedStatement("insert into payments values (null, :user_id, '', 0, :date) returning id")){
            stmt.setLong(":user_id", auth.user_id);
            stmt.setLong(":date", date);
            payment_id = SqlSerde.sqlSingle(stmt.executeQuery(), rs -> rs.getLong("id"));
        }

        ArrayList<ReceiptItem> items = new ArrayList<>();
        long amount = 0;
        try(var ticket = trans.namedPreparedStatement("insert into purchased_tickets values(null, :user_id, :ticket_id) returning id, (select name from tickets where id=:ticket_id)"); var organizer = trans.namedPreparedStatement("")){
            for(var item : order.items){
                switch(item){
                    case AccountOrganizerUpgrade ignore -> {
                    }
                    case Ticket(long id) -> {
                    }
                }
            }
        }

        var receipt = new Receipt(
                items,
                payment_id,
                amount,
                date
        );

        verify_payment(order.payment, 0);
    }
}
