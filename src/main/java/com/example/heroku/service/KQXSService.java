package com.example.heroku.service;

import com.example.heroku.common.CommonUtils;
import com.example.heroku.common.Constants;
import com.example.heroku.dto.CrawlerDto;
import com.example.heroku.dto.JsonResultDto;
import com.example.heroku.dto.History;
import com.example.heroku.dto.SearchResultDto;
import com.example.heroku.mapper.SearchResultMapper;
import com.example.heroku.repository.FireBaseRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    @Autowired
    private CompanyService companyService;

    @Autowired
    private SearchResultMapper searchResultMapper;

    /**
     * Redirect to other service searching based on parameter(no, company, date)
     *
     * @param no String
     * @param company String
     * @param strDate String
     * @return List<SearchResultDto>
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public List<JsonResultDto> searchByNoAndCompanyAndDate(String no, String company, String strDate) throws ExecutionException, InterruptedException {
        // company and date is empty
        if (!commonUtils.isValidNumberOfParameter(new String[]{no, company, strDate}, 2)) {
            return new ArrayList<>();
        }

        if (!StringUtils.hasText(company)) {
            return searchResultMapper.toDivideResultDtoListFromSearchResultDtoList(this.getByNoAndDate(no, strDate));
        } else if (!StringUtils.hasText(strDate)) {
            return searchResultMapper.toDivideResultDtoListFromSearchResultDtoList(this.getByNoAndCompany(no, company));
        } else {
            return Stream.of(searchResultMapper.toDivideResultDtoFromSearchResultDto(this.getByNoAndCompanyAndDate(no, company, strDate)))
                    .filter(item -> StringUtils.hasText(item.getWinPrizeName()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Get by companyName and date (as same as getting data from Document reference)
     *
     * @param companyName String
     * @param strDate String
     * @return CrawlerDto
     */
    public CrawlerDto getByCompanyNameAndDate(String companyName, String strDate) {

    }

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

                return results.stream()
                    .filter(item -> StringUtils.hasText(item.getWinPrizeName()))
                    .collect(Collectors.toList());
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
     * @return List<SearchResultDto>
     */
    public List<SearchResultDto> getByNoAndCompany(String no, String company) throws ExecutionException, InterruptedException {
        log.info("getByNoAndCompany: no = {}, company = {}", no, company);

        List<SearchResultDto> searchResultDtos = new ArrayList<>();

        Firestore firestore = fireBaseRepository.getFireStore();
        CollectionReference collection = firestore.collection(company);

        for (DocumentReference listDocument : collection.listDocuments()) {
            searchResultDtos.add(this.getByDocumentReference(listDocument, no, company));
        }

        return searchResultDtos;
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

        Firestore firestore = fireBaseRepository.getFireStore();
        String docPath = company + '/' + strDate;
        DocumentReference docCompany = firestore.document(docPath);

        return this.getByDocumentReference(docCompany, no, company);
    }

    /**
     * Get by Document Reference from firestore
     *
     * @param docCompany DocumentReference
     * @param no String
     * @param company String
     * @return SearchResultDto
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public SearchResultDto getByDocumentReference(DocumentReference docCompany, String no, String company) throws ExecutionException, InterruptedException {
        SearchResultDto resultDto = new SearchResultDto();
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
            if (StringUtils.hasText(results.get(i))
                && commonUtils.isWinningPrize(no, results.get(i))) {
                winPrizeName = searchResultMapper.winPrizeNameMapper(i);
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
