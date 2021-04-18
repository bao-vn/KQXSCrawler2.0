package com.example.heroku.service;

import com.example.heroku.dto.History;
import com.example.heroku.repository.FireBaseRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Save results by date
 */
@Service
@Slf4j
public class HistoryService {
    private final static String TBL_HISTORY = "tblHistory";

    @Autowired
    private FireBaseRepository fireBaseRepository;

    @Autowired
    private CompanyService companyService;

    /**
     * Get list of documentID
     *
     * @param collectionPath String
     * @return List<String>
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public List<String> getDocumentPaths(String collectionPath) throws ExecutionException, InterruptedException {
        log.info("getDocumentPaths: collectionPath = {}", collectionPath);

        Firestore firestore = fireBaseRepository.getFireStore();
        List<String> documentPaths = new ArrayList<>();
        CollectionReference collectionReference = firestore.collection(collectionPath);

        //asynchronously retrieve all documents
        ApiFuture<QuerySnapshot> future = collectionReference.get();

        // future.get() blocks on response
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        for (QueryDocumentSnapshot document : documents) {
            documentPaths.add(document.getId());
        }

        return documentPaths;
    }

    /**
     * Store all result collections by date when initiate database
     */
    public void syncHistoryByCollectionID(String collectionPath) throws ExecutionException, InterruptedException {
        log.info("syncHistoryByCollectionID: collectionPath = {}", collectionPath);

        List<String> docPathsByCollectionID = this.getDocumentPaths(collectionPath);
        for (String doc : docPathsByCollectionID) {
            this.saveHistoryDayByDay(doc, collectionPath);
        }
    }

    public void syncHistoryAllDB() throws ExecutionException, InterruptedException {
        log.info("syncHistoryAllDB()");

        // Get all companies
        List<String> companiesName = companyService.getCompanyPaths();

        for (String company : companiesName) {
            this.syncHistoryByCollectionID(company);
        }
    }

    /**
     * Store all result collections day by day
     */
    public void saveHistoryDayByDay(String strDate, String companyName) throws ExecutionException, InterruptedException {
        log.info("saveHistoryDayByDay: strDate = {}, companyName = {}", strDate, companyName);

        Firestore firestore = fireBaseRepository.getFireStore();
        CollectionReference tblHistory = firestore.collection(TBL_HISTORY);
        DocumentReference document = tblHistory.document(strDate);

        // set data
        Map<String, Object> history = new HashMap<>();

        history.put("date", strDate);
        history.put("updatedTime", FieldValue.serverTimestamp());
        ApiFuture<WriteResult> initialResult = tblHistory.document(strDate).set(history, SetOptions.merge());
        initialResult.get();

        // Atomically add a new region to the "regions" array field.
        ApiFuture<WriteResult> arrayUnion = document.update("companyName",
            FieldValue.arrayUnion(companyName));
        arrayUnion.get();
    }

    public void saveMultipleHistoryDayByDay(List<History> histories, String date) throws ExecutionException, InterruptedException {
        log.info("saveMultipleHistoryDayByDay");

        Firestore firestore = fireBaseRepository.getFireStore();
        WriteBatch batch = firestore.batch();

        String docPath = TBL_HISTORY + "/" + date;
        DocumentReference reference = firestore.document(docPath);
        if (!fireBaseRepository.isExistedDocument(docPath)) {
            batch.set(reference, new History());
        }
        for (History history : histories) {
            batch.update(reference, "date", history.getDate());
            batch.update(reference, "updatedTime", FieldValue.serverTimestamp());
            batch.update(reference, "companyName", FieldValue.arrayUnion(history.getUpdatedCompanyName()));

            // delete unused fields
            batch.update(reference, "strDate", FieldValue.delete());
            batch.update(reference, "updatedCompanyName", FieldValue.delete());
        }

        // asynchronously commit the batch
        ApiFuture<List<WriteResult>> future = batch.commit();
        // future.get() blocks on batch commit operation
        List<WriteResult> writeResultApiFuture = future.get();
        log.info("Size of batch: {}", writeResultApiFuture.size());
        for (WriteResult result : writeResultApiFuture) {
            log.info("Updated time : " + result.getUpdateTime());
        }
    }
}
