package server.infrastructure.root.media;

import server.infrastructure.DynamicMediaHandler;
import framework.web.annotations.Route;
import framework.web.annotations.Routes;
import framework.web.annotations.url.Path;

@SuppressWarnings("unused")
@Routes
public class MediaAPI {

    @Route("/<id>")
    public static byte[] media(DynamicMediaHandler handler, @Path long id){
        return handler.get(id);
    }
}
