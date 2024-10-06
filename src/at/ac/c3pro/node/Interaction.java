package at.ac.c3pro.node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import at.ac.c3pro.chormodel.Role;

public class Interaction extends ChoreographyNode {

	private Role sender = null;
	private Role receiver = null;
	private Message message = null;

	public Interaction() {
		super();
	}

	public Interaction(String name, Role sender, Role receiver, Message message) {
		super(name);
		this.setSender(sender);
		this.setReceiver(receiver);
		this.setMessage(message);
	}

	public Interaction(String name, String id, Role sender, Role receiver, Message message) {
		super(name);
		this.setSender(sender);
		this.setReceiver(receiver);
		this.setMessage(message);
		this.setId(id);
	}

	public Role getSender() {
		return sender;
	}

	public void setSender(Role sender) {
		this.sender = sender;
	}

	public String getId() {
		return super.getId();
	}

	public Role getReceiver() {
		return receiver;
	}

	public void setReceiver(Role receiver) {
		this.receiver = receiver;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public boolean hasRole(Role role) {
		return this.sender.equals(role) || this.receiver.equals(role);
	}

	public Set<Role> getRoles() {
		return new HashSet<Role>(Arrays.asList(new Role[] { this.sender, this.receiver }));
	}

	public Interaction clone() {
		return new Interaction(this.getName(), this.sender, this.receiver, this.message);
	}
}
