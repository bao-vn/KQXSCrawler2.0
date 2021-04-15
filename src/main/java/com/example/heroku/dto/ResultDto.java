package com.example.heroku.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultDto {
    private String giaiDacBiet;
    private String giaiNhat;
    private String giaiNhi;
    private List<String> giaiBa;
    private List<String> giaiTu;
    private String giaiNam;
    private List<String> giaiSau;
    private String giaiBay;
    private String giaiTam;
}
