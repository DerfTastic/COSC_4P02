package server.web.route;

import com.google.common.reflect.ClassPath;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import server.web.WebServer;
import server.web.annotations.*;
import server.web.annotations.url.Nullable;
import server.web.annotations.url.Path;
import server.web.annotations.url.QueryFlag;
import server.web.annotations.url.QueryValue;
import util.TypeReflect;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class RoutesBuilder {
    @SuppressWarnings("UnstableApiUsage")
    public static Stream<Class<?>> findAllClassesInPackage(String packageName) {
        try {
            return ClassPath.from(RoutesBuilder.class.getClassLoader())
                    .getTopLevelClassesRecursive(packageName)
                    .stream().map(ClassPath.ClassInfo::load);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void mountRoutes(WebServer server, String parentPath, String classPath){
        try{
            for(var clazz : findAllClassesInPackage(classPath).toList()){
                var pack = clazz.getPackage().getName()+"/";
                var path = parentPath+pack.substring(classPath.length()+1).replace(".", "/");
                if(clazz.isAnnotationPresent(Routes.class)){
                    attachRoutes(server, path, clazz);
                }else if(clazz.isAnnotationPresent(Handler.class)){
                    attachHandler(server, path, (Class<? extends HttpHandler>) clazz);
                }
            }
        }catch (Exception e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to load routes", e);
        }
    }

    public void attachHandler(WebServer server, String parentPath, Class<? extends HttpHandler> handlerClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        server.server.createContext(parentPath, handlerClass.getConstructor().newInstance());
        Logger.getGlobal().log(Level.FINE, "Route mounted at: '" + parentPath + "' -> "+handlerClass);
    }

    public void attachRoutes(WebServer server, String parentPath, Class<?> routeClass) {
        for(var method : routeClass.getDeclaredMethods()){
            if(method.getAnnotation(server.web.annotations.Route.class) == null) continue;
            var route = new RouteImpl(method, parentPath, this);
            route.addRoute(server);
            Logger.getGlobal().log(Level.FINE, "Route mounted at: '" + route.path + "' -> "+route.sourceMethod);
        }
    }


    protected RouteParameter<?> getParameterHandler(RouteImpl route, Parameter param) throws Throwable{
        if(param.getAnnotation(FromRequest.class) != null){
            var fr = param.getAnnotation(FromRequest.class).value();
            return fr.getConstructor().newInstance();
        }else if(param.getAnnotation(QueryFlag.class) != null){
            var qv = param.getAnnotation(QueryFlag.class);
            String name = qv.value().isBlank()?param.getName():qv.value();
            if(param.getType().equals(int.class)|param.getType().equals(Integer.class)){
                return request -> request.countQueryParam(name);
            }else if(param.getType().equals(boolean.class)|param.getType().equals(Boolean.class)){
                return request -> request.hasQueryParam(name);
            }else
                throw new RuntimeException("Unsupported type for @QueryValue on parameter " + param + " on method " + route.sourceMethod);
        }else{
            if(param.getAnnotation(Body.class) != null){
                if(param.getType().equals(byte[].class)){
                    return request -> request.exchange.getRequestBody().readAllBytes();
                }
                StringsAdapter<?> stringsAdapter = getParameterStringAdapter(param);
                return request -> {
                    try {
                        return stringsAdapter.parseSingular(new String(request.exchange.getRequestBody().readAllBytes()));
                    } catch (Throwable e) {
                        throw new ClientError.BadRequest("Failed to construct parameter " + param + " for method " + route.sourceMethod, e);
                    }
                };
            }else if(param.getAnnotation(Path.class) != null){
                StringsAdapter<?> stringsAdapter = getParameterStringAdapter(param);
                var path = param.getAnnotation(Path.class);
                var value = path.value().isEmpty()?param.getName():path.value();
                var index = route.findPathPartIndex(value);
                return request -> {
                    try {
                        return stringsAdapter.parseSingular(request.getPathPart(index));
                    } catch (Throwable e) {
                        throw new ClientError.BadRequest("Failed to construct parameter " + param + " for method " + route.sourceMethod, e);
                    }
                };
            }else if(param.getAnnotation(QueryValue.class) != null){
                StringsAdapter<?> stringsAdapter = getParameterStringAdapter(param);
                var qf = param.getAnnotation(QueryValue.class);
                var value = qf.value().isEmpty()?param.getName():qf.value();
                return request -> {
                    try {
                        return stringsAdapter.parseMultiple(request.getQueryParam(value));
                    } catch (Throwable e) {
                        throw new ClientError.BadRequest("Failed to construct parameter " + param + " for method " + route.sourceMethod, e);
                    }
                };
            }
        }
        if(param.getType().equals(HttpExchange.class)) return request -> request.exchange;
        if(param.getType().equals(Request.class)) return request -> request;
        return request -> request.getServer().getManagedResource(param.getType());
    }

    private StringsAdapter<?> getParameterStringAdapter(Class<?> type, boolean nullable) {
        if(type.isPrimitive()) return getParameterStringAdapter(TypeReflect.wrapPrimitives(type), nullable);

        if (type.equals(Integer.class)) {
            if(nullable)
                return (StringSingleNullableAdapter<?>)Integer::parseInt;
            else
                return (StringSingleAdapter<?>)Integer::parseInt;
        } else if (type.equals(Short.class)) {
            if(nullable)
                return (StringSingleNullableAdapter<?>)Short::parseShort;
            else
                return (StringSingleAdapter<?>)Short::parseShort;
        } else if (type.equals(Byte.class)) {
            if(nullable)
                return (StringSingleNullableAdapter<?>)Byte::parseByte;
            else
                return (StringSingleAdapter<?>)Byte::parseByte;
        } else if (type.equals(Long.class)) {
            if(nullable)
                return (StringSingleNullableAdapter<?>)Long::parseLong;
            else
                return (StringSingleAdapter<?>)Long::parseLong;
        } else if (type.equals(Boolean.class)) {
            if(nullable)
                return (StringSingleNullableAdapter<?>)Boolean::parseBoolean;
            else
                return (StringSingleAdapter<?>)Boolean::parseBoolean;
        } else if (type.equals(Float.class)) {
            if(nullable)
                return (StringSingleNullableAdapter<?>)Float::parseFloat;
            else
                return (StringSingleAdapter<?>)Float::parseFloat;
        } else if (type.equals(Double.class)) {
            if(nullable)
                return (StringSingleNullableAdapter<?>)Double::parseDouble;
            else
                return (StringSingleAdapter<?>)Double::parseDouble;
        } else if (type.equals(String.class)) {
            if(nullable)
                return (StringSingleNullableAdapter<?>) s->s;
            else
                return (StringSingleAdapter<?>) s->s;
        }
        if (type.equals(int[].class)) {
            var sa = getParameterStringAdapter(type.getComponentType(), nullable);
            return (StringListAdapter<?>) list -> {
                if(nullable&&list==null)return null;
                var l = new int[list.size()];
                for (int i = 0; i < l.length; i ++)
                    l[i] = (int) sa.parseSingular(list.get(i));
                return l;
            };
        } else if (type.equals(short[].class)) {
            var sa = getParameterStringAdapter(type.getComponentType(), nullable);
            return (StringListAdapter<?>) list -> {
                if(nullable&&list==null)return null;
                var l = new short[list.size()];
                for (int i = 0; i < l.length; i ++)
                    l[i] = (short) sa.parseSingular(list.get(i));
                return l;
            };
        } else if (type.equals(byte[].class)) {
            var sa = getParameterStringAdapter(type.getComponentType(), nullable);
            return (StringListAdapter<?>) list -> {
                if(nullable&&list==null)return null;
                var l = new short[list.size()];
                for (int i = 0; i < l.length; i ++)
                    l[i] = (short) sa.parseSingular(list.get(i));
                return l;
            };
        } else if (type.equals(long[].class)) {
            var sa = getParameterStringAdapter(type.getComponentType(), nullable);
            return (StringListAdapter<?>) list -> {
                if(nullable&&list==null)return null;
                var l = new long[list.size()];
                for (int i = 0; i < l.length; i ++)
                    l[i] = (long) sa.parseSingular(list.get(i));
                return l;
            };
        } else if (type.equals(boolean[].class)) {
            var sa = getParameterStringAdapter(type.getComponentType(), nullable);
            return (StringListAdapter<?>) list -> {
                if(nullable&&list==null)return null;
                var l = new boolean[list.size()];
                for (int i = 0; i < l.length; i ++)
                    l[i] = (boolean) sa.parseSingular(list.get(i));
                return l;
            };
        } else if (type.equals(float[].class)) {
            var sa = getParameterStringAdapter(type.getComponentType(), nullable);
            return (StringListAdapter<?>) list -> {
                if(nullable&&list==null)return null;
                var l = new float[list.size()];
                for (int i = 0; i < l.length; i ++)
                    l[i] = (float) sa.parseSingular(list.get(i));
                return l;
            };
        } else if (type.equals(double[].class)) {
            var sa = getParameterStringAdapter(type.getComponentType(), nullable);
            return (StringListAdapter<?>) list -> {
                if(nullable&&list==null)return null;
                var l = new float[list.size()];
                for (int i = 0; i < l.length; i ++)
                    l[i] = (float) sa.parseSingular(list.get(i));
                return l;
            };
        } else if (type.isArray()) {
            if (!type.getComponentType().isArray()) {
                var sa = getParameterStringAdapter(type.getComponentType(), nullable);
                return (StringListAdapter<?>) list -> {
                    if(nullable&&list==null)return null;
                    var l = new Object[list.size()];
                    for (int i = 0; i < l.length; i ++)
                        l[i] = sa.parseSingular(list.get(i));
                    return l;
                };
            }
        }
        throw new RuntimeException("Cannot get String handler for " + type);
    }

    private StringsAdapter<?> getParameterStringAdapter(Parameter param) {
        if(param.getAnnotation(Json.class) != null){
            return (StringSingleNullableAdapter<?>) str -> new Gson().fromJson(str, param.getType());
        }else{
            return getParameterStringAdapter(param.getType(), param.getAnnotation(Nullable.class)!=null);
        }
    }

    protected RouteReturn<?> getReturnHandler(RouteImpl route, Method method) throws Throwable{
        if(method.getReturnType().equals(void.class)){
            return (request, data) -> request.sendResponse(request, "");
        }else if(method.getAnnotation(Json.class) != null){
            return (request, data) -> request.sendResponse(request, data);
        }else if(method.getReturnType().equals(byte[].class)){
            return (request, data) -> request.sendResponse(request, (byte[])data);
        }else if(method.getReturnType().equals(String.class)){
            return (request, data) -> request.sendResponse(request, (String)data);
        }else{
            return (request, data) -> request.sendResponse(request, data.toString());
        }
    }
}
