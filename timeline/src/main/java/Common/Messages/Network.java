package Common;

import java.util.HashMap;
import java.util.Map;

public class Network {
    private Map<String, Client> network;

    public Network(Map<String, Client> network){
        this.network = new HashMap<>();
        for (Map.Entry<String, Client> entry : network.entrySet()) {
            this.network.put(entry.getKey(), entry.getValue().clone());
        }
    }

    public Map<String, Client> getNetwork() {
        Map<String, Client> newMap = new HashMap<>();

        for (Map.Entry<String, Client> entry : this.network.entrySet()) {
            newMap.put(entry.getKey(), entry.getValue().clone());
        }

        return newMap;
    }

    public String toString(){
        StringBuilder ss = new StringBuilder();

        ss.append("----- Network -----").append("\n");
        for (Client c : this.network.values()) {
            ss.append(c.toString());
        }

        return ss.toString();
    }
}