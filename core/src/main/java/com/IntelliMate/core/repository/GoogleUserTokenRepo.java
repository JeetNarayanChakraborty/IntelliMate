package com.IntelliMate.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;



@Repository
public interface GoogleUserTokenRepo extends JpaRepository<GoogleAuthUserToken, Long>
{
	@Query(value = "SELECT * FROM google_auth_user_token WHERE refresh_token = :refreshToken", nativeQuery = true)
	GoogleAuthUserToken findByRefreshToken(String refreshToken);
	
	
	//TODO: Implement to existsByRefreshToken
	
	
	GoogleAuthUserToken findByUserId(Long userId);
	boolean existsByUserId(Long userId);
	void deleteByUserId(Long userId);
}
