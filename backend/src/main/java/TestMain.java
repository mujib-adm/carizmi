import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.crypto.password.PasswordEncoder;

void main() {

    PasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    System.out.println("admin123: " + passwordEncoder.encode("admin123"));

    byte[] keyBytes = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
    String base64Key = Base64.getEncoder().encodeToString(keyBytes);
    IO.println("Your JWT secret (base64): " + base64Key);
}
