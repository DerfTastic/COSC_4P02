package framework.web.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to mark classes that define web routes with @Route.
 */
@Retention(RetentionPolicy.RUNTIME) // Annotation is retained at runtime, so reflection code can see it and read its values.
@Target(ElementType.TYPE) // Can only be applied to types
public @interface Routes {
}
