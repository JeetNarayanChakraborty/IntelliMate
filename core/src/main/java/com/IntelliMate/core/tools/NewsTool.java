package com.IntelliMate.core.tools;

import java.util.List;
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
	public String getNews(String query)
	{
		List<Article> articles = newsFetchService.fetchNews(query);
		
		if(articles.isEmpty())
		{
			return "No news articles found for the topic: " + query;
		}
		
		StringBuilder res = new StringBuilder();
		
		res.append("Here are the latest news articles:\n\n");

		for(int i=0; i<Math.min(5, articles.size()); i++)
		{
			Article article = articles.get(i);
		    res.append(i + 1).append(". **").append(article.getTitle()).append("**\n\n");  
		    res.append("   ").append(article.getDescription()).append("\n\n");  
		    res.append("   🔗 Link: ").append(article.getUrl()).append("\n\n---\n\n");  
		}
		
		
		return res.toString();
	}
}







