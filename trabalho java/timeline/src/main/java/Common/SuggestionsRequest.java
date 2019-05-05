package Common;

public class SuggestionsRequest {
    private Client from; //who requests
    private String to;  //who replies

    public SuggestionsRequest(Client from, String to){
        this.from = from;
        this.to = to;
    }

    public Client getFrom() {
        return this.from;
    }

    public String getTo() {
        return this.to;
    }

    public String toString(){
        return "----- Suggestions Request -----\n" +
                "To: "+this.to+"\n"+
                "From: \n"+this.from.toString();
    }
}