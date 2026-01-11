package com.IntelliMate.core.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.IntelliMate.core.repository.newsRepo.Article;
import com.IntelliMate.core.service.NewsService.NewsFetchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;




@ExtendWith(MockitoExtension.class)
class NewsTool_Test 
{
    @Mock
    private NewsFetchService newsFetchService;

    @InjectMocks
    private NewsTool newsTool;

    @Test
    @DisplayName("GIVEN news articles exist WHEN fetching THEN return limited and structured results")
    void getNews_ShouldReturnStructuredArticles() 
    {
        // Arrange
        String query = "Artificial Intelligence";
        List<Article> mockArticles = new ArrayList<>();
        
        for(int i=1; i<=7; i++) 
        {
            Article article = new Article();
            article.setTitle("Title " + i);
            article.setDescription("Description " + i);
            article.setUrl("http://news.com/" + i);
            mockArticles.add(article);
        }

        when(newsFetchService.fetchNews(query)).thenReturn(mockArticles);

        // Act
        Map<String, Object> response = newsTool.getNews(query);

        // Assert
        assertThat(response.get("success")).isEqualTo(true);
        assertThat(response.get("topic")).isEqualTo(query);
        assertThat(response.get("count")).isEqualTo(5); 

        @SuppressWarnings("unchecked")
        List<Map<String, String>> articles = (List<Map<String, String>>) response.get("articles");
        
        assertThat(articles).hasSize(5);
        assertThat(articles.get(0).get("title")).isEqualTo("Title 1");
        assertThat(articles.get(0).get("url")).isEqualTo("http://news.com/1");
        
        verify(newsFetchService, times(1)).fetchNews(query);
    }

    @Test
    @DisplayName("GIVEN no articles found WHEN fetching THEN return failure message")
    void getNews_ShouldHandleEmptyResults()
    {
        // Arrange
        String query = "Unknown Topic";
        when(newsFetchService.fetchNews(anyString())).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> response = newsTool.getNews(query);

        // Assert
        assertThat(response.get("success")).isEqualTo(false);
        assertThat(response.get("message")).isEqualTo("No news articles found for: " + query);
        assertThat(response.get("articles")).isNull();
    }

    @Test
    @DisplayName("GIVEN a service exception WHEN fetching THEN allow exception to propagate")
    void getNews_ShouldPropagateServiceExceptions() 
    {
        // Arrange
        when(newsFetchService.fetchNews(anyString())).thenThrow(new RuntimeException("News API Unavailable"));

        // Act & Assert
        try 
        {
            newsTool.getNews("breaking news");
        } 
        
        catch(Exception e) 
        {
            assertThat(e.getMessage()).isEqualTo("News API Unavailable");
        }
    }
}







