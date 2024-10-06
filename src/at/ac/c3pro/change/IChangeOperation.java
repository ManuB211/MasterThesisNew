package at.ac.c3pro.change;

import at.ac.c3pro.changePropagation.ChangePropagationUtil.ChgOpType;
import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.IRole;

public interface IChangeOperation {

	public void Propagate();

	public void Decompose();

	public String getId();

	public ChgOpType getType();

	public void setChoreography(Choreography c);

	public IRole getCurrentRole();

	public int getNb_nodes();
}
