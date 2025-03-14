package at.ac.c3pro.util;

public class Pair<obj1 extends Object, obj2 extends Object> {
    public obj1 first;
    public obj2 second;

    public Pair(obj1 o1, obj2 o2) {
        this.first = o1;
        this.second = o2;
    }

    public boolean equals(Pair<obj1, obj2> p) {
        return ((p.first == this.first) && (p.second == this.second));
    }

}