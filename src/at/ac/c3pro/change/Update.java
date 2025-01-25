package at.ac.c3pro.change;

import at.ac.c3pro.changePropagation.ChangePropagationUtil.ChgOpType;

public class Update extends ChangeOperation implements IChangeOperation {

    @Override
    public void Propagate() {
        // TODO Auto-generated method stub

    }

    public int getNb_nodes() {

        return 0;
    }

    @Override
    public void Decompose() {
        // TODO Auto-generated method stub

    }

    public ChgOpType getType() {
        return ChgOpType.Update;
    }

}
