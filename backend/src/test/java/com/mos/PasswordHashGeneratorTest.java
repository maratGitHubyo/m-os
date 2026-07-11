import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;

class PasswordHashGeneratorTest {

    @Test
    void writeAdminPasswordHashToFile() throws Exception {
        String hash = new BCryptPasswordEncoder().encode("admin123");
        Path output = Path.of("target/dev-seed-hash.txt");
        Files.writeString(output, hash);
    }
}
