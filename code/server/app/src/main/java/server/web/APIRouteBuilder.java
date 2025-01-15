package server.web;

import com.sun.net.httpserver.HttpExchange;
import server.db.DbConnection;
import server.db.DbManager;
import server.db.Transaction;
import server.web.route.RouteImpl;
import server.web.route.RouteParameter;
import server.web.route.RoutesBuilder;

import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.util.HashMap;

public class APIRouteBuilder extends RoutesBuilder {
    private final DbManager db;
    private final HashMap<Class<?>, RouteParameter<?>> parameterHandlerMap = new HashMap<>();

    public APIRouteBuilder(DbManager db){
        super(API.class);
        this.db = db;
        initializeParameterHandlers();
    }

    private void initializeParameterHandlers() {
        this.addParameterHandler(Transaction.class, new RouteParameter<>() {
            @Override
            public Transaction construct(RouteImpl.Request request) throws SQLException {
                return db.transaction();
            }

            @Override
            public void destructError(RouteImpl.Request request, Transaction type) throws Exception {
                type.close();
            }

            @Override
            public void destruct(RouteImpl.Request request, Transaction type) throws SQLException {
                type.tryCommit();
            }
        });
        addParameterHandler(DbConnection.class, new RouteParameter<>() {
            @Override
            public DbConnection construct(RouteImpl.Request request) throws Exception {
                return db.conn();
            }

            @Override
            public void destruct(RouteImpl.Request request, DbConnection type) throws Exception {
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
