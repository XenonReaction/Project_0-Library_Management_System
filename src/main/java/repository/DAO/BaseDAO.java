package repository.DAO;

import java.util.List;
import java.util.Optional;

/**
 * A generic DAO interface that defines common CRUD operations.
 *
 * @param <T> The type of model object this DAO manages.
 */
public interface BaseDAO<T> {

    /**
     * Saves a new object into the persistence layer.
     *
     * @param t the object to save
     * @return the saved object (with auto-generated fields populated, like ID)
     */
    T save(T t);

    /**
     * Finds an object by its unique ID.
     *
     * @param id the ID to search for
     * @return an Optional containing the found object, or empty if not found
     */
    Optional<T> findById(long id);

    /**
     * Retrieves all objects of this type.
     *
     * @return a List of all objects
     */
    List<T> findAll();

    /**
     * Updates an existing object in the persistence layer.
     *
     * @param t the object with updated fields
     */
    void update(T t);

    /**
     * Deletes an object by its ID.
     *
     * @param id the ID of the object to delete
     */
    void deleteById(long id);
}
