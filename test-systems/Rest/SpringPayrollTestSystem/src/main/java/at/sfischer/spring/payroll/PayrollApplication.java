package at.sfischer.spring.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PayrollApplication {

    // http://localhost:8080/v3/api-docs

    public static void main(String... args) {
        SpringApplication.run(PayrollApplication.class, args);
    }
}
