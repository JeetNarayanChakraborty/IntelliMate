package com.IntelliMate.core.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import com.IntelliMate.core.repository.newsRepo.Article;
import com.IntelliMate.core.service.NewsService.NewsFetchService;
import dev.langchain4j.agent.tool.Tool;




@Component
public class NewsTool 
{
	private NewsFetchService newsFetchService;
	
	
	public NewsTool(NewsFetchService newsFetchService)
	{
		this.newsFetchService = newsFetchService;
	}
	
	@Tool("Fetches latest news articles on a given topic.")
	public Map<String, Object> getNews(String query)
	{
	    List<Article> articles = newsFetchService.fetchNews(query);
	    
	    if(articles.isEmpty()) 
	    {
	        return Map.of(
	            "success", false,
	            "message", "No news articles found for: " + query
	        );
	    }
	    
	    // Return structured data
	    List<Map<String, String>> articleList = articles.stream()
	        .limit(5)
	        .map(article -> Map.of(
	            "title", article.getTitle(),
	            "description", article.getDescription(),
	            "url", article.getUrl()
	        ))
	        .toList();
	    
	    return Map.of(
	        "success", true,
	        "topic", query,
	        "articles", articleList,
	        "count", articleList.size()
	    );
	}
}







