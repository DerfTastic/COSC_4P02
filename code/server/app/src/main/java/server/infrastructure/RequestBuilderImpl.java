package server.infrastructure;

import framework.db.*;
import framework.web.error.Unauthorized;
import framework.web.request.Request;
import framework.web.route.RequestsBuilder;
import framework.web.route.RouteImpl;
import framework.web.route.RouteParameter;
import server.infrastructure.param.Config;
import server.infrastructure.param.NotRequired;
import server.infrastructure.session.*;

import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.util.HashMap;

public class RequestBuilderImpl extends RequestsBuilder {
    private final HashMap<Class<?>, RouteParameter<?>> parameterHandlerMap = new HashMap<>();
    private final server.Config config;

    public RequestBuilderImpl(server.Config config) {
        initializeParameterHandlers();
        this.config = config;
    }

    private void initializeParameterHandlers() {
        this.addParameterHandler(RoTransaction.class, new RouteParameter<>() {
            @Override
            public RoTransaction construct(Request request) throws SQLException {
                return request.server.getManagedState(DbManager.class).ro_transaction(request.mountedPath());
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
                return request.server.getManagedState(DbManager.class).rw_transaction(request.mountedPath());
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
                return request.server.getManagedState(DbManager.class).ro_conn(request.mountedPath());
            }

            @Override
            public void destruct(Request request, RoConn type) throws Exception {
                type.close();
            }
        });
        addParameterHandler(RwConn.class, new RouteParameter<>() {
            @Override
            public RwConn construct(Request request) throws Exception {
                return request.server.getManagedState(DbManager.class).rw_conn(request.mountedPath());
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
        if(UserSession.class.isAssignableFrom(parameter.getType())){
            var optional = parameter.isAnnotationPresent(NotRequired.class);
            if(AdminSession.class.isAssignableFrom(parameter.getType()))
                return request -> Session.require_admin_session(request, optional);
            if(OrganizerSession.class.isAssignableFrom(parameter.getType()))
                return request -> Session.require_organizer_session(request, optional);
            return request -> Session.require_session(request, optional);
        }

        if(parameter.isAnnotationPresent(Config.class)){
            var config = parameter.getAnnotation(Config.class);
            var name = config.name().equals("!")?parameter.getName():config.name();
            Object value = this.config.get(name, parameter.getType());
            return request -> value;
        }else if(parameterHandlerMap.containsKey(parameter.getType())){
            return parameterHandlerMap.get(parameter.getType());
        }else{
            return super.getParameterHandler(route, parameter);
        }
    }
}
