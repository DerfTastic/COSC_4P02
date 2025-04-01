package server.infrastructure.root.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;
import framework.db.RoTransaction;
import framework.db.RwTransaction;
import framework.util.SqlSerde;
import framework.web.Util;
import framework.web.annotations.*;
import server.infrastructure.param.auth.RequireSession;
import server.infrastructure.param.auth.SessionCache;
import server.infrastructure.param.auth.UserSession;
import server.mail.MailServer;

import javax.mail.Message;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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
    ) {
    }

    @JSONType(
            typeKey = "type",
            seeAlso = {AccountOrganizerUpgrade.class, Ticket.class}
    )
    public sealed interface OrderItem permits AccountOrganizerUpgrade, Ticket {
    }

    @JSONType(typeName = "AccountOrganizerUpgrade")
    public record AccountOrganizerUpgrade() implements OrderItem {
    }

    @JSONType(typeName = "Ticket")
    public record Ticket(long id) implements OrderItem {
    }

    public record Order(
            List<OrderItem> items,
            PaymentInfo payment
    ) {
    }

    @JSONType(
            typeKey = "type",
            seeAlso = {AccountOrganizerUpgradeReceipt.class, TicketReceipt.class},
            serializeFeatures = JSONWriter.Feature.WriteClassName
    )
    public sealed interface ReceiptItem permits AccountOrganizerUpgradeReceipt, TicketReceipt {
        long purchase_price();
    }

    @JSONType(typeName = "AccountOrganizerUpgrade")
    public record AccountOrganizerUpgradeReceipt(long purchase_price) implements ReceiptItem {
    }

    @JSONType(typeName = "Ticket")
    public record TicketReceipt(String name, long purchase_price, PurchasedTicketId id) implements ReceiptItem {
    }

    public record PurchasedTicketId(long pid, String salt) {
    }

    public record Receipt(
            long payment_id,
            List<ReceiptItem> items,
            long date,
            long subtotal,
            long fees,
            long gst,
            long total
    ) {
    }

    public static void verify_payment(PaymentInfo payment, long amount) {
        //TODO
    }

    @Route
    public static @Json List<Receipt> list_receipts(@FromRequest(RequireSession.class) UserSession auth, RoTransaction trans) throws SQLException {
        List<Receipt> list;
        try (var stmt = trans.namedPreparedStatement("select * from payments where user_id=:user_id order by payment_date desc")) {
            stmt.setLong(":user_id", auth.user_id);
            list = SqlSerde.sqlList(stmt.executeQuery(), rs -> new Receipt(
                    rs.getLong("id"),
                    JSON.parseArray(rs.getString("receipt"), ReceiptItem.class),
                    rs.getLong("payment_date"),
                    rs.getLong("subtotal"),
                    rs.getLong("fees"),
                    rs.getLong("gst"),
                    rs.getLong("total")
            ));
        }
        trans.commit();
        return list;
    }

    public record Estimate(
            List<ReceiptItem> items,
            long subtotal,
            long fees,
            long gst,
            long total
    ) {
    }

    @Route
    public static @Json Estimate create_estimate(@FromRequest(RequireSession.class) UserSession auth, RoTransaction trans, @Body @Json OrderItem[] order) throws SQLException {
        ArrayList<ReceiptItem> items = new ArrayList<>();
        long subtotal = 0;
        try (var ticket = trans.namedPreparedStatement("select name, price from tickets where id=:ticket_id AND event_id IN (select id from events where draft=false)");) {
            for (var item : order) {
                switch (item) {
                    case AccountOrganizerUpgrade ignore -> items.add(new AccountOrganizerUpgradeReceipt(50_000000));
                    case Ticket(long id) -> {
                        ticket.setLong(":ticket_id", id);
                        items.add(SqlSerde.sqlSingle(ticket.executeQuery(),
                                rs -> new TicketReceipt(
                                        rs.getString(1),
                                        rs.getLong(2),
                                        null
                                ))
                        );
                    }
                }
                subtotal += items.getLast().purchase_price();
            }
        }

        long fees = (subtotal * 15000) / (1_000000L); // 0.015 1.5%
        long gst = (subtotal * 50000) / (1_000000L); // 0.050 5.0%
        long total = subtotal + fees + gst;
        trans.commit();

        return new Estimate(
                items,
                subtotal,
                fees,
                gst,
                total
        );
    }

    @Route
    public static @Json Receipt make_purchase(@FromRequest(RequireSession.class) UserSession auth, RwTransaction trans, @Body @Json Order order, SessionCache cache, MailServer mail) throws SQLException {

        long date = System.currentTimeMillis();
        long payment_id;
        try (var stmt = trans.namedPreparedStatement("insert into payments values (null, :user_id, '', :date, 0, 0, 0, 0) returning id")) {
            stmt.setLong(":user_id", auth.user_id);
            stmt.setLong(":date", date);
            payment_id = SqlSerde.sqlSingle(stmt.executeQuery(), rs -> rs.getLong("id"));
        }

        ArrayList<ReceiptItem> items = new ArrayList<>();
        long subtotal = 0;
        try (var ticket = trans.namedPreparedStatement("""
                        insert into purchased_tickets values
                            (null, :user_id, :ticket_id, :payment_id, (select price from tickets where id=:ticket_id), :salt)
                        returning
                            (select name from tickets where id=:ticket_id),
                            (select price from tickets where id=:ticket_id),
                            id
                        """);
             var organizer = trans.namedPreparedStatement(
                     "update users set organizer=true where id=:user_id")
            ) {
            for (var item : order.items) {
                switch (item) {
                    case AccountOrganizerUpgrade ignore -> {
                        organizer.setLong(":user_id", auth.user_id);
                        if (organizer.executeUpdate() != 1)
                            throw new SQLException();
                        items.add(new AccountOrganizerUpgradeReceipt(50_000000));

                        if (cache != null) {
                            // we need to manually invalidate the cache here
                            try (var stmt = trans.namedPreparedStatement("select id from sessions where user_id=:id")) {
                                stmt.setLong(":id", auth.user_id);
                                SqlSerde.sqlForEach(stmt.executeQuery(), rs -> {
                                    cache.invalidate_session(rs.getLong("id"));
                                });
                            }
                        }
                    }
                    case Ticket(long id) -> {
                        ticket.setLong(":user_id", auth.user_id);
                        ticket.setLong(":ticket_id", id);
                        ticket.setLong(":payment_id", payment_id);
                        var rng = new byte[16];
                        new SecureRandom().nextBytes(rng);
                        var salt = Util.hashy(rng);
                        ticket.setString(":salt", salt);
                        items.add(SqlSerde.sqlSingle(ticket.executeQuery(),
                                rs -> new TicketReceipt(
                                        rs.getString(1),
                                        rs.getLong(2),
                                        new PurchasedTicketId(rs.getLong(3), salt)
                                )));
                    }
                }
                subtotal += items.getLast().purchase_price();
            }
        }

        long fees = (subtotal * 15000) / (1_000000L); // 0.015 1.5%
        long gst = (subtotal * 50000) / (1_000000L); // 0.050 5.0%
        long total = subtotal + fees + gst;
        try (var stmt = trans.namedPreparedStatement("update payments set receipt=:receipt, subtotal=:subtotal, fees=:fees, gst=:gst, total=:total where id=:payment_id")) {
            stmt.setLong(":payment_id", payment_id);
            stmt.setString(":receipt", JSON.toJSONString(items));
            stmt.setLong(":subtotal", subtotal);
            stmt.setLong(":fees", fees);
            stmt.setLong(":gst", gst);
            stmt.setLong(":total", total);
            if (stmt.executeUpdate() != 1)
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

        mail.sendMail(message -> {
            message.setRecipients(Message.RecipientType.TO, MailServer.fromStrings(auth.email));
            message.setSubject("Purchase");
            var str = new StringBuilder();
            str.append("<p>Sub Total: ").append(formatPrice(receipt.subtotal)).append("</p>");
            str.append("<p>Fees: ").append(formatPrice(receipt.fees)).append("</p>");
            str.append("<p>GST: ").append(formatPrice(receipt.gst)).append("</p>");
            str.append("<p>Total: ").append(formatPrice(receipt.total)).append("</p>");
            for (var item : receipt.items) {
                str.append("<div>");
                switch (item) {
                    case AccountOrganizerUpgradeReceipt(long purchase_price) -> {
                        str.append("<p>Account Upgrade: ").append(formatPrice(purchase_price)).append("</p>");
                    }
                    case TicketReceipt(String name, long purchase_price, PurchasedTicketId id) -> {
                        str.append("<p>Ticket: ")
                                .append(name).append(" ")
                                .append(formatPrice(purchase_price))
                                .append("</p>");
                        str.append("<img width=\"200\" height=\"200\" src=\"")
                                .append("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=")
                                .append(URLEncoder.encode(JSON.toJSONString(id), StandardCharsets.UTF_8))
                                .append("\"></img>");
                    }
                }
                str.append("</div>");
            }
            message.setContent(str.toString(), "text/html");
        });

        return receipt;
    }

    private static String formatPrice(long price) {
        return "$" + (price / 1000000) + "." + String.format("%06d", price % 1000000);
    }
}
