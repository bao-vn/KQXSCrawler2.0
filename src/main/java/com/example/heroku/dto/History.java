package com.example.heroku.dto;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class History {
    /** */
    private List<String> companyName;

    /** */
    private String strDate;

    /** publishedDate */
    private String date;

    private Timestamp updatedTime;
}
