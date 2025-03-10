package server.infrastructure.root.media;

import framework.web.WebServer;
import framework.web.annotations.OnMount;
import server.infrastructure.DynamicMediaHandler;
import framework.web.annotations.Route;
import framework.web.annotations.Routes;
import framework.web.annotations.url.Path;
import server.infrastructure.WebServerImpl;

import java.io.IOException;

@SuppressWarnings("unused")
@Routes
public class MediaAPI {

    @OnMount
    public static void init(WebServer server) throws IOException {
        server.addManagedState(new DynamicMediaHandler());
    }

    @Route("/<id>")
    public static byte[] media(DynamicMediaHandler handler, @Path long id){
        return handler.get(id);
    }
}
