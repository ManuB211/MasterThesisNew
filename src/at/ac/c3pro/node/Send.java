package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.Role;

public class Send extends InteractionActivity {
	

	public Send(Role target, Message message, String name) {
		super(target, message, name);		
	}
	
	public Send(Role target, Message message, String name, String id) {
		super(target, message, name);		
		this.setId(id);
	}
	
	//Sender represents the role of the model containing the current Send
	public Receive Complement(Role sender){
		return(new Receive(sender, this.getMessage(),this.getName().replaceAll("send", "receive")));
	}
	
	public Send clone(){
		return new Send(this.role,this.getMessage(),this.getName());
	}
	
	public Role getTargetRole(){
		return this.role;
	}
	
}
