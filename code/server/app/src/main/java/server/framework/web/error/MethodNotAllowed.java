package server.framework.web.error;

public class MethodNotAllowed extends ClientError {
    public MethodNotAllowed() {
        super(405);
    }
}
