package at.ac.c3pro.node;

public class XorGateway extends Gateway implements IGateway{

	public XorGateway(){
		super();
	}
	
	public XorGateway(String name){
		super(name);
	}
	public XorGateway(String name,String id){
		super(name, id);
	}
	
	public XorGateway clone(){
		return new XorGateway(this.getName());
	}
}
