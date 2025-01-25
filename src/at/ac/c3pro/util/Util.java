package at.ac.c3pro.util;

import java.util.LinkedList;
import java.util.List;

public class Util {

    public Util() {
        // TODO Auto-generated constructor stub
    }

    public static <O extends Object> List<List<O>> splitOperations(List<O> all_ops, int num) {
        List<List<O>> splitted_ops = new LinkedList<List<O>>();
        double slot_size = Math.ceil(all_ops.size() / num);
        int cur_idx = 0;

        for (int i = 1; i <= num; i++) {
            if (i == num) {
                // get the rest
                splitted_ops.add(all_ops.subList(cur_idx, all_ops.size() - 1));
            } else {
                // get slot wise
                splitted_ops.add(all_ops.subList(cur_idx, (int) (cur_idx + slot_size - 1)));
            }
            cur_idx += slot_size - 1;
        }
        return splitted_ops;
    }

}
