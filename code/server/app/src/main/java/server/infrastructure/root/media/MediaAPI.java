package server.infrastructure.root.media;

import framework.web.WebServer;
import framework.web.annotations.OnMount;
import server.infrastructure.DynamicMediaHandler;
import server.infrastructure.FileDynamicMediaHandler;
import framework.web.annotations.Route;
import framework.web.annotations.Routes;
import framework.web.annotations.url.Path;
import server.infrastructure.param.Config;

import java.io.IOException;

@SuppressWarnings("unused")
@Routes
public class MediaAPI {

    @OnMount
    public static void init(WebServer server, @Config String dynamic_media_path, @Config long dynamic_media_cache_size) throws IOException {
        server.addManagedState(
                new FileDynamicMediaHandler(dynamic_media_path, dynamic_media_cache_size, 8, 64),
                DynamicMediaHandler.class
        );
    }

    @Route("/<id>")
    public static byte[] media(DynamicMediaHandler handler, @Path long id){
        return handler.get(id);
    }
}
