package server.web;

import server.web.annotations.Body;
import server.web.annotations.Route;
import server.web.annotations.http.Get;
import server.web.annotations.http.Post;
import server.web.annotations.url.Path;

@SuppressWarnings("unused")
public class MediaAPI {


    @Route("/media/<id>")
    @Get
    public static byte[] media(@Path int id){
        return new byte[0];
    }

//    @Route("/media/<id>")
//    @Post
//    public static int media(@Body byte[] data){
//        return 0;
//    }
//
//    public static void delete_media(int id){
//
//    }
}
