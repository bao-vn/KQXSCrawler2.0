package com.example.heroku.service;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import com.example.heroku.dto.ScheduleDto;
import com.example.heroku.model.Company;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduledCrawlerService {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private CompanyService companyService;

    @Scheduled(cron = "0 0 16,17,18,19 * * *")
//    @Scheduled(fixedRate = 1000)
    public void scheduledCrawl() throws ExecutionException, InterruptedException {
        log.info("The time is now {}", dateFormat.format(new Date()));

        LocalDate currentDate = LocalDate.now();
        DayOfWeek day = currentDate.getDayOfWeek();
        String dayPath = "CN";

        // Compare with current Day
        if (day.equals(DayOfWeek.MONDAY)) {
        }

        // Get data from tblSchedule
        ScheduleDto scheduleDto = scheduleService.getByDayOfWeek(dayPath);

        // Crawl data from rss link: MienBac, MienNam, MienTrung


        Company company = companyService.getCompanyByName();

        // save to Collection by companyName

        // save to tblHistory
    }
}
