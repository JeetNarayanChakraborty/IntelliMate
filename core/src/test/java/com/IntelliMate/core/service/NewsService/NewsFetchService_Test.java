package com.IntelliMate.core.service.NewsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.IntelliMate.core.repository.newsRepo.Article;
import com.IntelliMate.core.repository.newsRepo.NewsAPIResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.List;




@ExtendWith(MockitoExtension.class)
class NewsFetchService_Test 
{
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NewsFetchService newsFetchService;

    private final String MOCK_API_KEY = "test_key_123";
    private final String MOCK_API_URL = "https://newsapi.org/v2/everything";

    
    
    @BeforeEach
    void setUp() 
    {
        // Injecting @Value fields manually since Spring context isn't loaded in unit tests
        ReflectionTestUtils.setField(newsFetchService, "apiKey", MOCK_API_KEY);
        ReflectionTestUtils.setField(newsFetchService, "apiUrl", MOCK_API_URL);
        
        // Injecting the mocked RestTemplate into the service
        ReflectionTestUtils.setField(newsFetchService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("GIVEN a valid query WHEN fetching news THEN return list of articles and verify URI")
    void fetchNews_ShouldReturnArticles_OnSuccess() 
    {
        // Arrange
        String query = "Artificial Intelligence";
        Article article = new Article();
        article.setTitle("AI Revolution");
        
        NewsAPIResponse mockResponse = new NewsAPIResponse();
        mockResponse.setStatus("ok");
        mockResponse.setArticles(Collections.singletonList(article));

        // Stubbing
        when(restTemplate.getForObject(anyString(), eq(NewsAPIResponse.class))).thenReturn(mockResponse);

        // Act
        List<Article> result = newsFetchService.fetchNews(query);

        // Assert
        assertThat(result).hasSize(1);
        
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(urlCaptor.capture(), eq(NewsAPIResponse.class));
        
        String capturedUrl = urlCaptor.getValue();
        
        // Verify individual components independently to avoid encoding/ordering issues
        assertThat(capturedUrl).startsWith(MOCK_API_URL);
        assertThat(capturedUrl).contains("apiKey=" + MOCK_API_KEY);
        assertThat(capturedUrl).contains("pageSize=10");
        assertThat(capturedUrl).contains("language=en");
        
        // Check for either '+' or '%20' for the space in the query
        assertThat(capturedUrl).matches(url -> 
            url.contains("q=Artificial+Intelligence") || url.contains("q=Artificial%20Intelligence")
        );
    }

    @Test
    @DisplayName("GIVEN a category WHEN status is not 'ok' THEN return empty list")
    void fetchNewsByCategory_ShouldReturnEmpty_OnApiFailure() 
    {
        // Arrange
        NewsAPIResponse errorResponse = new NewsAPIResponse();
        errorResponse.setStatus("error");

        when(restTemplate.getForObject(anyString(), eq(NewsAPIResponse.class))).thenReturn(errorResponse);

        // Act
        List<Article> result = newsFetchService.fetchNewsByCategory("technology");

        // Assert
        assertThat(result).isEmpty();
        verify(restTemplate).getForObject(contains("category=technology"), eq(NewsAPIResponse.class));
    }

    @Test
    @DisplayName("GIVEN an HTTP exception WHEN fetching news THEN catch exception and return empty list")
    void fetchNews_ShouldHandleExceptionsGracefully() 
    {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(NewsAPIResponse.class)))
                .thenThrow(new RuntimeException("Connection Timeout"));

        // Act
        List<Article> result = newsFetchService.fetchNews("finance");

        // Assert
        assertThat(result).isEmpty();
        // Verifying that even with an exception, the code returns an empty list instead of crashing
    }

    @Test
    @DisplayName("GIVEN a null response WHEN fetching news THEN return empty list")
    void fetchNews_ShouldHandleNullResponse() 
    {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(NewsAPIResponse.class))).thenReturn(null);

        // Act
        List<Article> result = newsFetchService.fetchNews("space");

        // Assert
        assertThat(result).isEmpty();
    }
}






