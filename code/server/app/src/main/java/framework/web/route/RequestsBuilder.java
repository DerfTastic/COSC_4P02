package framework.web.route;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import framework.util.func.Function1;
import framework.web.annotations.Body;
import framework.web.annotations.FromRequest;
import framework.web.annotations.Json;
import framework.web.request.Request;
import framework.web.annotations.url.Nullable;
import framework.web.annotations.url.Path;
import framework.web.annotations.url.QueryFlag;
import framework.web.annotations.url.QueryValue;
import framework.web.error.BadRequest;
import framework.util.TypeReflect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

public class RequestsBuilder {

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
                        throw new BadRequest("Failed to construct parameter " + param + " for method " + route.sourceMethod, e);
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
                        throw new BadRequest("Failed to construct parameter " + param + " for method " + route.sourceMethod, e);
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
                        throw new BadRequest("Failed to construct parameter " + param + " for method " + route.sourceMethod, e);
                    }
                };
            }
        }
        if(param.getType().equals(HttpExchange.class)) return request -> request.exchange;
        if(param.getType().equals(Request.class)) return request -> request;
        return request -> request.getServer().getManagedState(param.getType());
    }

    private StringsAdapter<?> getParameterStringAdapter(Class<?> type, boolean nullable) {
        if(type.isPrimitive()&&nullable) throw new RuntimeException("Cannot be nullable primitive");
        if(type.isPrimitive()) return getParameterStringAdapter(TypeReflect.wrapPrimitives(type), nullable);

        Map<Class<?>, Function1<Object, String>> map = Map.of(
                Byte.class, Byte::parseByte,
                Short.class, Short::parseShort,
                Integer.class, Integer::parseInt,
                Long.class, Long::parseLong,
                Boolean.class, Boolean::parseBoolean,
                Float.class, Float::parseFloat,
                Double.class, Double::parseDouble,
                String.class, s -> s
        );
        if(map.containsKey(type)){
            if (nullable)
                return (StringSingleNullableAdapter<?>)map.get(type)::call;
            else
                return (StringSingleAdapter<?>)map.get(type)::call;
        }

        if (type.isArray()) {
            var innerType = type.getComponentType();
            boolean innerNullable = innerType.isPrimitive()?false:nullable;
            var sa = getParameterStringAdapter(innerType, innerNullable);

            return switch(innerType){
                case Class<?> cl when cl == byte.class -> (StringListAdapter<?>) list -> {
                    if(nullable&&list==null)return null;
                    var l = new short[list.size()];
                    for (int i = 0; i < l.length; i ++)
                        l[i] = (short) sa.parseSingular(list.get(i));
                    return l;
                };
                case Class<?> cl when cl == short.class -> (StringListAdapter<?>) list -> {
                    if(nullable&&list==null)return null;
                    var l = new short[list.size()];
                    for (int i = 0; i < l.length; i ++)
                        l[i] = (short) sa.parseSingular(list.get(i));
                    return l;
                };
                case Class<?> cl when cl == int.class -> (StringListAdapter<?>) list -> {
                    if(nullable&&list==null)return null;
                    var l = new int[list.size()];
                    for (int i = 0; i < l.length; i ++)
                        l[i] = (int) sa.parseSingular(list.get(i));
                    return l;
                };
                case Class<?> cl when cl == long.class -> (StringListAdapter<?>) list -> {
                    if(nullable&&list==null)return null;
                    var l = new long[list.size()];
                    for (int i = 0; i < l.length; i ++)
                        l[i] = (long) sa.parseSingular(list.get(i));
                    return l;
                };
                case Class<?> cl when cl == float.class -> (StringListAdapter<?>) list -> {
                    if(nullable&&list==null)return null;
                    var l = new float[list.size()];
                    for (int i = 0; i < l.length; i ++)
                        l[i] = (float) sa.parseSingular(list.get(i));
                    return l;
                };
                case Class<?> cl when cl == double.class -> (StringListAdapter<?>) list -> {
                    if(nullable&&list==null)return null;
                    var l = new float[list.size()];
                    for (int i = 0; i < l.length; i ++)
                        l[i] = (float) sa.parseSingular(list.get(i));
                    return l;
                };
                default -> (StringListAdapter<?>) list -> {
                    if(nullable&&list==null)return null;
                    var l = new Object[list.size()];
                    for (int i = 0; i < l.length; i ++)
                        l[i] = sa.parseSingular(list.get(i));
                    return l;
                };
            };
        }
        throw new RuntimeException("Cannot get String handler for " + type);
    }

    private StringsAdapter<?> getParameterStringAdapter(Parameter param) {
        if(param.getAnnotation(Json.class) != null){
            //TODO a bunch of CPU time is spent here, consider using a faster json serializer?
            return (StringSingleNullableAdapter<?>) str -> new Gson().fromJson(str, param.getType());
        }else{
            return getParameterStringAdapter(param.getType(), param.getAnnotation(Nullable.class)!=null);
        }
    }

    protected RouteReturn<?> getReturnHandler(RouteImpl route, Method method) throws Throwable{
        if(method.getReturnType().equals(void.class)){
            return (request, data) -> request.sendResponse("");
        }else if(method.getAnnotation(Json.class) != null){
            return (request, data) -> request.sendResponse(data);
        }else if(method.getReturnType().equals(byte[].class)){
            return (request, data) -> request.sendResponse((byte[])data);
        }else if(method.getReturnType().equals(String.class)){
            return (request, data) -> request.sendResponse((String)data);
        }else{
            return (request, data) -> request.sendResponse(data.toString());
        }
    }
}
