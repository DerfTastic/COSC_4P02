package framework.web.error;

import framework.web.request.Request;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class ClientError extends Exception {
    public final int code;

    public ClientError(int code) {
        this.code = code;
    }

    public ClientError(int code, String message) {
        super(message);
        this.code = code;
    }

    public ClientError(int code, Throwable e) {
        super(e);
        this.code = code;
    }

    public ClientError(int code, String message, Throwable e) {
        super(message, e);
        this.code = code;
    }

    public void respond(Request request) throws IOException {
        request.sendResponse(code, getMessage());
        Logger.getGlobal().log(Level.WARNING, "Route " + request.path() + " Failed to complete", this);
    }

}
