package com.kipu_fav.read_module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ReadModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadModuleApplication.class, args);
    }

}
