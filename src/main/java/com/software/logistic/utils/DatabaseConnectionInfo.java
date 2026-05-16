package com.software.logistic.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatabaseConnectionInfo {

    private static final Pattern JDBC_MYSQL =
            Pattern.compile("jdbc:mysql://([^:/]+)(?::(\\d+))?/([^?]+)");

    private final String host;
    private final String port;
    private final String databaseName;

    private DatabaseConnectionInfo(String host, String port, String databaseName) {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
    }

    public static DatabaseConnectionInfo fromJdbcUrl(String jdbcUrl) {
        Matcher matcher = JDBC_MYSQL.matcher(jdbcUrl);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unsupported JDBC URL: " + jdbcUrl);
        }
        String host = matcher.group(1);
        String port = matcher.group(2) != null ? matcher.group(2) : "3306";
        String databaseName = matcher.group(3);
        return new DatabaseConnectionInfo(host, port, databaseName);
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
