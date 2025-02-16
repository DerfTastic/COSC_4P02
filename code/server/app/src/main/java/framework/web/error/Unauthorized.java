package framework.web.error;

public class Unauthorized extends ClientError {
    public Unauthorized() {
        super(401);
    }

    public Unauthorized(String message) {
        super(401, message);
    }
}
