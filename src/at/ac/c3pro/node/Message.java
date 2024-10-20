package at.ac.c3pro.node;

public class Message {

	public String name;
	public int size;
	public int importanceLevel; // TODO include in constructors
	public String id;
	// List<DataObjects> L = new LinkedList<DataObjects>();

	public Message(String name, String id) {
		this.id = id;
		this.name = name;
		size = 1;
		importanceLevel = 1;
		// To add Data Objects
		// Not yet implemented would be used for Compliance
	}

	public Message(String name) {
		this.name = name;
		size = 1;
		importanceLevel = 1;
		// To add Data Objects
		// Not yet implemented would be used for Compliance
	}

	public Message() {
		size = 1;
		importanceLevel = 1;
		// To add Data Objects
		// Not yet implemented would be used for Compliance
	}

	public String getId() {
		return id;
	}

	public int size() {
		return size;
	}

	public String toString() {
		return "Message_ID: " + this.id + ", Name: " + this.name != null ? this.name : "EMPTY";
	}

}
