package framework.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation binds methods to an HTTP endpoint (URL path on the server)
 * and an HTTP status code.
 */
@Retention(RetentionPolicy.RUNTIME) // Annotation is retained at runtime, so reflection code can see it and read its values.
@Target(ElementType.METHOD) // Can only be applied to methods
public @interface Route {
    String value() default "";
    int code() default 200; // 'OK' HTTP status code
}
