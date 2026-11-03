package sumdu.edu.ua.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Executable JAR (embedded Tomcat): started in the Docker image with java -jar app.jar. */
@SpringBootApplication(scanBasePackages = "sumdu.edu.ua")
public class AppInit {
  public static void main(String[] args) {
    SpringApplication.run(AppInit.class, args);
  }
}
