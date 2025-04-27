package framework.web.annotations;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation allows clients to fine-tune how exactly JSON data is parsed and/or serialized.
 */
@Retention(RetentionPolicy.RUNTIME) // Annotation is retained at runtime, so reflection code can see it and read its values.
@Target({ElementType.PARAMETER, ElementType.METHOD}) // Can be applied to method parameters or methods themselves
public @interface Json {
    JSONReader.Feature[] read() default {};
    JSONWriter.Feature[] write() default {};
}
