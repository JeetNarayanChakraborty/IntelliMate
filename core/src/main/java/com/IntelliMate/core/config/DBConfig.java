package com.IntelliMate.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;



@Configuration
public class DBConfig 
{
    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${db.password.path}")
    private String passwordPath;
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    
    @Bean
    public DataSource dataSource() 
    {
        try 
        {
        	// Read the database password from the specified file
            String password = Files.readString(Paths.get(passwordPath)).trim();

            return DataSourceBuilder.create()
                    .url(url)
                    .username(username)
                    .password(password)
                    .driverClassName(driverClassName)
                    .build();
        } 
        
        catch(IOException e) 
        {
        	// Handle the exception appropriately
            throw new RuntimeException("CRITICAL: Could not read DB password from: " + passwordPath, e);
        }
    }
}





