package com.example.heroku.controller;

import com.example.heroku.service.ScheduledCrawlerService;
import com.rometools.rome.io.FeedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class ScheduleController {
    @Autowired
    private ScheduledCrawlerService scheduledCrawlerService;

    @GetMapping("schedule-crawl")
    public ResponseEntity<String> testScheduledCrawl() throws InterruptedException, ExecutionException, FeedException, ParseException, IOException {
        scheduledCrawlerService.scheduledCrawl();

        return new ResponseEntity<>("Hehe, scheduledCrawl successfully!!!", HttpStatus.OK);
    }
}
