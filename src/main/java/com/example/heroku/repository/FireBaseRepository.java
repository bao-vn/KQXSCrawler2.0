package com.example.heroku.repository;

import com.example.heroku.common.CommonUtils;
import com.example.heroku.dto.CrawlerDto;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutionException;

import com.google.firebase.database.ServerValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Repository
@Slf4j
public class FireBaseRepository {
    private FirebaseDatabase realtimeDB;
    private Firestore firestore;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    private CommonUtils commonUtils;

    /**
     * Constructor
     *
     * @throws IOException
     */
    public FireBaseRepository() throws IOException {
//        File file = ResourceUtils.getFile("classpath:key.json");
//        FileInputStream serviceAccount = new FileInputStream(file);
        String key = "{\n"
            + "  \"type\": \"service_account\",\n"
            + "  \"project_id\": \"kqxs-firestore\",\n"
            + "  \"private_key_id\": \"be6969879505657709ab375a761595d3f39c104b\",\n"
            + "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDBmPNwHXVgHyze\\nJT3cD5Jx2jzOx4dmN57TT0v3fc6N52mtYMpYoHLpuhx9p2CnNPJRIGiM+d61l7wZ\\nR1X/EhIAGo5q1XxE1BEJ6dWHJNQ/mFVySsDPaTs4wW/PpZ+xSkAqLsU8gJOVxZTG\\ny2dPy6yThx4H45wHN9mu2g9R3OhVAKkpxAcBHte0XornKV1yuyri1315VgCFfncO\\npgigmAUjPw+4YWgPdRKQb7Z0Y5hXZ+89cmCrwxW+pWfp+yhTN6I/qC9uZD4jqDgs\\nh2LDJQAv7TkSXu+yWn4mTC/+/DMpu6BbTiRb61n+Dc8KANlz8rC6PZBOMSPIaNaY\\n/32TZfH3AgMBAAECggEAVTsVnZvSg31mHuzNZZcTdX1LfQuZ4BYHnecQvkZMS+v/\\n+d0daFRHEwAlL+qi4iosriuy67HL6y2Ama5uvUuC0jnezkjrm0+zqqYgJA3CRbc2\\nkwk3BnlBIdUDeSrk+cQDKK8LlX3Tu8HPJJADDT3RXefEAFwj4oejldeZnARB4r2Q\\ng53qCcSEOSMkUYJu6GNJTWoeBXppOMo36XQk8+KH7IiTmMYK8LH7/7yTxc/ZOEfU\\nu6USCttet4ghR1AgTmGwga0kLOLKSDkvWD3Bt/hQRpJtJfUQMYKD3j0T88+kERmB\\nF4noa3FXFbklAA5YE5oiWeFlqcNba99GBy9wuL0YTQKBgQD97OF4riNtvhbCnDta\\nY3VGIhH30lkifhR1sIBZaI+tAI2NNRGuym8et5cz6oQNASfDbyCnIQIf/tjNAqE+\\nks0Au2Jrne2bv8mGHtrZbpff8emQ9OhQ5SopHZshicWfp1xoMRvMc8s4sGCnFppd\\nuka8wObvow1rVwopHvFzZTIeDQKBgQDDLeLkSSw9lFBCuvoBEJxvs88q026Jzq/Z\\nfOG875LfCSPVZhMYNJ9pzIi/ofPjnolF9oFo1iDUOFhleifTV/C6zPlnN+pGfsth\\nB0fAa0B3y3YYUy1JNvRCsjyC/Rfy3LjgYLgwS7uUbkbzA67iK40V0+OOlsSBeEw1\\nwM+CmE3TEwKBgQDLWtrTL/qVl8dYgZAS+lXgHcbwBlh2BCzUd87CS7fMoAW80YRg\\nzCQjoHoKDMVGOOekXynyTsAJ6amsvW9u0lg1Ptw6PVceBYEZ1ToFKcmdgnpAKbKn\\nnm+eT+R9/nzzODAMDVAq840L0E0HY1WqvFSMM5x+y8EidqlfdzV86c0yPQKBgC63\\nHuVQ9jvO8d7m7cuvdCHwQZeYf46QX9qIX2dfWCH2CBOdmNhkT/t8rwZe9wb1/Rk3\\nxQWYqsbgrZesQzf8hmvPf6pK4dH5qygQHAZcJVQG6L8tuVsawh/slTss5LyGQ+HA\\nhJVaqwz90BF+Qi90CRK+YmrQfzwV1PfQrFovpgDtAoGBALB9ezExJyj5FWgOw29J\\nmvUe4eeE1i1cTxI3yzJDxUqXudi0lCGP1f9U6SNtvwAVC/FkgsWC+FB4Sz+ex+8G\\nQhR8LZcMHE+KnzcaLbrJ++uGZ8JJuDXYSp10bRGxJN4IGxOCuKlIVZecMEDJbAIM\\nHhjdLthvSxOpgdIgvnIvt1OQ\\n-----END PRIVATE KEY-----\\n\",\n"
            + "  \"client_email\": \"firebase-adminsdk-x4bbg@kqxs-firestore.iam.gserviceaccount.com\",\n"
            + "  \"client_id\": \"105020106653906025224\",\n"
            + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
            + "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n"
            + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
            + "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-x4bbg%40kqxs-firestore.iam.gserviceaccount.com\"\n"
            + "}";
        InputStream stream = new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8));

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .setDatabaseUrl("https://kqxs-firestore.firebaseio.com/")
                .build();

        FirebaseApp.initializeApp(options);

        realtimeDB = FirebaseDatabase.getInstance();
        firestore = FirestoreClient.getFirestore();
    }

    public FirebaseDatabase getRealtimeDB() {
        return realtimeDB;
    }

    public Firestore getFireStore() {
        return firestore;
    }

    /**
     * Add document
     *
     * @param pathDocument String document ID: "collection/document"
     * @param crawlerDtos KQXSDto data
     */
    public void saveResults(String pathDocument, CrawlerDto crawlerDtos) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = this.firestore.document(pathDocument);
        ApiFuture<WriteResult> initial = documentReference.set(crawlerDtos);
        initial.get();

        // updatedTime
        Map<String, Object> updatedTime = new HashMap<>();
        updatedTime.put("updatedTime", FieldValue.serverTimestamp());
        documentReference.set(updatedTime, SetOptions.merge());
    }

    /**
     * Save multiple of Results using batched writes
     *
     * @param crawlerDtoMap Map<String, CrawlerDto> contain(pathDocument, CrawlerDto)
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void saveMultipleResults(Map<String, CrawlerDto> crawlerDtoMap) throws ExecutionException, InterruptedException {
        WriteBatch batch = firestore.batch();

        // set data for each document path
        crawlerDtoMap.forEach((pathDocument, crawlerDto) -> {
            DocumentReference reference = firestore.document(pathDocument);
            batch.set(reference, crawlerDto);
            batch.update(reference, "updatedTime", FieldValue.serverTimestamp());
        });

        // asynchronously commit the batch
        ApiFuture<List<WriteResult>> future = batch.commit();
        // future.get() blocks on batch commit operation
        List<WriteResult> writeResultApiFuture = future.get();
        log.info("Size of batch: {}", writeResultApiFuture.size());
        for (WriteResult result : writeResultApiFuture) {
            log.info("Updated time : " + result.getUpdateTime());
        }
    }

    /**
     * Check existed Document Path
     *
     * @param documentPath String
     * @return boolean
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public boolean isExistedDocument(String documentPath) throws ExecutionException, InterruptedException {
        Firestore firestore = getFireStore();

        DocumentReference reference = firestore.document(documentPath);
        ApiFuture<DocumentSnapshot> future = reference.get();
        DocumentSnapshot snapshot = future.get();

        return snapshot.exists();
    }
}
