package at.ac.c3pro.change;

import at.ac.c3pro.changePropagation.ChangePropagationUtil.ChgOpType;
import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.IRole;

public interface IChangeOperation {

    void Propagate();

    void Decompose();

    String getId();

    ChgOpType getType();

    void setChoreography(Choreography c);

    IRole getCurrentRole();

    int getNb_nodes();
}
