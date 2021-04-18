package com.example.heroku.service;

import com.example.heroku.dto.CrawlerDto;
import com.example.heroku.dto.History;
import com.example.heroku.dto.ScheduleDto;
import com.example.heroku.model.Company;
import com.example.heroku.repository.FireBaseRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.rometools.rome.io.FeedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScheduleService {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private final static String TBL_SCHEDULE = "tblSchedule";

    @Autowired
    FireBaseRepository fireBaseRepository;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private HistoryService historyService;

    /**
     * Get schedule for each region in tblSchedule
     *
     * @param dayPath String
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public ScheduleDto getByDayOfWeek(String dayPath) throws ExecutionException, InterruptedException {
        Firestore firestore = fireBaseRepository.getFireStore();
        DocumentReference documentReference = firestore.document(TBL_SCHEDULE + "/" + dayPath);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot snapshot = future.get();

        if (snapshot.exists()) {
            return snapshot.toObject(ScheduleDto.class);
        }

        return new ScheduleDto();
    }

    public void testScheduledCrawl() throws ExecutionException, InterruptedException, ParseException, IOException, FeedException {
        log.info("The time is now {}", dateFormat.format(new Date()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate currentDate = LocalDate.now();
        String strCurrentDate = currentDate.format(formatter);
        DayOfWeek day = currentDate.getDayOfWeek();
        String dayPath = "CN";

        // Compare with current Day
        if (day.equals(DayOfWeek.MONDAY)) {
        }

        // Get data from tblSchedule
        ScheduleDto scheduleDto = this.getByDayOfWeek(dayPath);

        // Crawl data from rss link: MienBac, MienNam, MienTrung
        List<Company> companies = new ArrayList<>();
        List<History> histories = new ArrayList<>();
        for (String companyName : scheduleDto.getMienBac()) {
            companies.add(companyService.getCompanyByName(companyName));
            histories.add(History.builder()
                    .updatedCompanyName(companyName)
                    .date(strCurrentDate)
                    .build()
            );
        }

        for (String companyName : scheduleDto.getMienNam()) {
            companies.add(companyService.getCompanyByName(companyName));
            histories.add(History.builder()
                    .updatedCompanyName(companyName)
                    .date(strCurrentDate)
                    .build()
            );
        }

        for (String companyName : scheduleDto.getMienTrung()) {
            companies.add(companyService.getCompanyByName(companyName));
            histories.add(History.builder()
                    .updatedCompanyName(companyName)
                    .date(strCurrentDate)
                    .build()
            );
        }

        // save to Collection: <companyName>/<date>
        Map<String, CrawlerDto> crawlerDtoMap = crawlerService.updateCrawlerDtoMap(companies, new HashMap<>());
        fireBaseRepository.saveMultipleResults(crawlerDtoMap);

        // save to tblHistory/<date>
        historyService.saveMultipleHistoryDayByDay(histories, strCurrentDate);
    }
}
