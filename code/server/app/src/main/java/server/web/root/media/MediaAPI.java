package server.web.root.media;

import server.DynamicMediaHandler;
import server.web.annotations.Route;
import server.web.annotations.Routes;
import server.web.annotations.url.Path;

@SuppressWarnings("unused")
@Routes
public class MediaAPI {

    @Route("/<id>")
    public static byte[] media(DynamicMediaHandler handler, @Path long id){
        return handler.get(id);
    }
}
