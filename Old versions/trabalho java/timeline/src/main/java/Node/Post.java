package Node;

import java.io.Serializable;
import java.util.Date;

public class Post implements Serializable  {
    private String publisher;
    private String post;
    private Integer causalID;
    private Date utc;

    public Post(String publisher, String post, Integer causalID){
        this.publisher = publisher;
        this.post = post;
        this.causalID = causalID;
        this.utc = new Date(); //current date time
    }

    private Post(String publisher, String post, Integer causalID, Date utc){
        this.publisher = publisher;
        this.post = post;
        this.causalID = causalID;
        this.utc = utc;
    }

    public String getPublisher() {
        return this.publisher;
    }

    public String getPost() {
        return this.post;
    }

    public Integer getCausalID() {
        return this.causalID;
    }

    public Date getUtc() {
        return this.utc;
    }

    public String toString(){
        StringBuilder ss = new StringBuilder();

        ss.append("----- Post -----").append("\n");
        ss.append("Publisher: ").append(this.publisher).append("\n");
        ss.append("Post: ").append(this.post).append("\n");
        ss.append("Causal ID: ").append(this.causalID).append("\n");
        ss.append("UTC: ").append(this.utc).append("\n");

        return ss.toString();
    }

    @Override
    public Post clone(){
        return new Post(this.publisher, this.post, this.causalID, this.utc);
    }

    public boolean oneWeekOld(){
        int oneWeekSec = 604800000;

        Date today = new Date();
        Date limit = new Date(today.getTime()-oneWeekSec);

        return this.utc.before(limit);
    }
}
