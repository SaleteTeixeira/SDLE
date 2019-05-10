package Common;

import java.util.List;

//todo (geral): causalID nisto???
public class SuggestionsReply {
    private String publisherKey;
    private List<String> suggestedKeys;

    public SuggestionsReply(String publisherKey, List<String> suggestedKeys){
        this.publisherKey = publisherKey;
        this.suggestedKeys = suggestedKeys;
    }

    public String getPublisherKey() {
        return this.publisherKey;
    }

    public List<String> getSuggestedKeys() {
        return this.suggestedKeys;
    }

    public String toString(){
        return "----- Suggestions Reply -----\n" +
                "Suggested by: "+this.publisherKey+"\n"+
                "Suggested publishers keys:\n"+
                    this.suggestedKeys.toString();
    }
}