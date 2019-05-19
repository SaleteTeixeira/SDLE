package Common;

import Node.OrderedPostsByID;
import Node.Post;

import java.util.Set;
import java.util.TreeSet;

public class PostsReply {
    private Set<Post> posts;
    private Client from;
    private Client sender;
    private String to;

    public PostsReply(Set<Post> posts, Client from, Client sender, String to){
        this.posts = posts;
        this.from = from;
        this.sender = sender;
        this.to = to;
    }

    public Set<Post> getPosts() {
        Set<Post> result = new TreeSet<>(new OrderedPostsByID());

        for (Post p : this.posts) {
            result.add(p.clone());
        }

        return result;
    }

    public void setPosts(Set<Post> posts) {
        Set<Post> result = new TreeSet<>(new OrderedPostsByID());

        for (Post p : posts) {
            result.add(p.clone());
        }

        this.posts = result;
    }

    public Client getFrom() {
        return this.from.clone();
    }

    public void setFrom(Client from) {
        this.from = from.clone();
    }

    public Client getSender() {
        return this.sender.clone();
    }

    public void setSender(Client sender) {
        this.sender = sender.clone();
    }

    public String getTo() {
        return this.to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String toString(){
        StringBuilder ss = new StringBuilder();

        ss.append("----- Post Reply -----").append("\n");
        ss.append("To: ").append(this.to).append("\n");
        ss.append("From: ").append(this.from.toString()).append("\n");
        ss.append("Sender: ").append(this.sender.toString()).append("\n");
        ss.append("Posts: \n").append(this.posts.toString()).append("\n");

        return ss.toString();
    }
}
