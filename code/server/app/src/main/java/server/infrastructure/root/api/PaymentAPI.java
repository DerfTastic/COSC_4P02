package server.infrastructure.root.api;

import com.alibaba.fastjson2.JSON;
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

    @JSONType(
            typeKey = "type",
            seeAlso = {AccountOrganizerUpgradeReceipt.class, TicketReceipt.class}
    )
    public sealed interface ReceiptItem permits AccountOrganizerUpgradeReceipt, TicketReceipt{
        long purchase_price();
    }
    @JSONType(typeName = "AccountOrganizerUpgrade")
    public record AccountOrganizerUpgradeReceipt(long purchase_price) implements ReceiptItem {}
    @JSONType(typeName = "Ticket")
    public record TicketReceipt(String name, long id, long purchase_price) implements ReceiptItem {}

    public record Receipt(
            long payment_id,
            List<ReceiptItem> items,
            long date,
            long subtotal,
            long fees,
            long gst,
            long total
    ){

    }

    public static void verify_payment(PaymentInfo payment, long amount){
        //TODO
    }

    @Route
    public static @Json Receipt make_purchase(@FromRequest(RequireSession.class)UserSession auth, RwTransaction trans, @Body@Json Order order) throws SQLException {

        long date = System.currentTimeMillis();
        long payment_id;
        try(var stmt = trans.namedPreparedStatement("insert into payments values (null, :user_id, '', :date, 0, 0, 0, 0) returning id")){
            stmt.setLong(":user_id", auth.user_id);
            stmt.setLong(":date", date);
            payment_id = SqlSerde.sqlSingle(stmt.executeQuery(), rs -> rs.getLong("id"));
        }

        ArrayList<ReceiptItem> items = new ArrayList<>();
        long subtotal = 0;
        try(var ticket = trans.namedPreparedStatement("insert into purchased_tickets values(null, :user_id, :ticket_id, :payment_id, (select price from tickets where id=:ticket_id)) returning (select name from tickets where id=:ticket_id), id, (select price from tickets where id=:ticket_id)"); var organizer = trans.namedPreparedStatement("update users set organizer=true where id=:user_id")){
            for(var item : order.items){
                switch(item){
                    case AccountOrganizerUpgrade ignore -> {
                        organizer.setLong(":user_id", auth.user_id);
                        if(organizer.executeUpdate()!=1)
                            throw new SQLException();
                        items.add(new AccountOrganizerUpgradeReceipt(50_000000));
                    }
                    case Ticket(long id) -> {
                        ticket.setLong(":user_id", auth.user_id);
                        ticket.setLong(":ticket_id", id);
                        ticket.setLong(":payment_id", payment_id);
                        items.add(SqlSerde.sqlSingle(ticket.executeQuery(),
                                rs -> new TicketReceipt(
                                        rs.getString(1),
                                        rs.getLong(2),
                                        rs.getLong(3)
                                ))
                        );
                    }
                }
                subtotal += items.getLast().purchase_price();
            }
        }

        long fees = 0;
        long gst = 0;
        long total = subtotal + fees + gst;
        try(var stmt = trans.namedPreparedStatement("update payments set receipt=:receipt, subtotal=:subtotal, fees=:fees, gst=:gst, total=:total where id=:payment_id")){
            stmt.setLong(":payment_id", payment_id);
            stmt.setString(":receipt", JSON.toJSONString(items));
            stmt.setLong(":subtotal", subtotal);
            stmt.setLong(":fees", fees);
            stmt.setLong(":gst", gst);
            stmt.setLong(":total", total);
            if(stmt.executeUpdate()!=1)
                throw new SQLException();
        }

        var receipt = new Receipt(
                payment_id,
                items,
                date,
                subtotal,
                fees,
                gst,
                total
        );

        verify_payment(order.payment, total);
        trans.commit();

        return receipt;
    }
}
