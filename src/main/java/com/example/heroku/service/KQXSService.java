package com.example.heroku.service;

import com.example.heroku.common.CommonUtils;
import com.example.heroku.common.Constants;
import com.example.heroku.dto.CrawlerDto;
import com.example.heroku.dto.History;
import com.example.heroku.dto.SearchResultDto;
import com.example.heroku.repository.FireBaseRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class KQXSService {

    @Autowired
    private FireBaseRepository fireBaseRepository;

    @Autowired
    private CommonUtils commonUtils;

    /**
     * Get by no and date
     *
     * @param no String
     * @param strDate LocalDate
     * @return List<SearchResultDto>
     */
    public List<SearchResultDto> getByNoAndDate(String no, String strDate) throws ExecutionException, InterruptedException {
        log.info("getByNoAndDate: no = {} and date = {}", no, strDate);

        Firestore firestore = fireBaseRepository.getFireStore();
        String docPath = "tblHistory/" + strDate;
        DocumentReference docHistory = firestore.document(docPath);
        List<SearchResultDto> results = new ArrayList<>();

        // over 30 days: now > date + 30
        if (commonUtils.isOutDate(strDate)) {
            SearchResultDto outDate = SearchResultDto.builder()
                    .status(Constants.SEARCH_STATUS.OUT_DATE.name())
                    .build();
            results.add(outDate);

            return results;
        }

        // search in DB
        ApiFuture<DocumentSnapshot> future = docHistory.get();
        DocumentSnapshot documentByDate = future.get();
        History history;

        if (documentByDate.exists()) {
            history = documentByDate.toObject(History.class);

            List<String> companies = history.getCompanyName();
            if (!CollectionUtils.isEmpty(companies)) {
                for (String company : history.getCompanyName()) {
                    results.add(this.getByNoAndCompanyAndDate(no, company, strDate));
                }

                return results;
            }
        }

        // NOT_PUBLISHED
        SearchResultDto notPublished = SearchResultDto.builder()
                .status(Constants.SEARCH_STATUS.NOT_PUBLISHED.name())
                .build();
        results.add(notPublished);

        return results;
    }

    /**
     * Get by no and company name
     *
     * @param no String
     * @param company String
     * @return SearchResultDto
     */
    public SearchResultDto getByNoAndCompany(String no, String company) {
        log.info("getByNoAndCompany: no = {}, company = {}", no, company);

        return new SearchResultDto();
    }

    /**
     * Get by no, companyName, date
     *
     * @param no String
     * @param company String
     * @param strDate String
     * @return SearchResultDto
     */
    public SearchResultDto getByNoAndCompanyAndDate(String no, String company, String strDate) throws ExecutionException, InterruptedException {
        log.info("getByNoAndCompanyAndDate: no = {}, company = {}, date = {}", no, company, strDate);

        SearchResultDto resultDto = new SearchResultDto();
        Firestore firestore = fireBaseRepository.getFireStore();
        String docPath = company + '/' + strDate;
        DocumentReference docCompany = firestore.document(docPath);

        ApiFuture<DocumentSnapshot> future = docCompany.get();
        DocumentSnapshot result = future.get();

        if (result.exists()) {
            CrawlerDto resultByCompanyAndDate = result.toObject(CrawlerDto.class);
            resultDto = this.winPrize(resultByCompanyAndDate.getResults(), no);
            resultDto.setCompanyName(company);
        }

        return resultDto;
    }

    /**
     * Get list of winning prize
     *
     * @param results List<String>
     * @param no String
     * @return SearchResultDto
     */
    public SearchResultDto winPrize(List<String> results, String no) {
        log.info("winPrize: results = {}, no = {}", results, no);
        String winPrizeName = "";
        String winResult = "";

        for (int i = 0; i < results.size(); i++) {
            if (StringUtils.hasText(results.get(i)) && results.get(i).contains(no)) {
                winPrizeName = String.valueOf(i);
                winResult = results.get(i);
            }
        }

        return SearchResultDto.builder()
                .results(results)
                .winPrizeName(winPrizeName)
                .winResult(winResult)
                .build();
    }
}
