package Common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Util {
    public static String getPublicIp() {
        URL whatismyip = null;
        try {
            whatismyip = new URL("http://checkip.amazonaws.com");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.out.println("Hardcoded ip address, this should not happen.");
            return null;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error connecting to public ip external service");
            return null;
        }

        String ip = null; //you get the IP as a String
        try {
            ip = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error parsing ip address");
            return null;
        }
        return ip;
    }

    public static String LoadRSAKey(String RSAFile) {
        byte[] keyBytes = new byte[0];
        try {
            keyBytes = Files.readAllBytes(Paths.get(RSAFile));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = null;
        try {
            kf = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        try {
            return kf.generatePublic(spec).toString();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }
}