package ORM;

import ORM.Annotations.Entity;
import ORM.Annotations.Table;
import ORM.Annotations.Id;
import ORM.Annotations.GeneratedValue;
import ORM.Annotations.Column;
import ORM.Annotations.ForeignKey;

import java.util.List;

/**
 * The EntityManager interface is used to interact with a MySQL database.
 *
 * With an EntityManager instance you can perform various types of
 * MySQL operations, like using the built-in functions to create and drop tables,
 * to either insert, update or delete data, or you can create your own queries
 * if you would like to, and much more.
 *
 * Using the built-in annotations (for example @Entity, @Column, etc.)
 * you can easily map Java classes and objects to
 * MySQL tables and entities and vice versa.
 *
 * Every class that is annotated with these annotations should
 * have an empty constructor for the EntityManager to work properly.
 *
 * @see Entity
 * @see Table
 * @see Id
 * @see GeneratedValue
 * @see Column
 * @see ForeignKey
 */
public interface EntityManager {

    /**
     * Used for creating queries that do not return any type of results.
     * (for example UPDATE queries).
     *
     * @param SQL The SQL query
     * @return True if the query was executed without any errors;
     * False if the query was not executed properly or if
     * any errors occurred during execution
     */
    boolean createQuery(String SQL);

    /**
     * Used for creating queries that return any type of results
     * (for example SELECT queries).
     * If there are any results returned from the query,
     * the method returns a List with either one or more
     * instances of the class resultClass. But if the query
     * does not return any results, the method returns
     * an empty List.
     *
     * @param SQL The SQL query
     * @param resultClass The class type of the query result
     * @return A list of the instances of the resultClass
     */
    <T> List<T> createQuery(String SQL, Class<T> resultClass);

    /**
     * Used for creating a new table in the database.
     *
     * @param table The class that is going to act as a template for the new table
     */
    <T> void createTable(Class<T> table);

    /**
     * Used for dropping an existing table in the database.
     *
     * @param table The class whose corresponding table should be deleted
     */
    <T> void dropTable(Class<T> table);

    /**
     * Used to either insert the new data in the entity's corresponding database table,
     * or if the entity already exists there, it just updates it.
     *
     * How it works:
     *
     * When the id of the class is annotated with the Id and the GeneratedValue annotations
     * - If the current object doesn't have a value for its id, then
     *   the object is inserted into the table with an auto generated id;
     *   If its id has a value, then it checks if an entity with such id exists
     *   in the database table, and if it does, it updates it with the current object's fields.
     *
     * When the id of the class is annotated only with the Id annotation
     * - Checks if an entity with the current object's id exists in the database
     *   table, and if it does, it updates it with the current object's
     *   fields, and if it does not, it inserts it.
     *
     *
     * @param entity Current persist entity
     * @return True if the insert/update query was executed properly;
     * False if the query was not executed properly or if
     * any errors occurred during execution
     */
    boolean persist(Object entity);

    /**
     * Used for deleting an entity from the database.
     * If such entity does not exist in the database
     * the method throws an exception.
     *
     * @param entity Entity to be deleted
     */
    void remove(Object entity);

    /**
     * Used to retrieve an entity from the current database table.
     * Finding it by the primary key, searching in the
     * corresponding database table to the entityClass class.
     *
     * @param entityClass The class from whose corresponding table the entity will be retrieved
     * @param primaryKey The id of the entity that is searched for
     * @return An instance of the entityClass class if such entity exists in the database table;
     * Null if such entity does not exist in the database
     */
    <T> T find(Class<T> entityClass, long primaryKey);

    /**
     * Used to retrieve all entities from the database table,
     * corresponding to the entityClass class.
     *
     * @param entityClass The class from whose corresponding table the entities will be retrieved
     * @return A list consisting of one or more entities if there is data inside the table;
     * An empty list if the table is empty.
     */
    <T> List<T> findAll(Class<T> entityClass);

    /**
     * If the SQL of the performed operations by the user
     * should be shown in the console or not.
     *
     * @param value if true, the SQL will be show, and if false - it will be hidden.
     */
    void showSql(boolean value);

}
