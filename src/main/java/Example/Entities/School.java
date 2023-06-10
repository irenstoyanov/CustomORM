package Example.Entities;

import ORM.Annotations.*;

@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue
    private int id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String town;

    public School() {
    }

    public School(String name, String town) {
        this.name = name;
        this.town = town;
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

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    @Override
    public String toString() {
        return "School{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", town='" + town + '\'' +
                '}';
    }
}
