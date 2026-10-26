// Lab 7 – @Positive annotation
// Marks a numeric field that must be > 0 (enforced by AnnotationValidator).
package model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a numeric field that must have a strictly positive value.
 * Lab 7: validated at runtime by {@link util.AnnotationValidator}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Positive {
    String message() default "Field must be positive";
}
