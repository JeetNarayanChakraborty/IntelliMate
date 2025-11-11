package com.IntelliMate.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;



@Repository
public interface GoogleUserTokenRepo extends JpaRepository<GoogleAuthUserToken, String>
{
	@Query(value = "SELECT * FROM google_auth_user_token WHERE refresh_token = :refreshToken", nativeQuery = true)
	GoogleAuthUserToken findByRefreshToken(String refreshToken);
	
	@Query(value = "SELECT EXISTS(SELECT 1 FROM google_auth_user_token WHERE refresh_token = :refreshToken)", nativeQuery = true)
	boolean existsByRefreshToken(String refreshToken);
	
	GoogleAuthUserToken findByUserId(String userId);
	boolean existsByUserId(String userId);
	void deleteByUserId(String userId);
}
