package com.cms.core.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface providing CRUD operations for entities in the
 * JavaCMS system.
 *
 * <p>
 * This interface defines the standard repository pattern contract using Java
 * Generics
 * to provide type-safe data access operations. It serves as the foundation for
 * all repository
 * implementations in the content management system, ensuring consistent data
 * access patterns
 * across different entity types.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Repository Pattern - Encapsulates data
 * access logic and
 * provides a more object-oriented view of the persistence layer. This pattern
 * supports loose
 * coupling between the business logic and data access layers.
 * </p>
 *
 * <p>
 * <strong>Generics Implementation:</strong> Provides comprehensive Generics
 * usage
 * with parameterized types for entities (T) and identifiers (ID), providing
 * compile-time type
 * safety and eliminating the need for casting operations.
 * </p>
 *
 * <p>
 * <strong>Collections Framework:</strong> All methods return or work with
 * Collections Framework
 * types (List, Optional) providing proper integration with Java's collection
 * system.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Provides both Generics and Collections Framework
 * implementation through type-safe repository operations with comprehensive
 * collection usage.
 * </p>
 *
 * @param <T>  The entity type managed by this repository
 * @param <ID> The type of the entity's identifier
 * @see com.cms.core.repository.ContentRepository
 * @see com.cms.core.repository.UserRepository
 * @since 1.0
 * @author Otman Hmich S007924
 */
public interface Repository<T, ID> {

    /**
     * Saves an entity to the repository.
     *
     * <p>
     * <strong>Generics:</strong> Uses parameterized type T to ensure type safety
     * for entity operations without requiring casting.
     * </p>
     *
     * @param entity The entity to save, must not be null
     * @return The saved entity, potentially with updated metadata
     * @throws RepositoryException      if the save operation fails
     * @throws IllegalArgumentException if entity is null
     */
    T save(T entity) throws RepositoryException;

    /**
     * Retrieves an entity by its unique identifier.
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns Optional to handle the
     * possibility
     * of entities not being found, providing a null-safe approach to entity
     * retrieval.
     * </p>
     *
     * <p>
     * <strong>Generics:</strong> Both the ID parameter and return type are
     * parameterized
     * for compile-time type safety.
     * </p>
     *
     * @param id The unique identifier of the entity
     * @return An Optional containing the entity if found, empty Optional otherwise
     * @throws RepositoryException      if the retrieval operation fails
     * @throws IllegalArgumentException if id is null
     */
    Optional<T> findById(ID id) throws RepositoryException;

    /**
     * Retrieves all entities from the repository.
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns a List&lt;T&gt; containing
     * all entities,
     * providing proper use of parameterized collections for type-safe iteration.
     * </p>
     *
     * @return A List containing all entities, never null but may be empty
     * @throws RepositoryException if the retrieval operation fails
     */
    List<T> findAll() throws RepositoryException;

    /**
     * Retrieves entities with pagination support.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses List for paginated results and
     * provides advanced collection operations for data management.
     * </p>
     *
     * @param page The page number (0-based)
     * @param size The maximum number of entities per page
     * @return A List containing entities for the specified page
     * @throws RepositoryException      if the retrieval operation fails
     * @throws IllegalArgumentException if page or size parameters are invalid
     */
    List<T> findAll(int page, int size) throws RepositoryException;

    /**
     * Retrieves entities by multiple identifiers in a single operation.
     *
     * <p>
     * <strong>Collections Framework:</strong> Accepts a Collection of IDs and
     * returns
     * a List of entities, providing bulk operations with parameterized collections.
     * </p>
     *
     * <p>
     * <strong>Generics:</strong> Uses bounded wildcards for flexible ID collection
     * input
     * while maintaining type safety.
     * </p>
     *
     * @param ids Collection of identifiers to retrieve
     * @return A List of found entities (may be fewer than requested IDs)
     * @throws RepositoryException      if the retrieval operation fails
     * @throws IllegalArgumentException if ids collection is null
     */
    List<T> findAllById(Iterable<? extends ID> ids) throws RepositoryException;

    /**
     * Checks if an entity with the specified identifier exists.
     *
     * @param id The identifier to check
     * @return true if an entity exists with the given identifier, false otherwise
     * @throws RepositoryException      if the existence check fails
     * @throws IllegalArgumentException if id is null
     */
    boolean existsById(ID id) throws RepositoryException;

    /**
     * Returns the total count of entities in the repository.
     *
     * @return The total number of entities
     * @throws RepositoryException if the count operation fails
     */
    long count() throws RepositoryException;

    /**
     * Deletes an entity by its identifier.
     *
     * @param id The identifier of the entity to delete
     * @throws RepositoryException      if the delete operation fails
     * @throws IllegalArgumentException if id is null
     */
    void deleteById(ID id) throws RepositoryException;

    /**
     * Deletes the specified entity.
     *
     * <p>
     * <strong>Generics:</strong> Uses parameterized type to ensure type safety
     * during delete operations.
     * </p>
     *
     * @param entity The entity to delete, must not be null
     * @throws RepositoryException      if the delete operation fails
     * @throws IllegalArgumentException if entity is null
     */
    void delete(T entity) throws RepositoryException;

    /**
     * Deletes multiple entities in a single operation.
     *
     * <p>
     * <strong>Collections Framework:</strong> Accepts any Iterable of entities,
     * providing flexibility for different collection types while maintaining type
     * safety.
     * </p>
     *
     * @param entities The entities to delete
     * @throws RepositoryException      if the delete operation fails
     * @throws IllegalArgumentException if entities is null
     */
    void deleteAll(Iterable<? extends T> entities) throws RepositoryException;

    /**
     * Deletes all entities from the repository.
     *
     * <p>
     * <strong>Warning:</strong> This operation removes all data and should be used
     * with caution.
     * </p>
     *
     * @throws RepositoryException if the delete operation fails
     */
    void deleteAll() throws RepositoryException;

    /**
     * Saves multiple entities in a single batch operation.
     *
     * <p>
     * <strong>Collections Framework:</strong> Accepts and returns Iterable
     * collections
     * for bulk operations, improving performance for multiple entity operations.
     * </p>
     *
     * <p>
     * <strong>Generics:</strong> Uses bounded wildcards and parameterized return
     * types
     * for maximum flexibility while maintaining compile-time type safety.
     * </p>
     *
     * @param entities The entities to save
     * @return An Iterable of saved entities
     * @throws RepositoryException      if the batch save operation fails
     * @throws IllegalArgumentException if entities is null
     */
    Iterable<T> saveAll(Iterable<? extends T> entities) throws RepositoryException;

    /**
     * Performs a batch update operation on entities matching the specified
     * criteria.
     *
     * <p>
     * This method provides efficient bulk updates without requiring individual
     * entity retrieval and save operations.
     * </p>
     *
     * @param updateCriteria The criteria for selecting entities to update
     * @param updateData     The data to apply to matching entities
     * @return The number of entities updated
     * @throws RepositoryException      if the batch update operation fails
     * @throws IllegalArgumentException if parameters are null
     */
    long batchUpdate(java.util.Map<String, Object> updateCriteria, java.util.Map<String, Object> updateData)
            throws RepositoryException;

    /**
     * Flushes any pending changes to the underlying storage.
     *
     * <p>
     * This method ensures that all pending repository operations are committed
     * to the persistent storage layer.
     * </p>
     *
     * @throws RepositoryException if the flush operation fails
     */
    void flush() throws RepositoryException;
}
