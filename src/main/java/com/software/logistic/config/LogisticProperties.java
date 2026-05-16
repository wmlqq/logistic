package com.software.logistic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "logistic")
public class LogisticProperties {

    private Backup backup = new Backup();

    @Data
    public static class Backup {
        /**
         * Directory for mysqldump backup files.
         */
        private String directory = System.getProperty("user.home") + "/.logistic/backups";
    }
}
