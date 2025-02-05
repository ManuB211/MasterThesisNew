package at.ac.c3pro.node;

public class PrivateActivity extends PrivateNode {

    public PrivateActivity() {
        super();
    }

    public PrivateActivity(String name) {
        super(name);
    }

    public PrivateActivity(String name, String id) {
        super(name);
        this.setId(id);
    }

    public PublicNode getPublicActivity() {
        return new PublicNode(this.getName()) {
        };
    }

}
