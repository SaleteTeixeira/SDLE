package Node;

import java.io.Serializable;
import java.util.Date;

public class Post implements Serializable  {
    private String post;
    private Integer causalID;
    private Date utc;

    public Post(String post, Integer causalID){
        this.post = post;
        this.causalID = causalID;
        this.utc = new Date(); //current date time
    }

    public String getPost() {
        return post;
    }

    public Integer getCausalID() {
        return causalID;
    }

    public Date getUtc() {
        return utc;
    }

    public String toString(){
        StringBuilder ss = new StringBuilder();

        ss.append("----- Post -----").append("\n");
        ss.append("Post: ").append(this.post).append("\n");
        ss.append("Causal ID: ").append(this.causalID).append("\n");
        ss.append("UTC: ").append(this.utc).append("\n");

        return ss.toString();
    }

    public boolean oneWeekOld(){
        int oneWeekSec = 604800000;

        Date today = new Date();
        Date limit = new Date(today.getTime()-oneWeekSec);

        return this.utc.before(limit);
    }
}
