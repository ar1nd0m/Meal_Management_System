/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author arindam
 */
package com.mealapp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;

public class DBConnection {

    private static Connection connection;

    private DBConnection() {
    }

    public static Connection getConnection() throws Exception {

        if (connection == null || connection.isClosed()) {

            Properties props = new Properties();

            try (InputStream is = DBConnection.class
                    .getClassLoader()
                    .getResourceAsStream("db.properties")) {

                if (is == null) {
                    throw new RuntimeException("db.properties not found");
                }
                props.load(is);
            }

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.username");
            String pass = props.getProperty("db.password");

            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, pass);
        }

        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
