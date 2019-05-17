package Common.Messages;

import Common.Client;

public class PostsRequest {
    private int postCausalID;
    private Client from;
    private String to;
    private int ttl;

    public PostsRequest(int causalID, Client from, String to, int ttl){
        this.postCausalID = causalID;
        this.from = from.clone();
        this.to = to;
        this.ttl = ttl;
    }

    public int getPostCausalID() {
        return this.postCausalID;
    }

    public void setPostCausalID(int postCausalID) {
        this.postCausalID = postCausalID;
    }

    public Client getFrom() {
        return this.from.clone();
    }

    public void setFrom(Client from) {
        this.from = from.clone();
    }

    public String getTo() {
        return this.to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public int getTTL() {
        return this.ttl;
    }

    public void setTTL(int ttl) {
        this.ttl = ttl;
    }

    public String toString(){
        StringBuilder ss = new StringBuilder();

        ss.append("----- Post Request -----").append("\n");
        ss.append("TTL: ").append(this.ttl).append("\n");
        ss.append("Post ID: ").append(this.postCausalID).append("\n");
        ss.append("To: ").append(this.to).append("\n");
        ss.append("From: ").append(this.from.toString()).append("\n");

        return ss.toString();
    }
}