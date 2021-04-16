package com.example.heroku.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class ScheduleDto {
    private List<String> mienBac;
    private List<String> mienNam;
    private List<String> mienTrung;
}
