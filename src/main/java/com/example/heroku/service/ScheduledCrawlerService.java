package com.example.heroku.service;

import com.example.heroku.common.Constants;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.example.heroku.dto.CrawlerDto;
import com.example.heroku.dto.History;
import com.example.heroku.dto.ScheduleDto;
import com.example.heroku.model.Company;
import com.example.heroku.repository.FireBaseRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.rometools.rome.io.FeedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduledCrawlerService {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private FireBaseRepository fireBaseRepository;

    @Autowired
    private HistoryService historyService;

    /**
     * Schedule crawler day by day (16h00, 18h00)
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws ParseException
     * @throws IOException
     * @throws FeedException
     */
//    @Scheduled(fixedRate = 1000)
    @Scheduled(cron = "0 0 16,17,18,19 * * *")
    public void scheduledCrawl() throws ExecutionException, InterruptedException, ParseException, IOException, FeedException {
        log.info("Invoke method scheduledCrawl(). The time is now {}", dateFormat.format(new Date()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate currentDate = LocalDate.now();
        String strCurrentDate = currentDate.format(formatter);
        DayOfWeek day = currentDate.getDayOfWeek();
        String dayPath;

        // Compare with current Day
        switch (day) {
            case SUNDAY: dayPath = "CN";
                break;
            case MONDAY: dayPath = "T.2";
                break;
            case TUESDAY: dayPath = "T.3";
                break;
            case WEDNESDAY: dayPath = "T.4";
                break;
            case THURSDAY: dayPath = "T.5";
                break;
            case FRIDAY: dayPath = "T.6";
                break;
            case SATURDAY: dayPath = "T.7";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + day);
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

    /**
     * Run once a day
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleDelete() throws ExecutionException, InterruptedException {
        log.info("Invoke method scheduleDelete(). The time is now {}", dateFormat.format(new Date()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate currentDate = LocalDate.now();
        String strDeletedDate = currentDate.plusDays(-30).format(formatter);

        // Delete unused data by order:
        // 1. delete in Collection <Company>/<date>
        this.deleteCollectionNamedCompanyName(strDeletedDate);

        // 2. delete in tblHistory
        this.deleteHistory(strDeletedDate);
    }

    /**
     * Delete Document in collection tblHistory
     *
     * @param strDate String date to compare
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void deleteHistory(String strDate) throws ExecutionException, InterruptedException {
        log.info("deleteHistory: date = {}", strDate);

        Firestore firestore = fireBaseRepository.getFireStore();
        WriteBatch batch = firestore.batch();

        ApiFuture<QuerySnapshot> future = firestore.collection(Constants.TBL_HISTORY).whereLessThan("date", strDate).get();
        List<QueryDocumentSnapshot> documentSnapshots = future.get().getDocuments();

        for (DocumentSnapshot documentSnapshot : documentSnapshots) {
            batch.delete(documentSnapshot.getReference());
        }

        // asynchronously commit the batch
        ApiFuture<List<WriteResult>> futureBatch = batch.commit();
        // future.get() blocks on batch commit operation
        List<WriteResult> writeResultApiFuture = futureBatch.get();
        log.info("Size of batch: {}", writeResultApiFuture.size());
        for (WriteResult result : writeResultApiFuture) {
            log.info("Updated time : " + result.getUpdateTime());
        }
    }

    /**
     * Delete in Collection <Company>/<date> based on tblHistory
     *
     * @param strDate String date to compare
     */
    public void deleteCollectionNamedCompanyName(String strDate) throws ExecutionException, InterruptedException {
        log.info("deleteCollectionNamedCompanyName: date ={}", strDate);

        Firestore firestore = fireBaseRepository.getFireStore();
        WriteBatch batch = firestore.batch();

        ApiFuture<QuerySnapshot> future = firestore.collection(Constants.TBL_HISTORY).whereLessThan("date", strDate).get();
        List<QueryDocumentSnapshot> documentSnapshots = future.get().getDocuments();
        Map<String, History> historyMap = new HashMap<>();          // Map contains <date>/List<CompanyName>

        for (DocumentSnapshot documentSnapshot : documentSnapshots) {
            DocumentReference reference = documentSnapshot.getReference();
            historyMap.put(reference.getPath().split(Constants.SPLASH)[1], documentSnapshot.toObject(History.class));
        }

        // Delete list of company by day in tblHistory
        historyMap.forEach((date, history) -> {
            List<String> deletedCompany = history.getCompanyName();     // list of company
            deletedCompany.forEach(companyName -> {
                DocumentReference reference = firestore.document(companyName + Constants.SPLASH + date);
                batch.delete(reference);                                // delete collection named companyName
            });
        });

        // asynchronously commit the batch
        ApiFuture<List<WriteResult>> futureBatch = batch.commit();
        // future.get() blocks on batch commit operation
        List<WriteResult> writeResultApiFuture = futureBatch.get();
        log.info("Size of batch: {}", writeResultApiFuture.size());
        for (WriteResult result : writeResultApiFuture) {
            log.info("Updated time : " + result.getUpdateTime());
        }
    }

    /**
     * Get schedule for each region in tblSchedule
     *
     * @param dayPath String
     * @return ScheduleDto
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public ScheduleDto getByDayOfWeek(String dayPath) throws ExecutionException, InterruptedException {
        log.info("getByDayOfWeek: dayPath = {}", dayPath);

        Firestore firestore = fireBaseRepository.getFireStore();
        DocumentReference documentReference = firestore.document(Constants.TBL_SCHEDULE + Constants.SPLASH + dayPath);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot snapshot = future.get();

        if (snapshot.exists()) {
            return snapshot.toObject(ScheduleDto.class);
        }

        return new ScheduleDto();
    }
}
