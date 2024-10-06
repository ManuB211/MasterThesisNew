package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.RpstModel;

public class AndGateway extends Gateway implements IGateway{

	
	public AndGateway(){
		super();
	}
	
	public AndGateway(String name){
		super(name);
	}
	public AndGateway(String name, String id){
		super(name, id);
	}
	public AndGateway clone(){
		return new AndGateway(this.getName());
	}
	
}
