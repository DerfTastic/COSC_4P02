package server.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Util {

    public static String escapeHTML(String str) {
        return str.chars().mapToObj(c -> c > 127 || "\"'<>&".indexOf(c) != -1 ?
                "&#" + c + ";" : String.valueOf((char) c)).collect(Collectors.joining());
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = message.getBytes();
        sendResponse(exchange, statusCode, response);
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, byte[] content) throws IOException {
        exchange.sendResponseHeaders(statusCode, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
        logResponse(exchange.getRequestURI().toString(), statusCode, content.length);
    }

    public static void logResponse(String path, int code, int len){
        var frame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(
                e -> e
                    .skip(1)
                    .filter(f ->
                        !f.getMethodName().equals("sendResponse")
                    )
                    .findFirst()
            ).get();
        Logger.getGlobal().logp(
                Level.FINE, frame.getClassName(), frame.getMethodName(),
                "Requested: '"+path+"'" + " Response Code: "+code+" Content Length: "+len
        );
    }

}
