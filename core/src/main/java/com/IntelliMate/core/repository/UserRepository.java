package com.IntelliMate.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



@Repository
public interface UserRepository extends JpaRepository<User, String>
{
    User findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    void deleteByEmail(String email);
    
    User findByGoogleId(String googleId);
    
    boolean existsByGoogleId(String googleId);
    
    void deleteByGoogleId(String googleId);
    
    @Query("SELECT u FROM User u WHERE u.googleRefreshToken = :refreshToken")
    User findByGoogleRefreshToken(@Param("refreshToken") String refreshToken);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.googleRefreshToken = :refreshToken")
    boolean existsByGoogleRefreshToken(@Param("refreshToken") String refreshToken);
    
    @Query("SELECT u FROM User u WHERE u.googleAccessToken = :accessToken")
    User findByGoogleAccessToken(@Param("accessToken") String accessToken);
    
    @Query("SELECT u FROM User u WHERE u.googleTokenExpiry < :currentTime AND u.googleTokenExpiry IS NOT NULL")
    List<User> findUsersWithExpiredTokens(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT u FROM User u WHERE u.googleTokenExpiry > :currentTime AND u.googleAccessToken IS NOT NULL")
    List<User> findUsersWithValidTokens(@Param("currentTime") LocalDateTime currentTime);
}







