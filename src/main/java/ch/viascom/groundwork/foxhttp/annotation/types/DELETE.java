package ch.viascom.groundwork.foxhttp.annotation.types;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This @DELETE annotation sets the request type as DELETE.
 *
 * The annotation value (optional, default := nothing) can be used to define a new endpoint, which will be attached at the end of the defined path.
 *
 * @author patrick.boesch@viascom.ch
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DELETE {

    String value() default "";

    boolean completePath() default false;
}