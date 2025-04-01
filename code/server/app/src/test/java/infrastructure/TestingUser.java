package infrastructure;

import com.sun.net.httpserver.*;
import framework.db.DbManager;
import framework.web.WebServer;
import framework.web.error.BadRequest;
import framework.web.error.Unauthorized;
import framework.web.request.Request;
import org.junit.jupiter.api.Assertions;
import server.Config;
import server.infrastructure.param.auth.*;
import server.infrastructure.root.api.AccountAPI;
import server.infrastructure.root.api.OrganizerAPI;
import server.infrastructure.root.api.PaymentAPI;
import server.mail.MailServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class TestingUser {
    public String name;
    public String email;
    public String password;
    public String bio;
    public String disp_email;
    public String disp_phone_number;

    public long picture;
    public long banner;

    public String session;

    public TestingUser(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    /**
     * Registers this TestingUser using {@link AccountAPI#register}
     */
    public void register(MailServer mail, DbManager db, boolean send_mail_on_register) throws BadRequest, SQLException {
        try(var conn = db.rw_transaction(null)){
            var register = new AccountAPI.Register();
            register.name = this.name;
            register.email = this.email;
            register.password = this.password;
            AccountAPI.register(mail, conn, register, send_mail_on_register);
            conn.tryCommit();
        }
    }


    public void makeOrganizer(DbManager db, SessionCache cache, MailServer mail) throws SQLException, Unauthorized, BadRequest {
        var auth = this.userSession(db, cache);
        try(var conn = db.rw_transaction(null)){
            PaymentAPI.make_purchase(auth, conn,
                    new PaymentAPI.Order(
                            List.of(new PaymentAPI.AccountOrganizerUpgrade()),
                            new PaymentAPI.PaymentInfo("", "", "", "", "")
                    ), cache, mail);
            conn.tryCommit();
        }
    }

    /**
     * Logs in this TestingUser by using {@link AccountAPI#login} (which inserts into the "sessions" table) and then returns the session
     */
    public String login(MailServer mail, DbManager db, boolean send_mail_on_login) throws SQLException, UnknownHostException, Unauthorized {
        try(var conn = db.rw_transaction(null)){
            var login = new AccountAPI.Login();
            login.email = this.email;
            login.password = this.password;
            session = AccountAPI.login(mail, InetAddress.getByName("localhost"), "", conn, login, send_mail_on_login);
            conn.tryCommit();
            return session;
        }
    }

    public UserSession userSession(DbManager db, SessionCache cache) throws SQLException, Unauthorized {
        return new RequireSession().construct(createRequest(this.session, db, cache));
    }

    public UserSession organizerSession(DbManager db, SessionCache cache) throws SQLException, Unauthorized {
        return new RequireOrganizer().construct(createRequest(this.session, db, cache));
    }

    public UserSession adminSession(DbManager db, SessionCache cache) throws SQLException, Unauthorized {
        return new RequireAdmin().construct(createRequest(this.session, db, cache));
    }


    private static Request createRequest(String session, DbManager db, SessionCache cache){
        var server = new HttpServer() {
            InetSocketAddress addr;
            @Override
            public void bind(InetSocketAddress addr, int backlog) throws IOException {
                this.addr = addr;
            }

            @Override
            public void start() {}

            @Override
            public void setExecutor(Executor executor) {}

            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void stop(int delay) {}

            @Override
            public HttpContext createContext(String path, HttpHandler handler) {
                return null;
            }

            @Override
            public HttpContext createContext(String path) {
                return null;
            }

            @Override
            public void removeContext(String path) throws IllegalArgumentException {}

            @Override
            public void removeContext(HttpContext context) {}

            @Override
            public InetSocketAddress getAddress() {
                return this.addr;
            }
        };
        var exchange = new HttpExchange() {
            @Override
            public Headers getRequestHeaders() {
                return new Headers(Map.of("X-UserAPIToken", List.of(session)));
            }

            @Override
            public Headers getResponseHeaders() {
                return null;
            }

            @Override
            public URI getRequestURI() {
                return null;
            }

            @Override
            public String getRequestMethod() {
                return "";
            }

            @Override
            public HttpContext getHttpContext() {
                return null;
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getRequestBody() {
                return null;
            }

            @Override
            public OutputStream getResponseBody() {
                return null;
            }

            @Override
            public void sendResponseHeaders(int rCode, long responseLength) throws IOException {

            }

            @Override
            public InetSocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public int getResponseCode() {
                return 0;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return null;
            }

            @Override
            public String getProtocol() {
                return "";
            }

            @Override
            public Object getAttribute(String name) {
                return null;
            }

            @Override
            public void setAttribute(String name, Object value) {

            }

            @Override
            public void setStreams(InputStream i, OutputStream o) {

            }

            @Override
            public HttpPrincipal getPrincipal() {
                return null;
            }
        };
        var webserver = new WebServer(server);
        webserver.addManagedState(db, DbManager.class);
        webserver.addManagedState(db);
        webserver.addManagedState(cache, SessionCache.class);
        return new Request(webserver, exchange, "");
    }
}
