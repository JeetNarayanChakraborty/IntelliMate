package com.IntelliMate.core.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;



@Service
public class UserService implements UserDetailsService 
{
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException 
    {
    	User user = userRepository.findByEmail(email);
    	
    	if(user == null) throw new UsernameNotFoundException("User not found: " + email);
    	
    	String password = (user.getPassword() != null) ? user.getPassword() : "OAUTH2_USER";

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(password) 
                .roles("USER")   
                .disabled(false)
                .build();
    }
}




