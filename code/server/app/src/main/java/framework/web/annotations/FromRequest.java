package framework.web.annotations;

import framework.web.route.RouteParameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This parameter-level annotation binds a method parameter
 * to data extracted from an incoming HTTP request.
 */
@Retention(RetentionPolicy.RUNTIME) // Annotation is retained at runtime, so reflection code can see it and read its values.
@Target(ElementType.PARAMETER) // Can only be applied to method parameters
public @interface FromRequest {
    Class<? extends RouteParameter<?>> value(); // Specifies return type must be a child class of RouteParameter<?>
}
