import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;

class PasswordHashGeneratorTest {

    @Test
    void writeAdminPasswordHashToFile() throws Exception {
        String hash = new BCryptPasswordEncoder().encode("Kv7nR2xP");
        Path output = Path.of("target/dev-seed-hash.txt");
        Files.writeString(output, hash);
    }
}
