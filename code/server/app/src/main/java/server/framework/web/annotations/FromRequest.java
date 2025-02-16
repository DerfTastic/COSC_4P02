package server.framework.web.annotations;

import server.framework.web.route.RouteParameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FromRequest {
    Class<? extends RouteParameter<?>> value();
}
