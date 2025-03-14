package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.Role;

public class Event extends Node implements IEvent {

    //The role the Event belongs to
    private Role role;

    public Event(String name, String id) {
        super(name);
        this.setId(id);
    }

    public Event(String name) {
        super(name);
    }


    public Event clone() {
        return new Event(this.getName());
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role pRole) {
        role = pRole;
    }
}
