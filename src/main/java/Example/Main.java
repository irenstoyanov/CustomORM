package Example;

import Example.Entities.Course;
import Example.Entities.School;
import Example.Entities.Teacher;
import ORM.Connector;
import ORM.EntityManager;
import ORM.EntityManagerImpl;

import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        Connector.createConnection("root", "1234", "school_system");
        Connection connection = Connector.getConnection();

        EntityManager entityManager = new EntityManagerImpl(connection);
        entityManager.showSql(true);

        // Creating the tables
        entityManager.createTable(School.class);
        entityManager.createTable(Teacher.class);
        entityManager.createTable(Course.class);

        // Inserting data
        School school = new School("Coding School", "New York City");
        entityManager.persist(school);

        school.setId(1);

        Teacher teacher = new Teacher("Georgi", "Georgiev", 32, school);
        entityManager.persist(teacher);

        teacher.setId(1);

        Course course = new Course(524, "Java Programming", 499.99, school, teacher);
        entityManager.persist(course);

        // Updating data
        course.setCourseTopic("Learning and understanding basic and advanced concepts of the programming language Java");
        entityManager.persist(course);

        // Finding data
        Course courseFind = entityManager.find(Course.class, 524);
    }
}
