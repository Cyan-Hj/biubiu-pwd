import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class EncryptPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "rr031108";
        String encoded = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("Encoded: " + encoded);
    }
}
