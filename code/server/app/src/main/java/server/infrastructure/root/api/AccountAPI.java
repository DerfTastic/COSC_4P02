package server.infrastructure.root.api;

import com.alibaba.fastjson2.JSONReader;
import framework.web.TimedEvents;
import framework.web.WebServer;
import framework.web.annotations.*;
import framework.web.annotations.http.Delete;
import framework.web.annotations.url.Nullable;
import framework.web.request.Request;
import framework.web.route.RouteParameter;
import org.sqlite.SQLiteException;
import framework.db.RoConn;
import framework.db.RoTransaction;
import framework.db.RwTransaction;
import framework.web.error.BadRequest;
import framework.web.error.Unauthorized;
import server.infrastructure.DbManagerImpl;
import server.infrastructure.DynamicMediaHandler;
import server.infrastructure.param.Config;
import server.infrastructure.param.NotRequired;
import server.infrastructure.session.SessionCache;
import server.infrastructure.session.UserSession;
import server.mail.MailServer;
import framework.web.Util;
import framework.web.annotations.url.Path;
import framework.web.param.misc.IpHandler;
import framework.web.param.misc.UserAgentHandler;
import framework.util.SqlSerde;

import javax.mail.Message;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A group of HTTP API endpoints that provide a suite of user-account-related management operations.
 */
@SuppressWarnings("unused")
@Routes
public class AccountAPI {

    @OnMount
    public static void init(WebServer server){
        var db = server.getManagedState(DbManagerImpl.class);

        var cache = new SessionCache();
        server.addManagedState(cache);
        db.addUpdateHook((type, database, table, rowId) -> {
            if (table.equals("sessions")) {
                switch (type) {
                    case DELETE, UPDATE -> cache.invalidate_session(rowId);
                    case INSERT -> {
                    }
                }
            }
        });

        var timer = server.getManagedState(TimedEvents.class);
        timer.addMinutely(() -> {
            try(var trans = db.rw_transaction(">session_timer")){
                try(var stmt = trans.namedPreparedStatement("delete from sessions where expiration<:now")){
                    stmt.setLong(":now", new Date().getTime());
                    stmt.execute();
                }
                trans.commit();
                Logger.getGlobal().log(Level.FINE, "Ran session expiration clear");
            }catch (Exception e){
                Logger.getGlobal().log(Level.SEVERE, "Failed to run session expiration clear", e);
            }
        });
    }


    /**
     * Represents a combination of an email string and a password string.
     */
    public static class Login{
        public String email;
        public String password;
    }

    /**
     * Logs in a user (if they exist in the users DB table) by creating a session.
     *
     * @return Token of the user's new session.
     */
    @Route
    public static String login(MailServer mail, @FromRequest(IpHandler.class)InetAddress ip, @FromRequest(UserAgentHandler.class)String agent, RwTransaction trans,  @Body @Json Login login, @Config boolean send_mail_on_login) throws SQLException, Unauthorized {

        // Check if user exists in user table
        long user_id;
        login.password = Util.hashy((login.password+"\0\0\0\0"+login.email).getBytes());
        try(var stmt = trans.namedPreparedStatement("select id from users where email=:email AND pass=:pass")){
            stmt.setString(":email", login.email);
            stmt.setString(":pass", login.password);
            try(var res = stmt.executeQuery()){
                if(!res.next())
                    throw new Unauthorized("An account with the specified email does not exist, or the specified password is incorrect");
                try{
                    user_id = res.getLong(1);
                }catch (SQLException ignore){
                    throw new Unauthorized("An account with the specified email does not exist, or the specified password is incorrect");
                }
            }
        }

        // Make a new session since at this point they are an existent user.
        long session_id;
        try(var stmt = trans.namedPreparedStatement("insert into sessions values(null, null, :user_id, :exp, :agent, :ip) returning id")){
            stmt.setLong(":user_id", user_id);
            stmt.setLong(":exp", new Date().getTime() + 2628000000L);
            stmt.setString(":agent", agent);
            stmt.setString(":ip", ip.getHostAddress());
            try(var res = stmt.executeQuery()){
                session_id = res.getLong(1);
            }
        }

        // Generate token for session
        var hash = Util.hashy((login.email + "\0\0\0\0" + login.password + "\0\0\0\0" + session_id + "\0\0\0\0" + System.nanoTime()).getBytes());
        var token = String.format("%s%08X", hash, session_id);

        try(var stmt = trans.namedPreparedStatement("update sessions set token=:token where id=:id")){
            stmt.setString(":token", token);
            stmt.setLong(":id", session_id);
            stmt.execute();
        }

        trans.commit();

        if (send_mail_on_login) {
            mail.sendMail(message -> {
                Util.LocationQuery res = null;
                try {
                    res = Util.queryLocation(ip);
                } catch (Exception ignore) {
                }
                message.setRecipients(Message.RecipientType.TO, MailServer.fromStrings(login.email));
                message.setSubject("Warning");

                message.setContent("Someone logged into your account <br/>IP: " + ip + "<br/>User Agent: " + agent, "text/html");
            });
        }

        Logger.getGlobal().log(Level.FINER, "User: " + login.email + " Logged in with session: " + token);

        return token;
    }

    /**
     * Represents a record in the 'sessions' DB table without the 'id' and 'token' fields.
     */
    public static class Session{
        public long id;
        public long expiration;
        public String agent;
        public String ip;
    }

    /**
     * @return A list of sessions from the 'sessions' DB table, in the form of a {@link List List} of {@link Session Sessions}.
     * @param auth The account that's doing this
     * @param conn Read-only connection to DB.
     * @throws SQLException
     */
    @Route
    public static @Json List<Session> list_sessions(UserSession auth, RoConn conn) throws SQLException {
        List<Session> result;
        try(var stmt = conn.namedPreparedStatement("select * from sessions where user_id=:id")){
            stmt.setLong(":id", auth.user_id());
            result = SqlSerde.sqlList(stmt.executeQuery(), Session.class);
        }
        conn.close();
        return result;
    }

    /**
     * Remove a user session. Only works if session belongs to you and it exists.
     *
     * @param auth The account that's doing this
     * @param trans The read/write DB transaction this is going to happening in
     * @param session_id The id of the session to invalidate.
     * @throws BadRequest If the session does not belong to you or does not exist.
     * @throws SQLException
     */
    @Route("/invalidate_session/<session_id>")
    @Delete
    public static void invalidate_session(UserSession auth, RwTransaction trans, @Path long session_id) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("delete from sessions where id=:session_id AND user_id=:user_id")){
            stmt.setLong(":session_id", session_id);
            stmt.setLong(":user_id", auth.user_id());
            if(stmt.executeUpdate() != 1)
                throw new BadRequest("Could not invalidate session, session does not belong to you or does not exist");
        }
        trans.commit();
    }

    public static class DeleteAccount{
        public String email;
        public String password;
    }

    /**
     * Deletes this user's account (auth and account must be the same account) <br>
     * and removes their profile photo and banner from the media handler.
     *
     * @param auth The account that's doing this
     * @param trans The read/write DB transaction this is going to happening in
     * @param account The account to be deleted
     * @param media The {@link DynamicMediaHandler} to delete this user's pfp and banner from
     * @throws Unauthorized If auth and account don't have the same email
     * @throws SQLException
     */
    @Route
    public static void delete_account(UserSession auth, RwTransaction trans, @Body @Json DeleteAccount account, DynamicMediaHandler media) throws SQLException, Unauthorized {
        record DeleteResult(long picture, long banner){}

        if(!account.email.equals(auth.email()))
            throw new Unauthorized("Incorrect email");
        account.password = Util.hashy((account.password+"\0\0\0\0"+account.email).getBytes());

        DeleteResult res;
        try(var stmt = trans.namedPreparedStatement("delete from users where email=:email AND pass=:pass returning picture, banner")){
            stmt.setString(":email", auth.email());
            stmt.setString(":pass", account.password);
            res = SqlSerde.sqlSingle(stmt.executeQuery(), rs -> new DeleteResult(rs.getLong("picture"), rs.getLong("banner")));
        }
        trans.commit();
        if(res.picture!=0)
            media.delete(res.picture);
        if(res.banner!=0)
            media.delete(res.banner);
    }

    /** Represents an email and/or password change.
     * new_email or new_password can be null if not changing either one.
     */
    public static class ChangeAuth {
        public String old_email;
        public String new_email;
        public String old_password;
        public String new_password;
    }


    /**
     * Changes this account's email and/or password.
     *
     * @param auth The account that's doing this.
     * @param trans The read/write DB transaction this is going to happening in
     * @param ca The username and/or password change ({@link ChangeAuth})
     * @throws SQLException
     * @throws BadRequest If new username and new password are null.
     */
    @Route
    public static void change_auth(UserSession auth, RwTransaction trans, @Body @Json ChangeAuth ca) throws SQLException, BadRequest {
        if(ca.new_password==null&&ca.new_email==null)
            throw new BadRequest("Nothing to change");

        if(ca.new_password==null)ca.new_password = ca.old_password;
        if(ca.new_email==null)ca.new_email = ca.old_email;

        ca.old_password = Util.hashy((ca.old_password+"\0\0\0\0"+ca.old_email).getBytes());
        ca.new_password = Util.hashy((ca.new_password+"\0\0\0\0"+ca.new_email).getBytes());
        try(var stmt = trans.namedPreparedStatement("update users set pass=:new_password, email=:new_email where email=:old_email AND pass=:old_password AND id=:id")){
            stmt.setString(":old_email", ca.old_email);
            stmt.setString(":new_email", ca.new_email);
            stmt.setLong(":id", auth.user_id());
            stmt.setString(":old_password", ca.old_password);
            stmt.setString(":new_password", ca.new_password);
            stmt.execute();
        }

        try(var stmt = trans.namedPreparedStatement("delete from sessions where user_id=:id")){
            stmt.setLong(":id", auth.user_id());
            stmt.execute();
        }
        trans.commit();
    }

    /**
     * Represents a change to user info such as their name, bio, phone number, and email.
     *
     * This class is designed to support partial updates, meaning the client can provide only the fields they want to change
     * without needing to resend the entire user record.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class UpdateUser{
        public String name;
        public Optional<String> bio;
        public Optional<String> disp_phone_number;
        public Optional<String> disp_email;

        private static String requireString(JSONReader reader){
            if(reader.isString())
                return reader.readString();
            else
                throw new RuntimeException("Expected String value");
        }

        private static Optional<String> optionalString(JSONReader reader){
            if(reader.nextIfNull())
                return Optional.empty();
            if(reader.isString())
                return Optional.of(reader.readString());
            else
                throw new RuntimeException("Expected String value");
        }

        public UpdateUser(){}

        /**
         * @param reader The {@link JSONReader} that gets the new user data
         * @throws BadRequest if the input is malformed
         */
        public UpdateUser(JSONReader reader) throws BadRequest {
            if(!reader.nextIfObjectStart())
                throw new BadRequest("Expected an object");
            while(!reader.nextIfObjectEnd()){
                if(!reader.isString())
                    throw new BadRequest("Expected field");
                var field_name = reader.readString();
                if(!reader.nextIfMatch(':'))
                    throw new BadRequest("Expected colon");
                switch(field_name){
                    case "name" -> this.name = requireString(reader);
                    case "bio" -> this.bio = optionalString(reader);
                    case "disp_phone_number" -> this.disp_phone_number = optionalString(reader);
                    case "disp_email" -> this.disp_email = optionalString(reader);
                    default -> throw new BadRequest("Unknown field: " + field_name);
                }
            }
        }
    }

    /**
     * Instantiates a RouteParameter-derived class so that it can parse a {@link UpdateUser} from an HTTP request using {@link FromRequest}
     */
    public static class UpdateUserFromRequest implements RouteParameter<UpdateUser> {
        @Override
        public UpdateUser construct(Request request) throws Exception {
            try(var reader = JSONReader.of(request.exchange.getRequestBody().readAllBytes())){
                return new UpdateUser(reader);
            }
        }
    }

    /**
     * Updates a user's info (such as name, bio, phone number, and email) in the DB.
     *
     * @param auth The account that's doing this
     * @param trans The read/write DB transaction this is going to happening in
     * @param update An {@link UpdateUser} type that represents a user info change of any of the following fields: name, bio, disp_phone_number, disp_email
     * @throws SQLException
     * @throws BadRequest If SQL statement failed
     */
    @SuppressWarnings("OptionalAssignedToNull")
    @Route
    public static void update_user(UserSession auth, RwTransaction trans, @FromRequest(UpdateUserFromRequest.class) UpdateUser update) throws SQLException, BadRequest {
        var str = new StringBuilder().append("update users set ");

        if(update.name!=null)
            str.append("full_name=:full_name,");
        if(update.bio!=null)
            str.append("bio=:bio,");
        if(update.disp_email!=null)
            str.append("disp_email=:disp_email,");
        if(update.disp_phone_number!=null)
            str.append("disp_phone_number=:disp_phone_number,");

        if(str.charAt(str.length()-1)!=',') return;
        str.deleteCharAt(str.length()-1);
        str.append(" where id=:id");

        try(var stmt = trans.namedPreparedStatement(str.toString())){
            stmt.setLong(":id", auth.user_id());

            if(update.name!=null)
                stmt.setString(":full_name", update.name);
            if(update.bio!=null&&update.bio.isPresent())
                stmt.setString(":bio", update.bio.get());
            if(update.disp_email!=null&&update.disp_email.isPresent())
                stmt.setString(":disp_email", update.disp_email.get());
            if(update.disp_phone_number!=null&&update.disp_phone_number.isPresent())
                stmt.setString(":disp_phone_number", update.disp_phone_number.get());

            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to update user");
        }
        trans.commit();
    }

    /**
     * A marker interface to identify classes as containing user information.
     * @see PrivateUserInfo PrivateUserInfo
     * @see PublicUserInfo PublicUserInfo
     */
    public sealed interface UserInfo permits PrivateUserInfo, PublicUserInfo {}

    /**
     * Represents a user's public info (including name, bio, phone number, email, pfp, and banner)
     */
    public record PublicUserInfo(
        long id,
        String name,
        boolean organizer,
        String bio,
        String disp_email,
        String disp_phone_number,
        long picture,
        long banner
    ) implements UserInfo {
        public static PublicUserInfo make(ResultSet rs) throws SQLException {
            return make(rs, rs.getLong("id"));
        }

        public static PublicUserInfo make(ResultSet rs, long id) throws SQLException {
            var organizer = rs.getBoolean("organizer");
            var disp_email = rs.getString("disp_email");
            if(disp_email==null && organizer)
                disp_email = rs.getString("email");
            return new PublicUserInfo(
                    id,
                    rs.getString("full_name"),
                    organizer,
                    rs.getString("bio"),
                    disp_email,
                    rs.getString("disp_phone_number"),
                    rs.getLong("picture"),
                    rs.getLong("banner")
            );
        }
    }

    /**
     * Represents a user's private and public user info, including all attrs in {@link PublicUserInfo PublicUserInfo}
     * but also with their user id, email, and whether they are an organizer or admin.
     */
    public record PrivateUserInfo(
        long id,
        String name,
        String email,
        boolean organizer,
        boolean admin,
        String bio,
        String disp_email,
        String disp_phone_number,
        long picture,
        long banner
    ) implements UserInfo {}

    /**
     * @return User information as a {@link UserInfo} that is customizably parsable and serializable.
     * @param auth The account that's doing this
     * @param trans The read/write DB transaction this is going to happening in
     * @param id The id of the user to get this info from
     * @throws SQLException
     * @throws Unauthorized
     */
    @Route("/userinfo/<id>")
    public static @Json UserInfo userinfo(@NotRequired UserSession auth, RoTransaction trans, @Path @Nullable Long id) throws SQLException, Unauthorized {
        UserInfo result;
        if(id!=null){
            try(trans; var stmt = trans.namedPreparedStatement("select full_name, email, bio, organizer, disp_email, disp_phone_number, picture, banner from users where id=:id")){
                stmt.setLong(":id", id);
                var rs = stmt.executeQuery();
                result = PublicUserInfo.make(rs, id);
            }
        }else if(auth!=null){
            try(trans; var stmt = trans.namedPreparedStatement("select full_name, bio, disp_email, disp_phone_number, picture, banner from users where id=:id")){
                stmt.setLong(":id", auth.user_id());
                var rs = stmt.executeQuery();
                result = new PrivateUserInfo(
                        auth.user_id(),
                        rs.getString("full_name"),
                        auth.email(),
                        auth.organizer(),
                        auth.admin(),
                        rs.getString("bio"),
                        rs.getString("disp_email"),
                        rs.getString("disp_phone_number"),
                        rs.getLong("picture"),
                        rs.getLong("banner")
                );
            }
        }else
            throw new Unauthorized("No identification present");


        return result;
    }

    /**
     * Represents all the information needed to Register a user (name, email, and password)
     */
    public static class Register{
        public String name;
        public String email;
        public String password;
    }

    /**
     * Registers a user in the DB.
     *
     * @param mail The {@link MailServer} used to send them mail if send_mail_on_register is specified as true
     * @param trans The read/write DB transaction this is going to happening in
     * @param register A {@link Register Register} type (containe name, email, and password).
     * @param send_mail_on_register Whether to send them a confirmation email of their registration
     * @throws SQLException
     * @throws BadRequest
     */
    @Route
    public static void register(MailServer mail, RwTransaction trans, @Body @Json Register register, @Config boolean send_mail_on_register) throws SQLException, BadRequest {
        register.password = Util.hashy((register.password+"\0\0\0\0"+register.email).getBytes());
        try(var stmt = trans.namedPreparedStatement("insert into users values(null, :full_name, :email, :pass, false, false, null, null, null, null, null)")){
            stmt.setString(":full_name", register.name);
            stmt.setString(":email", register.email);
            stmt.setString(":pass", register.password);
            if(stmt.executeUpdate()!=1)
                throw new RuntimeException("Account couldn't be created");
        }catch (SQLiteException e){
            if(e.getResultCode().code==2067){//UNIQUE CONSTRAINTS FAILED
                throw new BadRequest("Account with that email already exists");
            }
            throw e;
        }
        trans.commit();

        if(send_mail_on_register)
            mail.sendMail(message -> {
                message.setRecipients(Message.RecipientType.TO, MailServer.fromStrings(register.email));
                message.setSubject("Welcome!");
                message.setContent("Thank you for registering for an account " + register.name, "text/html");
            });
    }


    /**
     * Sets a user's profile photo (called 'picture' in DB). Also adds this picture to the specified {@link DynamicMediaHandler}.
     *
     * @param session The session that this user is in now
     * @param trans The transaction this is happening in.
     * @param handler the {@link DynamicMediaHandler} that will handle this new image
     * @param data The raw data of the image (must be <= 10 MiB)
     * @return media_id of the newly added image in the dynamic media 'handler'
     * @throws SQLException
     * @throws BadRequest If file is too large (> 10 MiB) or DB update failed.
     */
    @Route
    public static long set_user_picture(UserSession session, RwTransaction trans, DynamicMediaHandler handler, @Body byte[] data) throws SQLException, BadRequest {
        // 10 MiB
        if(data.length > (1<<20)*10){
            throw new BadRequest("File too large, maximum file size is 10 MiB");
        }

        var media_id = handler.add(data);
        long old_media = 0;
        try{
            try(var stmt = trans.namedPreparedStatement("select picture from users where id=:id")){
                stmt.setLong(":id", session.user_id());
                old_media = stmt.executeQuery().getLong(1);
            }

            try(var stmt = trans.namedPreparedStatement("update users set picture=:picture where id=:id")){
                stmt.setLong(":id", session.user_id());
                stmt.setLong(":picture", media_id);
                if(stmt.executeUpdate()!=1)
                    throw new BadRequest("Failed to set picture for user");
            }
            trans.commit();
        }catch (Exception e){
            trans.commit();
            if(media_id!=0){
                handler.delete(media_id);
            }
        }
        if(old_media!=0){
            handler.delete(old_media);
        }

        return media_id;
    }

    /**
     * Sets a user's profile banner (called 'banner' in DB). Also adds this picture to the specified {@link DynamicMediaHandler}.
     *
     * @param session The session that this user is in now
     * @param trans The transaction this is happening in.
     * @param handler the {@link DynamicMediaHandler} that will handle this new image
     * @param data The raw data of the image (must be <= 10 MiB)
     * @return media_id of the newly added image in the dynamic media 'handler'
     * @throws SQLException
     * @throws BadRequest If file is too large (> 10 MiB) or DB update failed.
     */
    @Route
    public static long set_user_banner_picture(UserSession session, RwTransaction trans, DynamicMediaHandler handler, @Body byte[] data) throws SQLException, BadRequest {
        // 10 MiB
        if(data.length > (1<<20)*10){
            throw new BadRequest("File too large, maximum file size is 10 MiB");
        }

        var media_id = handler.add(data);
        long old_media = 0;
        try{
            try(var stmt = trans.namedPreparedStatement("select banner from users where id=:id")){
                stmt.setLong(":id", session.user_id());
                old_media = stmt.executeQuery().getLong("banner");
            }

            try(var stmt = trans.namedPreparedStatement("update users set banner=:banner where id=:id")){
                stmt.setLong(":id", session.user_id());
                stmt.setLong(":banner", media_id);
                if(stmt.executeUpdate()!=1)
                    throw new BadRequest("Failed to set banner picture for user");
            }
            trans.commit();
        }catch (Exception e){
            trans.commit();
            if(media_id!=0){
                handler.delete(media_id);
            }
        }
        if(old_media!=0){
            handler.delete(old_media);
        }

        return media_id;
    }


    public record UserId(String email, long id){}
    public static class PasswordResetManager{
        private HashMap<String, UserId> h1 = new HashMap<>();
        private HashMap<String, UserId> h2 = new HashMap<>();

        public synchronized UserId remove(String token){
            var res = h1.remove(token);
            if(res!=null)return res;
            return h2.remove(token);
        }
        public synchronized boolean put(String token, UserId id){
            if(h1.containsKey(token) || h2.containsKey(token))return false;
            h1.put(token, id);
            return true;
        }

        public synchronized void tick(){
            h2.clear();
            var tmp = h2;
            h2 = h1;
            h1 = tmp;
        }
    }

    @OnMount
    public static void init_reset_password_token_store(WebServer server){
        var timer = server.getManagedState(TimedEvents.class);
        var prm = new PasswordResetManager();
        server.addManagedState(prm);
        timer.addAtRate(prm::tick, 15*60*1000);
    }

    /**
     * Initiates a password reset process by generating a token and sending a reset email to the user.
     *
     * This method checks if a user with the provided email exists in the database, and if so, generates a
     * random token for resetting the user's password. The token is stored and associated with the user's
     * email and ID. A reset email is then sent to the user containing a link with the token, allowing them
     * to reset their password.
     *
     * @param mail The {@link MailServer} used to send the password reset email
     * @param trans The read/write DB transaction this is going to happening in
     * @param email The email address of the user requesting a password reset.
     * @param prm The {@link PasswordResetManager PasswordResetManager} used to store and manage the reset token.
     * @param url_root The root URL of the application to construct the password reset link.
     * @throws SQLException if there is an error while querying the database or interacting with SQL.
     */
    @Route
    public static void reset_password(MailServer mail, RoTransaction trans, @Body String email, PasswordResetManager prm, @Config String url_root) throws SQLException {
        long id;
        try(var stmt = trans.namedPreparedStatement("select id from users where email=:email")){
            stmt.setString(":email", email);
            var ids = SqlSerde.sqlList(stmt.executeQuery(), rs -> rs.getLong("id"));
            if(ids.isEmpty())
                return;
            id = ids.getFirst();
        }
        trans.close();

        var userId = new UserId(email, id);
        String rngStr;
        do{
            var rng = new byte[32];
            new SecureRandom().nextBytes(rng);
            rngStr = Util.base64Str(rng);
        }while(!prm.put(rngStr, userId));

        String finalRngStr = rngStr;
        mail.sendMail(message -> {
            message.setRecipients(Message.RecipientType.TO, MailServer.fromStrings(email));
            message.setSubject("Do you want to reset your password \uD83E\uDD28");
            message.setContent("click this link if you are sure you really really want to reset your password! <a href=\""+url_root+"/account/reset_password?token="+ URLEncoder.encode(finalRngStr, StandardCharsets.UTF_8) +"\">RESET</a>", "text/html");
        });
    }

    public record PasswordReset(String token, String email, String password){}

    /**
     * Completes the password reset process by validating the provided token and updating the user's password.
     *
     * This method takes the reset token and new password from the user, validates the token, ensures the
     * email matches the one associated with the token, and if valid, updates the user's password in the
     * database. It also clears any active sessions associated with the user to force them to log in again.
     *
     * @param mail The {@link MailServer} used to send the password reset email
     * @param trans The read/write DB transaction this is going to happening in
     * @param reset The {@link PasswordReset PasswordReset} object containing the token, email, and new password.
     * @param prm The {@link PasswordResetManager PasswordResetManager} used to validate and remove the reset token.
     * @throws SQLException If there is an error while interacting with the database during password update.
     * @throws BadRequest If the reset token is invalid or the email does not match.
     */
    @Route
    public static void do_reset_password(MailServer mail, RwTransaction trans, @Body @Json PasswordReset reset, PasswordResetManager prm) throws SQLException, BadRequest {
        var id = prm.remove(reset.token);
        if(id==null)
            throw new BadRequest("Link has expired");
        if(!id.email.equals(reset.email))
            throw new BadRequest("Email incorrect");

        var password = Util.hashy((reset.password+"\0\0\0\0"+id.email).getBytes());
        try(var stmt = trans.namedPreparedStatement("update users set pass=:pass where id=:id AND email=:email")){
            stmt.setString(":pass", password);
            stmt.setLong(":id", id.id);
            stmt.setString(":email", id.email);
            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to actually reset password");
        }
        try(var stmt = trans.namedPreparedStatement("delete from sessions where user_id=:user_id")){
            stmt.setLong(":user_id", id.id);
            stmt.execute();
        }
        trans.commit();
    }
}
