package at.ac.c3pro.chormodel;

public class Role implements IRole {
    public String name;
    public String id;
    public double weight = 1;

    public Role(String name) {
        this.name = name;
    }

    public Role(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public void setweight(double weight) {
        this.weight = weight;
    }

    public String toString() {
        return this.name;
    }

    public boolean equals(Role role) {
        return this.name.equals(role.name);
    }

    public String getName() {
        return this.name;
    }

}
