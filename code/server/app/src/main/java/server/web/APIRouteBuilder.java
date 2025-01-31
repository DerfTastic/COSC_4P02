package server.web;

import server.db.*;
import server.web.route.Request;
import server.web.route.RouteImpl;
import server.web.route.RouteParameter;
import server.web.route.RoutesBuilder;

import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.util.HashMap;

public class APIRouteBuilder extends RoutesBuilder {
    private final DbManager db;
    private final HashMap<Class<?>, RouteParameter<?>> parameterHandlerMap = new HashMap<>();

    public APIRouteBuilder(WebServer server){
        this.db = server.getManagedResource(DbManager.class);
        initializeParameterHandlers();
    }

    private void initializeParameterHandlers() {
        this.addParameterHandler(RoTransaction.class, new RouteParameter<>() {
            @Override
            public RoTransaction construct(Request request) throws SQLException {
                return db.ro_transaction();
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
                return db.rw_transaction();
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
                return db.ro_conn();
            }

            @Override
            public void destruct(Request request, RoConn type) throws Exception {
                type.close();
            }
        });
        addParameterHandler(RwConn.class, new RouteParameter<>() {
            @Override
            public RwConn construct(Request request) throws Exception {
                return db.rw_conn();
            }

            @Override
            public void destruct(Request request, RwConn type) throws Exception {
                type.close();
            }
        });
        addParameterHandler(DbManager.class, request -> db);
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
