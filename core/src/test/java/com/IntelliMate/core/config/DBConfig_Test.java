package com.IntelliMate.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;



class DBConfig_Test 
{
    private final DBConfig dbConfig = new DBConfig();

    
    
    @Test
    @DisplayName("Verify successful DataSource creation with external password")
    void should_ReturnDataSource_When_PasswordFileIsValid() 
    {
        // Arrange: Injection of configuration values via Reflection
        ReflectionTestUtils.setField(dbConfig, "url", "jdbc:h2:mem:test");
        ReflectionTestUtils.setField(dbConfig, "username", "sa");
        ReflectionTestUtils.setField(dbConfig, "passwordPath", "/secrets/db_pass.txt");
        ReflectionTestUtils.setField(dbConfig, "driverClassName", "org.h2.Driver");

        // Act: Mocking of static file system call and bean initialization
        try(MockedStatic<Files> mockedFiles = mockStatic(Files.class)) 
        {
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenReturn("secret_password");

            DataSource dataSource = dbConfig.dataSource();

            // Assert: Confirmation of non-null bean and type verification
            assertThat(dataSource).isNotNull();
        }
    }

    @Test
    @DisplayName("Verify RuntimeException when password file is inaccessible")
    void should_ThrowRuntimeException_When_FileReadFails() 
    {
        // Arrange: Setup of invalid path configuration
        ReflectionTestUtils.setField(dbConfig, "passwordPath", "invalid/path");

        // Act & Assert: Verification of error handling for IO failures
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) 
        {
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenThrow(new IOException("File not found"));

            assertThrows(RuntimeException.class, dbConfig::dataSource, 
                "Expected RuntimeException when password file cannot be read");
        }
    }
}





