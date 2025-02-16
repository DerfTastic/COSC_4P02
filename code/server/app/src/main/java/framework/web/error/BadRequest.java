package framework.web.error;

public class BadRequest extends ClientError {
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
