package com.example.heroku.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsonResultDto {
    private String companyName;

    /** All results of prizes */
    private ResultDto results;

    /** prize won */
    private String winPrizeName;

    /** result won */
    private String winResult;

    /** WIN, LOSE, OUT_DATE, NOT_PUBLISHED */
    private String status;
}
