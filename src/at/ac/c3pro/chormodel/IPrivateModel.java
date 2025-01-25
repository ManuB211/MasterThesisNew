package at.ac.c3pro.chormodel;


import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IPrivateNode;


public interface IPrivateModel extends IRpstModel<Edge<IPrivateNode>, IPrivateNode> {

    //void change(Fragment<E,V> F);
    String toDOT();
}
