package com.IntelliMate.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface GoogleUserTokenRepo extends JpaRepository<GoogleAuthUserToken, Long>
{
	GoogleAuthUserToken findByUserId(Long userId);
	boolean existsByUserId(Long userId);
	void deleteByUserId(Long userId);
}
