package at.ac.c3pro.node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import at.ac.c3pro.chormodel.Role;

public abstract class InteractionActivity extends Node implements IPublicNode, IPrivateNode{

	 private Message message=null;
	 public Role role=null;
	 
	 
	public InteractionActivity(Role role, Message message, String name){
		super(name);
		this.role= role;
		this.message= message;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
	
	public InteractionActivity Complement(Role role){	
		
		if (this instanceof Receive)
			return(new Send(role, this.message,this.getName().replaceAll("receive", "send")));
		else 
			return(new Receive(role, this.message,this.getName().replaceAll("receive", "send")));
			
	}
	
	public boolean hasRole(Role role) {
		return this.role.equals(role);
	}

    public Set<Role> getRoles() {
        return new HashSet<Role>(Arrays.asList(new Role[]{ this.role }));
    }
    
    public InteractionActivity clone(){
    	if(this instanceof Send)
    		return new Send(this.role, this.message, this.getName());
    	return new Receive(this.role, this.message, this.getName());
    }
	
}
