package com.IntelliMate.core.service.EncryptionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;



class JasyptEncryptionService_Test 
{
    private JasyptEncryptionService encryptionService;
    private final String MASTER_PASSWORD = "test-master-secret-key-123";

    @BeforeEach
    void setUp() 
    {
        encryptionService = new JasyptEncryptionService();
        ReflectionTestUtils.setField(encryptionService, "masterPassword", MASTER_PASSWORD);
        encryptionService.init();
    }

    @Test
    @DisplayName("Should encrypt and decrypt text successfully")
    void test_EncryptionAndDecryption() 
    {
        // Arrange
        String originalText = "Sensitive-OAuth-Token-2026";

        // Act
        String encryptedText = encryptionService.encrypt(originalText);
        String decryptedText = encryptionService.decrypt(encryptedText);

        // Assert
        assertThat(encryptedText).isNotNull();
        assertThat(encryptedText).isNotEqualTo(originalText);
        assertThat(decryptedText).isEqualTo(originalText);
    }

    @Test
    @DisplayName("Should produce different cipher text for the same plain text (Random IV check)")
    void test_RandomIvEffect() 
    {
        // Arrange
        String plainText = "Same-Text";

        // Act
        String firstEncryption = encryptionService.encrypt(plainText);
        String secondEncryption = encryptionService.encrypt(plainText);

        // Assert
        // Because of RandomIvGenerator, two encryptions of the same text should look different
        assertThat(firstEncryption).isNotEqualTo(secondEncryption);
        
        // But both must decrypt back to the same plain text
        assertThat(encryptionService.decrypt(firstEncryption)).isEqualTo(plainText);
        assertThat(encryptionService.decrypt(secondEncryption)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should throw exception when decrypting invalid cipher text")
    void test_DecryptInvalidText() 
    {
        String invalidCipher = "not-a-valid-encrypted-string";
        
        assertThrows(Exception.class, () -> 
        {
            encryptionService.decrypt(invalidCipher);
        });
    }
}





