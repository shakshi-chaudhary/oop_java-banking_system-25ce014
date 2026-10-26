// Lab 7 – AnnotationValidator
// Uses reflection to inspect @Positive and @MaxLength on domain objects.
package util;

import exception.ValidationException;
import model.annotation.MaxLength;
import model.annotation.Positive;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection-based validator that reads {@link Positive} and {@link MaxLength}
 * annotations from domain object fields at runtime.
 * Lab 7: demonstrates {@code java.lang.reflect} API.
 */
public final class AnnotationValidator {

    private AnnotationValidator() {}

    /**
     * Validates all annotated fields in the given object.
     * Collects all violations before throwing.
     *
     * @throws ValidationException if any field violates its annotation constraint
     */
    public static void validate(Object obj) throws ValidationException {
        if (obj == null) throw new ValidationException("object", "must not be null");

        List<String> violations = new ArrayList<>();
        Class<?> clazz = obj.getClass();

        // Walk the class hierarchy to pick up inherited annotated fields
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    checkPositive(obj, field, violations);
                    checkMaxLength(obj, field, violations);
                } catch (IllegalAccessException e) {
                    violations.add("Cannot access field: " + field.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }

        if (!violations.isEmpty()) {
            throw new ValidationException("annotation", String.join("; ", violations));
        }
    }

    private static void checkPositive(Object obj, Field field, List<String> violations)
            throws IllegalAccessException {
        Positive ann = field.getAnnotation(Positive.class);
        if (ann == null) return;

        Object value = field.get(obj);
        if (value instanceof Number n && n.doubleValue() <= 0) {
            violations.add(field.getName() + " " + ann.message());
        }
    }

    private static void checkMaxLength(Object obj, Field field, List<String> violations)
            throws IllegalAccessException {
        MaxLength ann = field.getAnnotation(MaxLength.class);
        if (ann == null) return;

        Object value = field.get(obj);
        if (value instanceof String s && s.length() > ann.value()) {
            violations.add(field.getName() + ": " + ann.message() +
                           " (max=" + ann.value() + ", actual=" + s.length() + ")");
        }
    }

    /**
     * Returns the value of the field annotated with {@link model.annotation.Id}
     * via reflection.  Used by {@link service.Repository}.
     */
    public static Object getIdValue(Object obj) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(model.annotation.Id.class)) {
                    field.setAccessible(true);
                    try { return field.get(obj); }
                    catch (IllegalAccessException e) { throw new RuntimeException(e); }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
