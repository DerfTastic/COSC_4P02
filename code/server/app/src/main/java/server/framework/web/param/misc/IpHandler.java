package server.framework.web.param.misc;

import server.framework.web.request.Request;
import server.framework.web.route.RouteParameter;

import java.net.InetAddress;

public class IpHandler implements RouteParameter<InetAddress> {
    @Override
    public InetAddress construct(Request request) throws Exception {
        if(request.exchange.getRequestHeaders().containsKey("Cf-connecting-ip")){
            return InetAddress.getByName(request.exchange.getRequestHeaders().getFirst("Cf-connecting-ip"));
        }
        return request.exchange.getRemoteAddress().getAddress();
    }
}
