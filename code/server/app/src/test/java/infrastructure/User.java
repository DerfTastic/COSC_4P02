package infrastructure;

import framework.db.DbManager;
import framework.web.error.BadRequest;
import framework.web.error.Unauthorized;
import org.junit.jupiter.api.Assertions;
import server.Config;
import server.infrastructure.param.auth.SessionCache;
import server.infrastructure.param.auth.UserSession;
import server.infrastructure.root.api.AccountAPI;
import server.infrastructure.root.api.OrganizerAPI;
import server.mail.MailServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;

public class User {
    public String name;
    public String email;
    public String password;
    public String bio;
    public String disp_email;
    public String disp_phone_number;

    public long picture;
    public long banner;

    public String session;

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public void register(MailServer mail, DbManager db, Config config) throws BadRequest, SQLException {
        try(var conn = db.rw_transaction(null)){
            var register = new AccountAPI.Register();
            register.name = this.name;
            register.email = this.email;
            register.password = this.password;
            AccountAPI.register(mail, conn, register, config);
            conn.tryCommit();
        }
    }

    public void makeOrganizer(DbManager db, SessionCache cache) throws SQLException, Unauthorized, BadRequest {
        var auth = this.userSession(db, cache);
        try(var conn = db.rw_transaction(null)){
            OrganizerAPI.convert_to_organizer_account(auth, conn, cache);
            conn.tryCommit();
        }
    }

    public String login(MailServer mail, DbManager db, Config config) throws SQLException, UnknownHostException, Unauthorized {
        try(var conn = db.rw_transaction(null)){
            var login = new AccountAPI.Login();
            login.email = this.email;
            login.password = this.password;
            session = AccountAPI.login(mail, InetAddress.getByName("localhost"), "", conn, login, config);
            conn.tryCommit();
            return session;
        }
    }

    public UserSession userSession(DbManager db, SessionCache cache) throws SQLException, Unauthorized {
        try(var conn = db.ro_transaction(null)){
            UserSession auth = UserSession.create(this.session, conn, cache);
            conn.tryCommit();
            return auth;
        }
    }

    public UserSession organizerSession(DbManager db, SessionCache cache) throws SQLException, Unauthorized {
        try(var conn = db.ro_transaction(null)){
            UserSession session = UserSession.create(this.session, conn, cache);
            Assertions.assertTrue(session.organizer);
            conn.tryCommit();
            return session;
        }
    }

    public UserSession adminSession(DbManager db, SessionCache cache) throws SQLException, Unauthorized {
        try(var conn = db.ro_transaction(null)){
            UserSession session = UserSession.create(this.session, conn, cache);
            Assertions.assertTrue(session.admin);
            conn.tryCommit();
            return session;
        }
    }
}
