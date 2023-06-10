package Example.Entities;

import ORM.Annotations.*;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    private int id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "course_topic")
    private String courseTopic;

    @Column(nullable = false)
    private double price;

    @ForeignKey(name = "school_id", referencedColumnName = "id", nullable = false)
    private School school;

    @ForeignKey(name = "teacher_id", referencedColumnName = "id", nullable = false)
    private Teacher teacher;

    public Course() {
    }

    public Course(int id, String name, double price, School school, Teacher teacher) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.school = school;
        this.teacher = teacher;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourseTopic() {
        return courseTopic;
    }

    public void setCourseTopic(String courseTopic) {
        this.courseTopic = courseTopic;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", courseTopic='" + courseTopic + '\'' +
                ", price=" + price +
                ", school=" + school +
                ", teacher=" + teacher +
                '}';
    }
}
