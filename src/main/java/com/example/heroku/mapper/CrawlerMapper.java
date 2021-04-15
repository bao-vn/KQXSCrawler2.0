package com.example.heroku.mapper;

import com.example.heroku.dto.CrawlerDto;
import com.example.heroku.dto.JsonCrawlerDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CrawlerMapper {
    @Autowired
    private SearchResultMapper searchResultMapper;

    /**
     * To JsonCrawlerDto from CrawlerDto
     *
     * @param crawlerDto CrawlerDto
     * @return JsonCrawlerDto
     */
    public JsonCrawlerDto toJsonCrawlerDto(CrawlerDto crawlerDto) {
        return JsonCrawlerDto.builder()
            .title(crawlerDto.getTitle())
            .link(crawlerDto.getLink())
            .publishedDate(crawlerDto.getPublishedDate())
            .results(searchResultMapper.toResultDtoFromList(crawlerDto.getResults()))
            .updatedTime(crawlerDto.getUpdatedTime())
            .build();
    }
}
