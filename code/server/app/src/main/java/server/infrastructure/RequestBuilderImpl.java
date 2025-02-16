package server.infrastructure;

import server.framework.db.*;
import server.framework.web.request.Request;
import server.framework.web.route.RequestsBuilder;
import server.framework.web.route.RouteImpl;
import server.framework.web.route.RouteParameter;

import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.util.HashMap;

public class RequestBuilderImpl extends RequestsBuilder {
    private final HashMap<Class<?>, RouteParameter<?>> parameterHandlerMap = new HashMap<>();

    public RequestBuilderImpl(){
        initializeParameterHandlers();
    }

    private void initializeParameterHandlers() {
        this.addParameterHandler(RoTransaction.class, new RouteParameter<>() {
            @Override
            public RoTransaction construct(Request request) throws SQLException {
                return request.server.getManagedResource(DbManager.class).ro_transaction();
            }

            @Override
            public void destructError(Request request, RoTransaction type) throws Exception {
                type.close();
            }

            @Override
            public void destruct(Request request, RoTransaction type) throws SQLException {
                type.tryCommit();
            }
        });
        this.addParameterHandler(RwTransaction.class, new RouteParameter<>() {
            @Override
            public RwTransaction construct(Request request) throws SQLException {
                return request.server.getManagedResource(DbManager.class).rw_transaction();
            }

            @Override
            public void destructError(Request request, RwTransaction type) throws Exception {
                type.close();
            }

            @Override
            public void destruct(Request request, RwTransaction type) throws SQLException {
                type.tryCommit();
            }
        });
        addParameterHandler(RoConn.class, new RouteParameter<>() {
            @Override
            public RoConn construct(Request request) throws Exception {
                return request.server.getManagedResource(DbManager.class).ro_conn();
            }

            @Override
            public void destruct(Request request, RoConn type) throws Exception {
                type.close();
            }
        });
        addParameterHandler(RwConn.class, new RouteParameter<>() {
            @Override
            public RwConn construct(Request request) throws Exception {
                return request.server.getManagedResource(DbManager.class).rw_conn();
            }

            @Override
            public void destruct(Request request, RwConn type) throws Exception {
                type.close();
            }
        });
    }

    private <T> void addParameterHandler(Class<T> clazz, RouteParameter<T> handler){
        parameterHandlerMap.put(clazz, handler);
    }

    @Override
    protected RouteParameter<?> getParameterHandler(RouteImpl route, Parameter parameter) throws Throwable {
        if(parameterHandlerMap.containsKey(parameter.getType())){
            return parameterHandlerMap.get(parameter.getType());
        }else{
            return super.getParameterHandler(route, parameter);
        }
    }
}
