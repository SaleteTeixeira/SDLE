package Common;

import io.atomix.utils.net.Address;
import java.io.Serializable;

public class Client implements Serializable {
    private String username;
    private String key;
    private Address address;

    public Client(String tempUsername, String k) {
        this.username = tempUsername;
        this.key = k;
        this.address = null;
    }

    public Client(String u, String k, Address a) {
        this.username = u;
        this.key = k;
        this.address = a;
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
        return this.address;
    }

    public void setAddress(Address address){
        this.address = address;
    }

    public String toString() {
        StringBuilder ss = new StringBuilder();

        ss.append("----- Client -----").append("\n");
        ss.append("Username: ").append(this.username).append("\n");
        ss.append("Public key: ").append(this.key).append("\n");
        ss.append("Address: ").append(this.address.toString()).append("\n");

        return ss.toString();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Client clone() {
        return new Client(this.username, this.key, this.address);
    }
}
