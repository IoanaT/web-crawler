import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class JsLibrary {

    private String name;

    private String md5;

    JsLibrary(String name, String page) {
        this.name = name;
        this.md5 = calculateMd5(page);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    private String calculateMd5(String page) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(page.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsLibrary jsLibrary = (JsLibrary) o;
        return md5.equals(jsLibrary.md5);
    }

    @Override
    public int hashCode() {
        return Objects.hash(md5);
    }

    @Override
    public String toString() {
        return this.name + " ";
    }

}
