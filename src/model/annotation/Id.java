// Lab 7 – @Id annotation
// Marks the primary-key field of a domain object (used by AnnotationValidator
// and Repository<T> via reflection).
package model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the unique identifier of a domain object.
 * Lab 7: custom annotation retained at runtime for reflection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {
    // marker – no elements needed
}
