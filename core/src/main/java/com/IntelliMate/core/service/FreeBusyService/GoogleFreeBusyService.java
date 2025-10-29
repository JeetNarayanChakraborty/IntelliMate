package com.IntelliMate.core.service.FreeBusyService;

import org.springframework.stereotype.Service;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;




@Service
public class GoogleFreeBusyService 
{
	private final GoogleOAuthService googleOAuthService;
	
	
	public GoogleFreeBusyService(GoogleOAuthService googleOAuthService) 
	{
		this.googleOAuthService = googleOAuthService;
	}
	
	
	
	
	
	
	

}
