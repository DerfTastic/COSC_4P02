package server.infrastructure.root.api;

import server.framework.db.RwTransaction;
import server.framework.web.annotations.*;
import server.framework.web.param.auth.RequireSession;
import server.framework.web.param.auth.UserSession;

/**
 * @implNote <br>
 * createOrder: Client -> US -(token)> Client <br>
 * ~showOrderDetails Client -(token)> paymentAPI -> Client <br>
 * confirmOrder Client -(token)> paymentAPI <br>
 * finishOrder Client -(token)> US <br>
 * |---capturePayment US -(token)> paymentAPI <br>
 */
@SuppressWarnings("unused")
@Routes
public class PaymentAPI {

    /**
     * Creates an order from a list of tickets the user wants to purchase
     * Note: this doesn't reserve the tickets and they can be purchased after this
     *
     * @implNote This is normally responsible for creating the order on the payment API then relaying the token back to the user
     */
    @Route
    public static String createOrder(@FromRequest(RequireSession.class)UserSession session, RwTransaction trans, @Body @Json int[] tickets){
        return "";
    }

    /**
     * Shows the order detains
     * @implNote This is normally done on the client side through the payment API but this is an example endpoint
     */
    public static void showOrderDetails(@FromRequest (RequireSession.class)UserSession session, String token){

    }

    /**
     * This confirms the client is willing to pay the order
     *
     * @implNote  This is normally done on the client side through the payment API but this is an example endpoint
     */
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
    @Route
    public static void finishOrder(@FromRequest (RequireSession.class)UserSession session, String token){

    }
}
