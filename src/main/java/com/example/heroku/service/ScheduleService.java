package com.example.heroku.service;

import com.example.heroku.dto.ScheduleDto;
import com.example.heroku.repository.FireBaseRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class ScheduleService {
    @Autowired
    FireBaseRepository fireBaseRepository;

    public ScheduleDto getByDayOfWeek(String dayPath) throws ExecutionException, InterruptedException {
        Firestore firestore = fireBaseRepository.getFireStore();
        DocumentReference documentReference = firestore.document(dayPath);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot snapshot = future.get();

        if (snapshot.exists()) {
            return snapshot.toObject(ScheduleDto.class);
        }

        return new ScheduleDto();
    }
}
