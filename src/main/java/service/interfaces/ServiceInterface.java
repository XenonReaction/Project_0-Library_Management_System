package service.interfaces;

import java.util.List;
import java.util.Optional;

/**
 * Generic service-layer interface defining standard CRUD operations
 * for application models.
 *
 * <p>This interface represents the <strong>service layer contract</strong>
 * and sits between controllers and DAOs. Implementations are responsible
 * for enforcing business rules, coordinating validation, and delegating
 * persistence operations to the repository (DAO) layer.</p>
 *
 * <p><strong>Design intent:</strong>
 * <ul>
 *   <li>Controllers depend on services, not DAOs</li>
 *   <li>Services operate on <em>models</em>, not entities</li>
 *   <li>DAOs remain responsible only for persistence</li>
 * </ul>
 * </p>
 *
 * <p><strong>Type parameters:</strong>
 * <ul>
 *   <li>{@code T} — Service-layer model type (e.g., {@code Book}, {@code Member}, {@code Loan})</li>
 *   <li>{@code ID} — Identifier type (recommended: {@link Long} to match {@code BIGINT})</li>
 * </ul>
 *
 * <p>This interface intentionally mirrors common CRUD semantics while
 * leaving implementation details (validation, existence checks,
 * transaction boundaries) to concrete service classes.</p>
 *
 * @param <T>  the service-layer model type
 * @param <ID> the identifier type for the model
 */
public interface ServiceInterface<T, ID> {

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    /**
     * Creates a new model instance.
     *
     * <p>Implementations typically:</p>
     * <ul>
     *   <li>Validate the input model</li>
     *   <li>Convert the model to a persistence entity</li>
     *   <li>Delegate the insert to a DAO</li>
     *   <li>Return the generated identifier</li>
     * </ul>
     *
     * @param model the model to create
     * @return the generated identifier for the new model
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException if persistence fails
     */
    ID create(T model);

    // ---------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------

    /**
     * Retrieves a model by its identifier.
     *
     * <p>If no record exists with the given identifier, an empty
     * {@link Optional} is returned.</p>
     *
     * @param id the identifier to look up
     * @return an {@code Optional} containing the model if found
     */
    Optional<T> getById(ID id);

    /**
     * Retrieves all models of this type.
     *
     * <p>Implementations should return an empty list rather than {@code null}
     * when no records exist.</p>
     *
     * @return a list of all models
     */
    List<T> getAll();

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    /**
     * Updates an existing model identified by the given id.
     *
     * <p>Implementations typically:</p>
     * <ul>
     *   <li>Verify that the id exists</li>
     *   <li>Validate the updated model</li>
     *   <li>Apply the id to the updated model if required</li>
     *   <li>Persist changes via the DAO layer</li>
     * </ul>
     *
     * @param id           the identifier of the model to update
     * @param updatedModel the model containing updated values
     * @return the updated model after persistence
     * @throws IllegalArgumentException if validation fails or id does not exist
     * @throws RuntimeException if persistence fails
     */
    T update(ID id, T updatedModel);

    // ---------------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------------

    /**
     * Deletes the model identified by the given id.
     *
     * <p>The return value allows services to express business rules such as:</p>
     * <ul>
     *   <li>{@code false} if the record does not exist</li>
     *   <li>{@code false} if deletion is blocked by a business rule</li>
     *   <li>{@code true} if the deletion succeeded</li>
     * </ul>
     *
     * @param id the identifier of the model to delete
     * @return {@code true} if the model was deleted, {@code false} otherwise
     * @throws RuntimeException if an unexpected persistence error occurs
     */
    boolean delete(ID id);
}
