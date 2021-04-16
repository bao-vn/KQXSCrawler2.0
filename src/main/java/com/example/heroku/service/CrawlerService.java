package com.example.heroku.service;

import com.example.heroku.common.CommonUtils;
import com.example.heroku.dto.CrawlerDto;
import com.example.heroku.model.Company;
import com.example.heroku.repository.FireBaseRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;

/**
 * Crawl from rss link
 */
@Service
@Slf4j
public class CrawlerService {
    @Autowired
    private FireBaseRepository fireBaseRepository;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private CompanyService companyService;

    private final String URL = "https://xskt.com.vn/rss/";

    /**
     * Parse data from SyndEntry of rss link
     *
     * @param entry SyndEntry
     * @return CrawlerDto
     * @throws ParseException
     */
    public CrawlerDto parseDataFromSyndEntry(SyndEntry entry) throws ParseException {
        log.info("parseDataFromSyndEntry: entry = {}", entry);

        String resultsData = entry.getDescription().getValue();
        List<String> results;
        if (resultsData.contains("\\[")) {
            // TODO: Get list of results from KQXS Mien Nam
            log.info("TODO: Get list of results from KQXS Mien Nam");
            results = commonUtils.multipleString2KQXSDescription(resultsData).get(0);
        } else {
            results = commonUtils.string2KQXSDescription(resultsData);
        }

        // parse date from link
        // Example: https://xskt.com.vn/xsag/ngay-18-3-2021
        Date date = commonUtils.parseToLocalDateFromLink(entry.getLink());
        String strDate = commonUtils.parseToStringDateFromLink(entry.getLink());

        return CrawlerDto.builder()
            .title(entry.getTitle())
            .results(results)
            .link(entry.getLink())
            .publishedDate(date)
            .strPublishedDate(strDate)
            .build();
    }

    /**
     * Get the first KQXS from rss link
     *
     * @param url String link rss
     * @return CrawlerDto
     */
    public CrawlerDto getTheFirstKQXSFromRssLink(String url) throws IOException, FeedException, ParseException {
        log.info("getTheFirstKQXSFromRssLink: url = {}", url);

        URL feedUrl = new URL(url);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader((feedUrl)));

        return this.parseDataFromSyndEntry(feed.getEntries().get(0));
    }

    /**
     * Get KQXS from rss link
     *
     * @param url path get from DB
     * @return List<KQXSDto> group by companyID
     * @throws IOException URL, XmlReader Exception
     * @throws FeedException
     * @throws ParseException
     */
    public List<CrawlerDto> getKQXSFromRssLink(String url) throws IOException, FeedException, ParseException {
        log.info("getKQXSFromRssLink: url = {}", url);

        URL feedUrl = new URL(url);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader((feedUrl)));

        // parse to json
        List<CrawlerDto> crawlerDtos = new ArrayList<>();

        for (SyndEntry entry : feed.getEntries()) {
            CrawlerDto crawlerDto = this.parseDataFromSyndEntry(entry);
            crawlerDtos.add(crawlerDto);
        }

        return crawlerDtos;
    }

    /**
     * Save results in Collection
     *
     * @param company company info
     * @throws IOException
     * @throws FeedException
     * @throws ParseException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void save(Company company) throws IOException, FeedException, ParseException, ExecutionException, InterruptedException {
        log.info("save: company = {}", company);

        // format pathDocument = "tblBinhDinh/<yyyy-MM-dd>"
        List<CrawlerDto> crawlerDtos = this.getKQXSFromRssLink(company.getLink());

        for (CrawlerDto crawlerDto : crawlerDtos) {
            String pathDocument = company.getCompanyName()
                + '/'
                + crawlerDto.getStrPublishedDate();
            fireBaseRepository.saveResults(pathDocument, crawlerDto);
        }
    }

    /**
     * Crawl data from rss link in Companies table
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws ParseException
     * @throws IOException
     * @throws FeedException
     */
    public void crawlDataFromRssLink() throws ExecutionException, InterruptedException, ParseException, IOException, FeedException {
        log.info("crawlDataFromRssLink");

        List<Company> companies = companyService.getCompanies();

        for (Company company : companies) {
            this.save(company);
        }
    }

    /**
     * Crawl data from rss links
     *
     * @return List<String>
     * @throws IOException connect to html by Jsoup
     */
    public List<Company> crawlRssLinks() throws IOException {
        log.info("crawlRssLinks()");

        Proxy proxy = new Proxy(Proxy.Type.HTTP,
            new InetSocketAddress("127.0.0.1", 1080));
        Connection connection = Jsoup.connect(URL)
//            .proxy(proxy)
            .userAgent("Mozilla")
            .timeout(5000)
            .cookie("cookiename", "val234")
            .cookie("cookiename", "val234")
            .referrer("http://google.com")
            .header("headersecurity", "xyz123");

        Document docCustomConn = connection.get();

        // get with element id = "ulrss"
        Elements elements = docCustomConn.select("#ulrss > li > a");

        List<Company> rssLinks = new ArrayList<>();
        String host = "https://xskt.com.vn/";
        for (Element element : elements) {
            String link = host + element.attr("href");
            String companyName = commonUtils.parseCompanyNameFromTitleLink(element.text());
            rssLinks.add(Company.builder()
                .companyName(companyName)
                .link(link)
                .build());
        }

        return rssLinks;
    }
}
