package ORM;

import ORM.Annotations.*;
import ORM.Exceptions.AnnotationException;
import ORM.Exceptions.EntityManagerException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityManagerImpl implements EntityManager {

    private final Connection connection;
    private boolean showSql;

    public EntityManagerImpl(Connection connection) {
        this.connection = connection;
        this.showSql = false;
    }

    @Override
    public boolean createQuery(String SQL) {
        try {
            PreparedStatement SQLQuery = connection.prepareStatement(SQL);
            return SQLQuery.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    @Override
    public <T> List<T> createQuery(String SQL, Class<T> resultClass) {
        try {
            PreparedStatement SQLQuery = connection.prepareStatement(SQL);
            ResultSet resultSet = SQLQuery.executeQuery();

            Constructor<T> constructor = resultClass.getDeclaredConstructor();
            constructor.setAccessible(true);

            List<T> objects = new ArrayList<>();
            while (resultSet.next()) {
                T object = constructor.newInstance();

                for (Field field : resultClass.getDeclaredFields()) {
                    field.setAccessible(true);

                    if (!field.isAnnotationPresent(Id.class) &&
                            !field.isAnnotationPresent(Column.class) &&
                            !field.isAnnotationPresent(ForeignKey.class)) {
                        continue;
                    }

                    Object value = getFieldValue(resultSet, field);
                    setValueToField(object, field, value);
                }

                objects.add(object);
            }

            return objects;
        } catch (Exception e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    @Override
    public <T> void createTable(Class<T> table) {
        checkIfClassIsValidEntity(table);

        String tableName = getTableName(table);
        String columnsInitialization = getColumnsInitialization(table);

        String query = String.format("create table `%s` (%n%s%n)", tableName, columnsInitialization);

        try {
            PreparedStatement createTableQuery = connection.prepareStatement(query);
            createTableQuery.execute();

            if (this.showSql) {
                System.out.println("SQL Query:");
                System.out.printf(query + "%n%n");
            }
        } catch (SQLException e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    @Override
    public <T> void dropTable(Class<T> table) {
        String tableName = getTableName(table);

        String query = String.format("drop table `%s`", tableName);

        try {
            PreparedStatement dropTableQuery = connection.prepareStatement(query);
            dropTableQuery.execute();

            if (this.showSql) {
                System.out.println("SQL Query:");
                System.out.printf(query + "%n%n");
            }
        } catch (SQLException e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    @Override
    public boolean persist(Object entity) {
        Class<?> entityClass = entity.getClass();

        checkIfClassIsValidEntity(entityClass);
        checkIfClassHasExistingDbTable(entityClass);

        Field primaryKey = getPrimaryKeyField(entityClass);
        primaryKey.setAccessible(true);
        Object primaryKeyValue;

        try {
            primaryKeyValue = primaryKey.get(entity);
        } catch (IllegalAccessException e) {
            throw new EntityManagerException(e.getMessage());
        }

        String query = "";

        String primaryKeyString = String.valueOf(primaryKeyValue);
        if (primaryKey.isAnnotationPresent(GeneratedValue.class)) {
            if (Long.parseLong(primaryKeyString) == 0) {
                query = insert(entity, entityClass, primaryKey);
            } else {
                query = update(entity, entityClass, primaryKey, primaryKeyValue);
            }
        } else {
            boolean originalValue = showSql;
            showSql = false;
            if (find(entityClass, Long.parseLong(primaryKeyString)) == null) {
                query = insert(entity, entityClass, primaryKey);
            } else {
                query = update(entity, entityClass, primaryKey, primaryKeyValue);
            }
            showSql = originalValue;
        }

        try {
            PreparedStatement persistQuery = connection.prepareStatement(query);
            int rowsAffected = persistQuery.executeUpdate();

            if (this.showSql) {
                System.out.println("SQL Query:");
                System.out.printf(query + "%n%n");
            }

            return rowsAffected == 1;
        } catch (SQLException e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    @Override
    public void remove(Object entity) {
        Class<?> entityClass = entity.getClass();

        checkIfClassIsValidEntity(entityClass);
        checkIfClassHasExistingDbTable(entityClass);

        Field primaryKey = getPrimaryKeyField(entityClass);
        primaryKey.setAccessible(true);
        Object primaryKeyValue;

        try {
            primaryKeyValue = primaryKey.get(entity);
        } catch (IllegalAccessException e) {
            throw new EntityManagerException(e.getMessage());
        }

        String tableName = getTableName(entityClass);

        boolean isValid = checkIfEntityExistsInDatabase(entity, entityClass, primaryKeyValue);
        if (!isValid) {
            String message = String.format("No such entity found in database \"%s\"", tableName);
            throw new EntityManagerException(message);
        }

        String query = String.format("delete from `%s` where `%s` = '%s'",
                tableName, primaryKey.getName(), primaryKeyValue);

        try {
            PreparedStatement removeQuery = connection.prepareStatement(query);
            removeQuery.execute();

            if (this.showSql) {
                System.out.println("SQL Query:");
                System.out.printf(query + "%n%n");
            }
        } catch (SQLException e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, long primaryKey) {
        String tableName = getTableName(entityClass);
        String allColumnNames = getAllColumnNames(entityClass, true);
        Field primaryKeyField = getPrimaryKeyField(entityClass);

        String query = String.format("select %s from `%s` where `%s` = '%s'",
                allColumnNames, tableName, primaryKeyField.getName(), primaryKey);

        try {
            PreparedStatement findObjectQuery = connection.prepareStatement(query);
            ResultSet resultSet = findObjectQuery.executeQuery();

            if (this.showSql) {
                System.out.println("SQL Query:");
                System.out.printf(query + "%n%n");
            }

            if (!resultSet.next()) {
                return null;
            }

            Constructor<T> constructor = entityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T object = constructor.newInstance();

            for (Field field : entityClass.getDeclaredFields()) {
                field.setAccessible(true);

                if (!field.isAnnotationPresent(Id.class) &&
                        !field.isAnnotationPresent(Column.class) &&
                        !field.isAnnotationPresent(ForeignKey.class)) {
                    continue;
                }

                Object value = getFieldValue(resultSet, field);
                setValueToField(object, field, value);
            }

            return object;
        } catch (Exception e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass) {
        String tableName = getTableName(entityClass);
        String allColumnNames = getAllColumnNames(entityClass, true);

        String query = String.format("select %s from `%s`", allColumnNames, tableName);

        try {
            PreparedStatement findAllQuery = connection.prepareStatement(query);
            ResultSet resultSet = findAllQuery.executeQuery();

            if (this.showSql) {
                System.out.println("SQL Query:");
                System.out.printf(query + "%n%n");
            }

            List<T> objects = new ArrayList<>();

            boolean originalValue = showSql;
            showSql = false;
            while (resultSet.next()) {
                Field primaryKeyField = getPrimaryKeyField(entityClass);
                Object primaryKey = resultSet.getObject(primaryKeyField.getName());

                T object = find(entityClass, Long.parseLong(String.valueOf(primaryKey)));
                objects.add(object);
            }
            showSql = originalValue;

            return objects;
        } catch (Exception e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    @Override
    public void showSql(boolean value) {
        this.showSql = value;
    }

    private void checkIfClassIsValidEntity(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            String exceptionMessage = String.format
                    ("Entity \"%s\" is not annotated as a database entity.", clazz.getName());
            throw new AnnotationException(exceptionMessage);
        }

        if (!clazz.isAnnotationPresent(Table.class)) {
            String exceptionMessage = String.format
                    ("Entity \"%s\" is not annotated as a database table.", clazz.getName());
            throw new AnnotationException(exceptionMessage);
        }

        List<Field> primaryKeyList = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .collect(Collectors.toList());

        if (primaryKeyList.isEmpty()) {
            String message = String.format("Entity \"%s\" does not have a specified primary key.", clazz.getName());
            throw new AnnotationException(message);
        }

        if (primaryKeyList.size() > 1) {
            String message = String.format("Entity \"%s\" has more than one primary key.", clazz.getName());
            throw new AnnotationException(message);
        }

        Class<?> primaryKey = primaryKeyList.get(0).getType();
        if (primaryKey != int.class && primaryKey != Integer.class && primaryKey != long.class && primaryKey != Long.class) {
            String message = String.format("Entity \"%s\" has a primary key with type other that int or long.", clazz.getName());
            throw new EntityManagerException(message);
        }
    }

    private void checkIfClassHasExistingDbTable(Class<?> table) {
        String query = String.format("select table_name from information_schema.tables where table_name = '%s'",
                getTableName(table));

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                String message = String.format("Entity \"%s\" does not have an existing database table yet.", table.getName());
                throw new EntityManagerException(message);
            }
        } catch (SQLException e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    private Object getFieldValue(ResultSet resultSet, Field field) throws SQLException {
        Object value = null;

        if (field.isAnnotationPresent(Id.class)) {
            value = resultSet.getObject(getColumnName(field, Id.class));
        } else if (field.isAnnotationPresent(Column.class)) {
            value = resultSet.getObject(getColumnName(field, Column.class));
        } else if (field.isAnnotationPresent(ForeignKey.class)) {
            Object foreignKeyField = resultSet.getObject(getColumnName(field, ForeignKey.class));
            long fkFieldPrimaryKey = Long.parseLong(String.valueOf(foreignKeyField));

            boolean originalValue = showSql;
            showSql = false;
            value = find(field.getType(), fkFieldPrimaryKey);
            showSql = originalValue;
        }

        return value;
    }

    private void setValueToField(Object object, Field field, Object value) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        if (fieldType == LocalDate.class) {
            LocalDate date = LocalDate.parse(String.valueOf(value));
            field.set(object, date);
        } else if (fieldType == LocalDateTime.class) {
            LocalDateTime dateTime = LocalDateTime.parse(String.valueOf(value));
            field.set(object, dateTime);
        } else {
            field.set(object, value);
        }
    }

    private <T> String getTableName(Class<T> table) {
        Table tableAnnotation = table.getAnnotation(Table.class);
        String tableName = tableAnnotation.name();

        return tableName.trim().isEmpty() ? table.getSimpleName() : tableName;
    }

    private <T> String getColumnsInitialization(Class<T> table) {
        List<String> columns = new ArrayList<>();
        List<Field> foreignKeyFields = new ArrayList<>();

        for (Field field : table.getDeclaredFields()) {
            field.setAccessible(true);

            String columnName;
            String columnType;
            String columnConstraints;

            if (field.isAnnotationPresent(Id.class)) {
                columnName = getColumnName(field, Id.class);
                columnType = getColumnSqlType(field, null);
                columnConstraints = "primary key";

                if (field.isAnnotationPresent(GeneratedValue.class)) {
                    columnConstraints += " auto_increment";
                }
            } else if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);

                columnName = getColumnName(field, Column.class);
                columnType = getColumnSqlType(field, column);
                columnConstraints = getColumnConstraints(column);
            } else if (field.isAnnotationPresent(ForeignKey.class)) {
                Class<?> fieldClass = field.getType();

                checkIfClassIsValidEntity(fieldClass);
                checkIfClassHasExistingDbTable(fieldClass);

                Field primaryKeyField = getPrimaryKeyField(fieldClass);
                primaryKeyField.setAccessible(true);

                ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);

                columnName = getColumnName(field, ForeignKey.class);
                columnType = getColumnSqlType(primaryKeyField, null);
                columnConstraints = getColumnConstraints(foreignKey);

                foreignKeyFields.add(field);
            } else {
                continue;
            }

            String columnInitialization = String.format("`%s` %s %s",
                    columnName, columnType, columnConstraints);
            columns.add(columnInitialization);
        }

        addForeignKeyConstraints(columns, foreignKeyFields, table);

        return String.join(String.format(",%n"), columns);
    }

    private String getColumnName(Field field, Class<?> annotationType) {
        String columnName = "";

        if (annotationType == Column.class) {
            columnName = field.getAnnotation(Column.class).name();
        } else if (annotationType == Id.class) {
            columnName = field.getAnnotation(Id.class).name();
        } else if (annotationType == ForeignKey.class) {
            columnName = field.getAnnotation(ForeignKey.class).name();
        }

        return columnName.trim().isEmpty() ? field.getName() : columnName;
    }

    private String getColumnSqlType(Field field, Column columnAnnotation) {
        Class<?> fieldType = field.getType();
        String sqlType = "";

        if (fieldType == String.class) {
            int length = columnAnnotation.length();
            sqlType = String.format("varchar(%d)", length);
        } else if (fieldType == char.class || fieldType == Character.class) {
            sqlType = "char(1)";
        } else if (fieldType == int.class || fieldType == Integer.class) {
            sqlType = "int";
        } else if (fieldType == long.class || fieldType == Long.class) {
            sqlType = "bigint";
        } else if (fieldType == double.class || fieldType == Double.class) {
            sqlType = "double";
        } else if (fieldType == float.class || fieldType == Float.class) {
            sqlType = "float";
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            sqlType = "bit(1)";
        } else if (fieldType == LocalDate.class) {
            sqlType = "date";
        } else if (fieldType == LocalDateTime.class) {
            sqlType = "datetime";
        }

        return sqlType;
    }

    private String getColumnConstraints(Column columnAnnotation) {
        String nullable = "null";

        if (!columnAnnotation.nullable()) {
            nullable = "not null";
        }

        if (columnAnnotation.unique()) {
            String unique = "unique";
            return nullable + " " + unique;
        }

        return nullable;
    }

    private String getColumnConstraints(ForeignKey foreignKeyAnnotation) {
        String nullable = "null";

        if (!foreignKeyAnnotation.nullable()) {
            nullable = "not null";
        }

        if (foreignKeyAnnotation.unique()) {
            String unique = "unique";
            return nullable + " " + unique;
        }

        return nullable;
    }

    private Field getPrimaryKeyField(Class<?> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .collect(Collectors.toList())
                .get(0);
    }

    private <T> void addForeignKeyConstraints(List<String> columns, List<Field> foreignKeyFields, Class<T> table) {
        String constraint = "constraint `%s`%n" + " foreign key (`%s`)%n" + " references `%s`(`%s`)";

        for (Field field : foreignKeyFields) {
            Class<?> fieldClass = field.getType();

            String referencedColumn = getColumnName(getPrimaryKeyField(fieldClass), Id.class);
            String referencedTable = getTableName(fieldClass);
            String columnName = getColumnName(field, ForeignKey.class);
            String foreignKeyName = String.format("fk_%s_%s",
                    getTableName(table), referencedTable);

            columns.add(String.format(constraint, foreignKeyName, columnName, referencedTable, referencedColumn));
        }
    }

    private String insert(Object entity, Class<?> entityClass, Field primaryKey) {
        boolean includePrimaryKey = !primaryKey.isAnnotationPresent(GeneratedValue.class);

        String tableName = getTableName(entityClass);
        String columnNames = getAllColumnNames(entityClass, includePrimaryKey);
        String values = getInsertValues(entity, entityClass, includePrimaryKey);

        return String.format("insert into `%s` (%s)%n" + "values (%s)",
                tableName, columnNames, values);
    }

    private <T> String getAllColumnNames(Class<T> table, boolean includePrimaryKey) {
        String tableName = getTableName(table);

        String query = String.format("show columns from `%s`", tableName);

        if (!includePrimaryKey) {
            query += " where not `Key` = 'PRI'";
        }

        try {
            PreparedStatement getColumnNamesQuery = connection.prepareStatement(query);
            ResultSet resultSet = getColumnNamesQuery.executeQuery();

            List<String> columnNames = new ArrayList<>();

            while (resultSet.next()) {
                String columnName = resultSet.getString("Field");
                columnName = String.format("`%s`", columnName);
                columnNames.add(columnName);
            }

            return String.join(", ", columnNames);
        } catch (SQLException e) {
            throw new EntityManagerException(e.getMessage());
        }
    }

    private String getInsertValues(Object entity, Class<?> entityClass, boolean includePrimaryKey) {
        List<Field> fields = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class)
                        || field.isAnnotationPresent(Column.class)
                        || field.isAnnotationPresent(ForeignKey.class))
                .collect(Collectors.toList());

        if (!includePrimaryKey) {
            fields = fields.stream()
                    .filter(field -> !field.isAnnotationPresent(Id.class))
                    .collect(Collectors.toList());
        }

        List<String> values = new ArrayList<>();

        for (Field field : fields) {
            field.setAccessible(true);

            try {
                Object fieldValue = field.get(entity);
                if (field.isAnnotationPresent(ForeignKey.class)) {
                    Class<?> fieldClass = field.getType();

                    checkIfClassIsValidEntity(fieldClass);
                    checkIfClassHasExistingDbTable(fieldClass);

                    Field fkFieldPrimaryKey = getPrimaryKeyField(fieldClass);
                    fkFieldPrimaryKey.setAccessible(true);

                    fieldValue = fkFieldPrimaryKey.get(fieldValue);
                }
                String value = String.format("'%s'", fieldValue);
                values.add(value);
            } catch (IllegalAccessException e) {
                throw new EntityManagerException(e.getMessage());
            }
        }

        return String.join(", ", values);
    }

    private String update(Object entity, Class<?> entityClass, Field primaryKey, Object primaryKeyValue) {
        String tableName = getTableName(entityClass);
        String columnsAndNewValues = getUpdateColumnsAndNewValues(entity, entityClass);

        return String.format("update `%s`%n" + "set %s%n" + "where `%s` = '%s'",
                tableName, columnsAndNewValues, primaryKey.getName(), primaryKeyValue);
    }

    private String getUpdateColumnsAndNewValues(Object entity, Class<?> entityClass) {
        List<Field> fields = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> !field.isAnnotationPresent(Id.class)
                        && (field.isAnnotationPresent(Column.class) ||
                            field.isAnnotationPresent(ForeignKey.class)))
                .collect(Collectors.toList());

        fields.forEach(field -> field.setAccessible(true));

        List<String> result = new ArrayList<>();

        for (Field field : fields) {
            try {
                String columnName;
                Object newValue = field.get(entity);

                if (field.isAnnotationPresent(Id.class)) {
                    columnName = getColumnName(field, Id.class);
                } else if (field.isAnnotationPresent(Column.class)) {
                    columnName = getColumnName(field, Column.class);
                } else if (field.isAnnotationPresent(ForeignKey.class)) {
                    columnName = getColumnName(field, ForeignKey.class);
                    Field fkFieldPrimaryKey = getPrimaryKeyField(newValue.getClass());
                    fkFieldPrimaryKey.setAccessible(true);

                    newValue = fkFieldPrimaryKey.get(newValue);
                } else {
                    continue;
                }

                String columnAndValue = String.format("`%s` = '%s'", columnName, newValue);
                result.add(columnAndValue);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        return String.join(", ", result);
    }

    private boolean checkIfEntityExistsInDatabase(Object entity, Class<?> entityClass, Object primaryKeyValue) {
        boolean originalValue = showSql;
        showSql = false;
        Object dbObject = find(entityClass, Long.parseLong(String.valueOf(primaryKeyValue)));
        showSql = originalValue;

        if (dbObject == null) {
            return false;
        }

        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);

            if (!field.isAnnotationPresent(Id.class) &&
                    !field.isAnnotationPresent(Column.class) &&
                    !field.isAnnotationPresent(ForeignKey.class)) {
                continue;
            }

            try {
                String entityField = String.valueOf(field.get(entity));
                String dbObjectField = String.valueOf(field.get(dbObject));

                if (!entityField.equals(dbObjectField)) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                throw new EntityManagerException(e.getMessage());
            }
        }

        return true;
    }
}
