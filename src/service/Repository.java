// Lab 13 – Generic Repository<T>
// Uses @Id reflection (via AnnotationValidator) to key objects by their ID.
package service;

import util.AnnotationValidator;

import java.util.*;

/**
 * Generic in-memory repository keyed by the field annotated with {@link model.annotation.Id}.
 * Lab 13: demonstrates generics with bounded type parameter and reflection.
 *
 * @param <T> the domain entity type (must have exactly one {@code @Id}-annotated field)
 */
public class Repository<T> {

    private final Map<Object, T> store = new LinkedHashMap<>();

    /** Persist an entity; its @Id value becomes the key. */
    public void save(T entity) {
        Object key = AnnotationValidator.getIdValue(entity);
        if (key == null)
            throw new IllegalArgumentException(
                    "Entity must have a field annotated with @Id: " + entity);
        store.put(key, entity);
    }

    /** Find by the @Id value; returns Optional.empty() if absent. */
    public Optional<T> findById(Object id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Remove by the @Id value; returns true if the entity existed. */
    public boolean deleteById(Object id) {
        return store.remove(id) != null;
    }

    /** All stored entities as an unmodifiable list. */
    public List<T> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    /** Number of stored entities. */
    public int count() { return store.size(); }

    /** True if no entities are stored. */
    public boolean isEmpty() { return store.isEmpty(); }
}
