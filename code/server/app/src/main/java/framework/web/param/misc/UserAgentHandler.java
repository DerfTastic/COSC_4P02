package framework.web.param.misc;

import framework.web.request.Request;
import framework.web.route.RouteParameter;

public class UserAgentHandler implements RouteParameter<String> {
    @Override
    public String construct(Request request) throws Exception {
        return request.exchange.getRequestHeaders().getFirst("User-Agent");
    }
}
