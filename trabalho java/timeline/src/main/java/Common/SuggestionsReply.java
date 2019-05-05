package Common;

import java.util.List;

//todo (geral): causalID nisto???
public class SuggestionsReply {
    private List<String> suggestedKeys;
    private Client from; //who replies
    private String to;  //who requested

    public SuggestionsReply(List<String> suggestedKeys, Client from, String to){
        this.suggestedKeys = suggestedKeys;
        this.from = from;
        this.to = to;
    }

    public List<String> getSuggestedKeys() {
        return this.suggestedKeys;
    }

    public Client getFrom() {
        return this.from;
    }

    public String getTo() {
        return this.to;
    }

    public String toString(){
        return "----- Suggestions Reply -----\n" +
                "To: "+this.to+"\n"+
                "From: \n"+
                this.from.toString()+
                "Suggested Keys:\n"+
                this.suggestedKeys.toString();
    }
}