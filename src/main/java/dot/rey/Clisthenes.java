package dot.rey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("dot.rey")
public class Clisthenes {
    public static void main(String... args) {
        SpringApplication.run(Clisthenes.class, args);
    }

}
