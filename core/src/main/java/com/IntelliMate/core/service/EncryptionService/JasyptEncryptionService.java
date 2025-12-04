package com.IntelliMate.core.service.EncryptionService;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;



@Service
public class JasyptEncryptionService 
{
	@Value("${JASYPT_ENCRYPTOR_PASSWORD}")
	private String masterPassword;
	
	private StandardPBEStringEncryptor encryptor;
	
	
	
	public JasyptEncryptionService() {}
	
	// Init method to initialize the encryptor with the master password
	// and algorithm and IV generator
	@PostConstruct
    public void init() 
	{
        encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(masterPassword);
        encryptor.setAlgorithm("PBEWITHHMACSHA256ANDAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
    }
	
	// Encrypt a plain text string
	public String encrypt(String plainText) 
	{
		return encryptor.encrypt(plainText);
	}
	
	// Decrypt a cipher text string
	public String decrypt(String cipherText) 
	{
		return encryptor.decrypt(cipherText);
	}
}







