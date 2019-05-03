package Client;

import java.util.Date;

public class Post {
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

    //todo (sofia): comparar se é maior que 1 semana
}
