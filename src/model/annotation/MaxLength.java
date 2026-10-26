// Lab 7 – @MaxLength annotation
// Marks a String field whose length must not exceed a given maximum.
package model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field with a maximum allowed character length.
 * Lab 7: validated at runtime by {@link util.AnnotationValidator}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MaxLength {
    /** Maximum number of characters allowed (inclusive). */
    int value();
    String message() default "Field exceeds maximum length";
}
