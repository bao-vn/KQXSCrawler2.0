package com.example.heroku.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Error {
    private String title;
    private String status;
    private String detail;
}
