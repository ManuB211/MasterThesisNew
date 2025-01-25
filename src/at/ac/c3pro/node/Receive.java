package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.Role;

public class Receive extends InteractionActivity {


    public Receive(Role sender, Message message, String name) {
        super(sender, message, name);
    }

    public Receive(Role sender, Message message, String name, String id) {
        super(sender, message, name);
        this.setId(id);
    }

    //target represents the role of the model containing the current receive
    public Send Complement(Role target) {
        return (new Send(target, this.getMessage(), this.getName().replaceAll("receive", "send")));
    }

    public Receive clone() {
        return new Receive(this.role, this.getMessage(), this.getName());
    }
}
