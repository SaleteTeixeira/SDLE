package Node;

import java.util.Comparator;

public class ChronologicalPosts implements Comparator<Post> {

    @Override
    public int compare(Post p1, Post p2) {
        if(p1.getUtc().after(p2.getUtc())) return -1;
        if(p1.getUtc().before(p2.getUtc())) return 1;
        return 0;
    }
}
