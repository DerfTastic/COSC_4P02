package framework.web.request;

import java.io.IOException;

public interface RequestHandler {
    void handle(Request request) throws IOException;
}
