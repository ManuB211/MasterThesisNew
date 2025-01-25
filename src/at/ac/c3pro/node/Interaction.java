package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.Role;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Interaction extends ChoreographyNode {

    public enum InteractionType {
        MESSAGE_EXCHANGE, HANDOVER_OF_WORK, SHARED_RESOURCE, SYNCHRONOUS_ACTIVITY
    }

    // In the case of Message or Handover of Work Participant1 will be the sender
    // and 2 the receiver
    // In the case of Synchronous Task or Ressource Sharing, there is no sender or
    // receiver
    private Role participant1 = null;
    private Role participant2 = null;
    private Message message = null;
    private InteractionType interactionType;

    public Interaction() {
        super();
    }

    public Interaction(String name, Role participant1, Role participant2, Message message) {
        super(name);
        this.setParticipant1(participant1);
        this.setParticipant2(participant2);
        this.setMessage(message);
    }

    public Interaction(String name, String id, Role participant1, Role participant2, Message message) {
        super(name);
        this.setParticipant1(participant1);
        this.setParticipant2(participant2);
        this.setMessage(message);
        this.setId(id);
    }

    public Role getParticipant1() {
        return participant1;
    }

    public void setParticipant1(Role sender) {
        this.participant1 = sender;
    }

    public String getId() {
        return super.getId();
    }

    public Role getParticipant2() {
        return participant2;
    }

    public void setParticipant2(Role receiver) {
        this.participant2 = receiver;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public boolean hasRole(Role role) {
        return this.participant1.equals(role) || this.participant2.equals(role);
    }

    public Set<Role> getRoles() {
        return new HashSet<Role>(Arrays.asList(this.participant1, this.participant2));
    }

    public Interaction clone() {
        return new Interaction(this.getName(), this.participant1, this.participant2, this.message);
    }

    public String toString() {
        return this.getName() + ": " + this.getParticipant1() + " -> " + this.getParticipant2() + " "
                + this.getMessage();
    }
}
