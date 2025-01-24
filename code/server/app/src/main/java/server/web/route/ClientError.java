package server.web.route;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class ClientError extends Exception {
    public final int code;

    private ClientError(int code) {
        this.code = code;
    }

    private ClientError(int code, String message) {
        super(message);
        this.code = code;
    }

    private ClientError(int code, Throwable e) {
        super(e);
        this.code = code;
    }

    private ClientError(int code, String message, Throwable e) {
        super(message, e);
        this.code = code;
    }

    public void respond(Request request) throws IOException {
        request.sendResponse(request, code, getMessage());
        Logger.getGlobal().log(Level.WARNING, "Route " + request.path() + " Faield to complete", this);
    }

    public static class BadRequest extends ClientError{
        public BadRequest() {
            super(400);
        }

        public BadRequest(String message) {
            super(400, message);
        }

        public BadRequest(Throwable e) {
            super(400, e);
        }
        public BadRequest(String message, Throwable e) {
            super(400, message, e);
        }

    }

    public static class Unauthorized extends ClientError{
        public Unauthorized() {
            super(401);
        }

        public Unauthorized(String message) {
            super(401, message);
        }
    }

    public static class Forbidden extends ClientError{
        public Forbidden() {
            super(403);
        }

        public Forbidden(String message) {
            super(403, message);
        }
    }

    public static class MethodNotAllowed extends ClientError{
        public MethodNotAllowed(){super(405);}
    }
}
