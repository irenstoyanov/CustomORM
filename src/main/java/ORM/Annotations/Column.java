package ORM.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the annotated field in the class
 * as a column in the database.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    /**
     * (Optional) The name of the database column.
     * Defaults to the field name.
     */
    String name() default "";

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

    /**
     * (Optional) The length of the database column.
     * Applies only when the given column is string.
     * 255 by default.
     */
    int length() default 255;
}
