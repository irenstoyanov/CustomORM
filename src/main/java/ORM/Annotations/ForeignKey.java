package ORM.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the annotated field in the class
 * as a foreign key column in the database.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ForeignKey {

    /**
     * The name of the database foreign key column.
     */
    String name();

    /**
     * The name of the database referenced column.
     */
    String referencedColumnName();

    /**
     * (Optional) Whether the database column
     * can be null or not. True by default.
     */
    boolean nullable() default true;

    /**
     * (Optional) Whether the database column
     * should be set as unique or not.
     * False by default.
     */
    boolean unique() default false;
}
