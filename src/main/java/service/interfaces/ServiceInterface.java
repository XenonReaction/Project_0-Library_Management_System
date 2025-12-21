package service.interfaces;

import java.util.List;
import java.util.Optional;

/**
 * Generic service interface for CRUD operations on service-layer models.
 *
 * @param <T> The model type (e.g., Book, Member, Loan)
 * @param <ID> The identifier type (recommended: Long for BIGINT)
 */
public interface ServiceInterface<T, ID> {

    // Create
    ID create(T model);

    // Read
    Optional<T> getById(ID id);
    List<T> getAll();

    // Update
    /**
     * Updates the model with the given id using the provided updated model.
     * Implementations typically ensure the id exists and apply the id to the updated model if needed.
     */
    T update(ID id, T updatedModel);

    // Delete
    boolean delete(ID id);
}
