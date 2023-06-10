package ORM.Exceptions;

/**
 * Thrown whenever an entity is not annotated properly
 * and is causing the EntityManager to not function well.
 */
public class AnnotationException extends RuntimeException {
    public AnnotationException(String message) {
        super(message);
    }
}
