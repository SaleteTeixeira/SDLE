package Common;

import io.atomix.utils.net.Address;
import java.io.Serializable;

public class Client implements Serializable {
    private String username;
    private String key;
    private Common.Address address;

    public Client(String tempUsername, String k) {
        this.username = tempUsername;
        this.key = k;
        this.address = null;
    }

    public Client(String u, String k, Address a) {
        this.username = u;
        this.key = k;
        if(a==null) this.address=null;
        else this.address = new Common.Address(a.host(), a.port());
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Address getAddress() {
        if(this.address==null) return null;
        else return this.address.get();
    }

    public void setAddress(Address address){
        this.address = new Common.Address(address.host(), address.port());
    }

    public String toString() {
        StringBuilder ss = new StringBuilder();

        ss.append("----- Client -----").append("\n");
        ss.append("Username: ").append(this.username).append("\n");
        ss.append("Public key: ").append(this.key).append("\n");
        if(this.address!=null){
            ss.append("Address: ").append(this.address.get().toString()).append("\n");
        }
        else {
            ss.append("Adress: ND").append("\n");
        }

        return ss.toString();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Client clone() {
        return new Client(this.username, this.key, this.getAddress());
    }
}
