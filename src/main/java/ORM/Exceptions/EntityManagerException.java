package ORM.Exceptions;

/**
 * Thrown whenever something went wrong when
 * performing operations with the EntityManager
 */
public class EntityManagerException extends RuntimeException {
    public EntityManagerException(String message) {
        super(message);
    }
}
