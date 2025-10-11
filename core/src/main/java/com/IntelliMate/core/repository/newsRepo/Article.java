package com.IntelliMate.core.repository.newsRepo;

import com.fasterxml.jackson.annotation.JsonProperty;




public class Article 
{
    private String title;
    private String description;
    private String url;
    private String urlToImage;
    private String publishedAt;
    
    @JsonProperty("source")
    private Source source;
    
    
    
    // Inner class for source
    public static class Source 
    {
        private String name;
        
        public String getName() 
        {
            return name;
        }
        
        public void setName(String name) 
        {
            this.name = name;
        }
    }
    
    public String getTitle() 
    {
        return title;
    }
    
    public void setTitle(String title) 
    {
        this.title = title;
    }
    
    public String getDescription() 
    {
        return description;
    }
    
    public void setDescription(String description) 
    {
        this.description = description;
    }
    
    public String getUrl() 
    {
        return url;
    }
    
    public void setUrl(String url) 
    {
        this.url = url;
    }
    
    public String getUrlToImage() 
    {
        return urlToImage;
    }
    
    public void setUrlToImage(String urlToImage) 
    {
        this.urlToImage = urlToImage;
    }
    
    public String getPublishedAt() 
    {
        return publishedAt;
    }
    
    public void setPublishedAt(String publishedAt) 
    {
        this.publishedAt = publishedAt;
    }
    
    public Source getSource() 
    {
        return source;
    }
    
    public void setSource(Source source) 
    {
        this.source = source;
    }
}






