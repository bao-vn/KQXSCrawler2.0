package com.example.heroku.controller;

import com.example.heroku.service.ScheduledCrawlerService;
import com.rometools.rome.io.FeedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class ScheduleController {
    @Autowired
    private ScheduledCrawlerService scheduledCrawlerService;

    @PostMapping("/schedule/crawl")
    public ResponseEntity<String> testScheduledCrawl() throws InterruptedException, ExecutionException, FeedException, ParseException, IOException {
        scheduledCrawlerService.scheduledCrawl();

        return new ResponseEntity<>("Hehe, scheduleCrawl successfully!!!", HttpStatus.OK);
    }

    @PostMapping("schedule/delete")
    public ResponseEntity<String> testScheduleDelete() throws ExecutionException, InterruptedException {
        scheduledCrawlerService.scheduleDelete();

        return new ResponseEntity<>("Hehe, scheduleDelete successfully!!!", HttpStatus.OK);
    }
}
