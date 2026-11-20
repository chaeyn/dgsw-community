package kr.hs.dgsw.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DgswCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(DgswCommunityApplication.class, args);
    }

}
