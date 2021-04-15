package com.example.heroku.mapper;

import com.example.heroku.common.Constants;
import com.example.heroku.dto.DivideResultDto;
import com.example.heroku.dto.ResultDto;
import com.example.heroku.dto.SearchResultDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SearchResultMapper {

    /**
     * Divide for each result to single field ResultDto
     *
     * @param results List<String>
     * @return ResultDto
     */
    public ResultDto toResultDtoFromList(List<String> results) {
        return ResultDto.builder()
            .giaiDacBiet(results.get(0))
            .giaiNhat(results.get(1))
            .giaiNhi(results.get(2))
            .giaiBa(Arrays.asList(results.get(3).split("-").clone()))
            .giaiTu(Arrays.asList(results.get(4).split("-").clone()))
            .giaiNam(results.get(5))
            .giaiSau(Arrays.asList(results.get(6).split("-").clone()))
            .giaiBay(results.get(7))
            .giaiTam(results.get(8))
            .build();
    }

    /**
     * To DivideResultDto from SearchResultDto
     *
     * @param searchResultDto SearchResultDto
     * @return DivideResultDto
     */
    public DivideResultDto toDivideResultDtoFromSearchResultDto(SearchResultDto searchResultDto) {
        return DivideResultDto.builder()
            .companyName(searchResultDto.getCompanyName())
            .results(this.toResultDtoFromList(searchResultDto.getResults()))
            .winPrizeName(searchResultDto.getWinPrizeName())
            .winResult(searchResultDto.getWinResult())
            .status(searchResultDto.getStatus())
            .build();
    }

    /**
     * To list of DivideResultDto from list of SearchResultDto
     *
     * @param searchResultDtos List<SearchResultDto>
     * @return List<DivideResultDto>
     */
    public List<DivideResultDto> toDivideResultDtoListFromSearchResultDtoList(List<SearchResultDto> searchResultDtos) {
        return searchResultDtos.stream().map(this::toDivideResultDtoFromSearchResultDto).collect(Collectors.toList());
    }

    /**
     * Get win prize name
     *
     * @param index int
     * @return String
     */
    public String winPrizeNameMapper(int index) {
        if (index > Constants.WIN_PRIZE_NAME.length - 1) {
            index = Constants.WIN_PRIZE_NAME.length - 1;
        }

        return Constants.WIN_PRIZE_NAME[index];
    }
}
