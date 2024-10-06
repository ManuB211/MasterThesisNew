package at.ac.c3pro.change;

import java.util.UUID;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.INode;
import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.IRole;
import at.ac.c3pro.chormodel.Role;

public abstract class ChangeOperation<E extends Edge<N>, N extends INode>  implements IChangeOperation{
    private String id = "";
    public Choreography c = null;
    public Role currentRole;
    
    
    
    public ChangeOperation() {
        this.id = UUID.randomUUID().toString();
    }
    @Override
    public void Propagate() {
        // TODO Auto-generated method stub
        //
    }
    

    @Override
    public void Decompose() {
        // TODO Auto-generated method stub
        //
    }

    public String getId() {
        return this.id;
    }

    public IRole getCurrentRole(){
    	return this.currentRole;
    }
  
    @Override
    public void setChoreography(Choreography c) {
        this.c = c;
    }

}
