package framework.web.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks methods that should be automatically
 * invoked when the HTTP endpoints are mounted.
 */
@Retention(RetentionPolicy.RUNTIME) // Annotation is retained at runtime, so reflection code can see it and read its values.
@Target({ElementType.METHOD}) // Can only be applied to methods
public @interface OnMount {}
