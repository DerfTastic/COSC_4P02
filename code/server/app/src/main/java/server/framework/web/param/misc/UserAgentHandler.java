package server.framework.web.param.misc;

import server.framework.web.route.Request;
import server.framework.web.route.RouteParameter;

public class UserAgentHandler implements RouteParameter<String> {
    @Override
    public String construct(Request request) throws Exception {
        return request.exchange.getRequestHeaders().getFirst("User-Agent");
    }
}
