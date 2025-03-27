package server.infrastructure.root.media;

import framework.web.WebServer;
import framework.web.annotations.OnMount;
import server.Config;
import server.infrastructure.DynamicMediaHandler;
import server.infrastructure.FileDynamicMediaHandler;
import framework.web.annotations.Route;
import framework.web.annotations.Routes;
import framework.web.annotations.url.Path;

import java.io.IOException;

@SuppressWarnings("unused")
@Routes
public class MediaAPI {

    @OnMount
    public static void init(WebServer server, Config config) throws IOException {
        server.addManagedState(
                new FileDynamicMediaHandler(config.dynamic_media_path, config.dynamic_media_cache_size, 8, 64),
                DynamicMediaHandler.class
        );
    }

    @Route("/<id>")
    public static byte[] media(DynamicMediaHandler handler, @Path long id){
        return handler.get(id);
    }
}
