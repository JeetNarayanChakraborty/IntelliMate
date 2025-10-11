package com.IntelliMate.core.service.NewsService;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.IntelliMate.core.repository.newsRepo.Article;
import com.IntelliMate.core.repository.newsRepo.NewsAPIResponse;



@Service
public class NewsFetchService 
{
	@Value("${news.api.key}")
	private String apiKey;
	
	@Value("${news.api.url}")
	private String apiUrl;
	
	private RestTemplate restTemplate;

	
    public NewsFetchService() 
    {
        this.restTemplate = new RestTemplate();
    }
    
    public List<Article> fetchNews(String query)
    {
    	try 
    	{
    		// Build URL with query parameters
        	String url = UriComponentsBuilder.fromUriString(apiUrl)
        				.queryParam("q", query)
    	                .queryParam("apiKey", apiKey)
    	                .queryParam("pageSize", 10)  // Limit to 10 articles
    	                .queryParam("sortBy", "publishedAt")  // Latest first
    	                .queryParam("language", "en")  // English language only
    					.toUriString();
        	
        	// Make the API call
        	NewsAPIResponse response = restTemplate.getForObject(url, NewsAPIResponse.class);

        	
            // Return articles or empty list
            if(response != null && "ok".equals(response.getStatus())) 
            {
                return response.getArticles();
            }
        	
            return new ArrayList<>();	
    	}
    	
        catch(Exception e) 
        {
        	System.err.println("Error fetching news: " + e.getMessage());
        	return new ArrayList<>();	
        }
    }
    
    public List<Article> fetchNewsByCategory(String category)
	{
		try 
		{
			// Build URL with query parameters
			String url = UriComponentsBuilder.fromUriString(apiUrl)
						.queryParam("category", category)
		                .queryParam("apiKey", apiKey)
		                .queryParam("pageSize", 10)  // Limit to 10 articles
		                .queryParam("sortBy", "publishedAt")  // Latest first
		                .queryParam("language", "en")  // English language only
						.toUriString();
			
			// Make the API call
			NewsAPIResponse response = restTemplate.getForObject(url, NewsAPIResponse.class);

			
			// Return articles or empty list
			if(response != null && "ok".equals(response.getStatus())) 
			{
				return response.getArticles();
			}
			
			return new ArrayList<>();	
		}
		
		catch(Exception e) 
		{
			System.err.println("Error fetching news: " + e.getMessage());
			return new ArrayList<>();	
		}
	}
}






