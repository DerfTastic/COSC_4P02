package server.web.param.misc;

import server.web.route.Request;
import server.web.route.RouteParameter;

public class UserAgentHandler implements RouteParameter<String> {
    @Override
    public String construct(Request request) throws Exception {
        return request.exchange.getRequestHeaders().getFirst("User-Agent");
    }
}
