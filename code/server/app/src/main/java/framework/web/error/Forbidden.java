package framework.web.error;

public class Forbidden extends ClientError {
    public Forbidden() {
        super(403);
    }

    public Forbidden(String message) {
        super(403, message);
    }
}
