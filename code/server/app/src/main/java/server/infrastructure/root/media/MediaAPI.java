package server.infrastructure.root.media;

import server.infrastructure.DynamicMediaHandler;
import server.framework.web.annotations.Route;
import server.framework.web.annotations.Routes;
import server.framework.web.annotations.url.Path;

@SuppressWarnings("unused")
@Routes
public class MediaAPI {

    @Route("/<id>")
    public static byte[] media(DynamicMediaHandler handler, @Path long id){
        return handler.get(id);
    }
}
