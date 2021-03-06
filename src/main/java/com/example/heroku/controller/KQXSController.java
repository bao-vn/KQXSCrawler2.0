package com.example.heroku.controller;

import com.example.heroku.common.CommonUtils;
import com.example.heroku.dto.JsonCrawlerDto;
import com.example.heroku.dto.JsonResultDto;
import com.example.heroku.repository.FireBaseRepository;
import com.example.heroku.dto.CrawlerDto;
import com.example.heroku.service.KQXSService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api")
public class KQXSController {
    @Autowired
    private FireBaseRepository fireBaseRepository;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private KQXSService kqxsService;

    @RequestMapping("/kqxs/mien-bac")
    public ResponseEntity<List<CrawlerDto>> parseKQXS2Json() throws IOException, FeedException, ParseException {
        String url = "https://xskt.com.vn/rss-feed/an-giang-xsag.rss";
        URL feedUrl = new URL(url);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader((feedUrl)));

        // parse to json
        List<CrawlerDto> crawlerDtos = new ArrayList<>();

        for (SyndEntry entry : feed.getEntries()) {
            // parse date from link
            // Example: https://xskt.com.vn/xsag/ngay-18-3-2021
            Date date = commonUtils.parseToLocalDateFromLink(entry.getLink());

            CrawlerDto crawlerDto = CrawlerDto.builder()
                    .title(entry.getTitle())
                    .results(commonUtils.string2KQXSDescription(entry.getDescription().getValue()))
                    .link(entry.getLink())
                    .publishedDate(date)
                    .build();

            crawlerDtos.add(crawlerDto);
        }

        return new ResponseEntity<>(crawlerDtos, HttpStatus.OK);
    }

    @RequestMapping("/kqxs/mien-nam")
    public ResponseEntity<List<CrawlerDto>> parseKQXSMienNam() throws IOException, FeedException, ParseException {
        String url = "https://xskt.com.vn/rss-feed/mien-nam-xsmn.rss";
        URL feedUrl = new URL(url);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader((feedUrl)));

        // parse to json
        List<CrawlerDto> crawlerDtos = new ArrayList<>();

        for (SyndEntry entry : feed.getEntries()) {
            // parse date from link
            // Example: https://xskt.com.vn/xsag/ngay-18-3-2021
            Date date = commonUtils.parseToLocalDateFromLink(entry.getLink());

            CrawlerDto crawlerDto = CrawlerDto.builder()
                    .title(entry.getTitle())
                    .results(commonUtils.multipleString2KQXSDescription(entry.getDescription().getValue()).get(0))
                    .link(entry.getLink())
                    .publishedDate(date)
                    .build();

            crawlerDtos.add(crawlerDto);
        }

        return new ResponseEntity<>(crawlerDtos, HttpStatus.OK);
    }

    @RequestMapping("/kqxs/data")
    public ResponseEntity<Map<String, Object>> readData() throws ExecutionException, InterruptedException {
        Firestore fireStore = fireBaseRepository.getFireStore();

        CollectionReference kqxsCrawler = fireStore.collection("KQXSCrawler");
        DocumentReference anGiang = kqxsCrawler.document("AnGiang");
        DocumentReference binhDinh = fireStore.document("KQXSCrawler/BinhDinh");
        // asynchronously retrieve the document
        ApiFuture<DocumentSnapshot> futureAnGiang = anGiang.get();
        ApiFuture<DocumentSnapshot> futureBinhDinh = binhDinh.get();
        // ...
        // future.get() blocks on response
        DocumentSnapshot documentAnGiang = futureAnGiang.get();
        DocumentSnapshot documentBinhDinh = futureBinhDinh.get();

        Map<String, Object> result = new HashMap<>();
        if (documentAnGiang.exists()) {
            result = documentAnGiang.getData();
            log.info("Document data: " + documentAnGiang.getData());
        } else {
            log.info("No such document!");
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/")
    public String hello() {
        return "Hello World\n";
    }

    /**
     * Search by no, companyName, date
     *
     * @param no String
     * @param strCompanyName String
     * @param strDate String
     * @return List<DivideResultDto>
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("/search")
    public ResponseEntity<List<JsonResultDto>> getByNoAndCompanyNameAndDate(@RequestParam(value = "no") String no
            , @RequestParam(value = "companyName", required = false) String strCompanyName
            , @RequestParam(value = "date", required = false) String strDate)
        throws ExecutionException, InterruptedException {
        log.info("Search by no = {} and date = {}", no, strDate);
        List<JsonResultDto> searchResultDtos = kqxsService.searchByNoAndCompanyAndDate(no, strCompanyName, strDate);

        return new ResponseEntity<>(searchResultDtos, HttpStatus.OK);
    }

    /**
     * Get results by companyName and date
     *
     * @param companyName String
     * @param date String
     * @return List<JsonCrawlerDto>
     */
    @GetMapping("company/{companyName}/date/{date}")
    public ResponseEntity<JsonCrawlerDto> getByCompanyNameAndDate(@PathVariable("companyName") String companyName
        , @PathVariable("date") String date) throws ExecutionException, InterruptedException {
        log.info("getByCompanyNameAndDate: companyName = {}, date = {}", companyName, date);

        JsonCrawlerDto jsonCrawlerDtos = kqxsService.getByCompanyNameAndDate(companyName, date);

        return new ResponseEntity<>(jsonCrawlerDtos, HttpStatus.OK);
    }
}