package Node;

import java.util.Comparator;

public class OrderedPostsByID implements Comparator<Post> {

    @Override
    public int compare(Post p1, Post p2) {
        if(p1.getCausalID() > p2.getCausalID()) return 1;
        if(p1.getCausalID() < p2.getCausalID()) return -1;
        return 0;
    }
}
