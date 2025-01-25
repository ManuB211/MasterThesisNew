package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.Role;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

//TODO: Refactor to work with participants
public abstract class InteractionActivity extends Node implements IPublicNode, IPrivateNode {

    private Message message = null;
    public Role role = null;

    public InteractionActivity(Role role, Message message, String name) {
        super(name);
        this.role = role;
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public InteractionActivity Complement(Role role) {

        if (this instanceof Receive)
            return (new Send(role, this.message, this.getName().replaceAll("receive", "send")));
        else
            return (new Receive(role, this.message, this.getName().replaceAll("receive", "send")));

    }

    public boolean hasRole(Role role) {
        return this.role.equals(role);
    }

    public Set<Role> getRoles() {
        return new HashSet<Role>(Arrays.asList(this.role));
    }

    public InteractionActivity clone() {
        if (this instanceof Send)
            return new Send(this.role, this.message, this.getName());
        return new Receive(this.role, this.message, this.getName());
    }

    /**
     * Returns the name but crops out the information for sender, receiver or
     * participants This is done, as the graph building algorithms need the same
     * name for a collaboration between two participants
     *
     * @return Name string
     */
    public String getNameWithoutSenderReceiverInfo() {
        return super.getName().split("\\(")[0];
    }

}
