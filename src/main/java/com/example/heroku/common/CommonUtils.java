package com.example.heroku.common;

import com.example.heroku.dto.CrawlerDto;

import java.lang.reflect.Field;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommonUtils {
    /**
     * Convert KQXSDto to Map<String, Object>
     *
     * @param crawlerDto KQXSDto
     * @return Map<String, Object>
     */
    public Map<String, Object> convertToMap(CrawlerDto crawlerDto) {
        // title, link, publishedDate, results[9]
        Map<String, Object> kqxs = new HashMap<>();
        Field[] fields = crawlerDto.getClass().getDeclaredFields();
        kqxs.put(fields[0].getName(), crawlerDto.getTitle());
        kqxs.put(fields[1].getName(), crawlerDto.getLink());
        kqxs.put(fields[2].getName(), crawlerDto.getPublishedDate());

        // result[9]
        List<String> results = crawlerDto.getResults();
        kqxs.put("results", results);

        return kqxs;
    }

    /**
     * Parse to LocalDate from rss link
     * Example: https://xskt.com.vn/xsag/ngay-18-3-2021
     *
     * @param link: String from rss link
     * @return Date
     */
    public Date parseToLocalDateFromLink(String link) throws ParseException {
        String[] strLinks = link.split("/");
        String[] strDates = strLinks[strLinks.length - 1].split("-");
        String strDate = strDates[2] + "/" + strDates[1] + "/" + strDates[3].substring(2);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        LocalDate localDate = LocalDate.parse(strDate, dateTimeFormatter);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        return Date.from(localDate.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    /**
     * Parse to LocalDate from rss link
     * Example: https://xskt.com.vn/xsag/ngay-18-3-2021
     *
     * @param link: String from rss link
     * @return Date
     */
    public String parseToStringDateFromLink(String link) throws ParseException {
        String[] strLinks = link.split("/");
        String[] strDates = strLinks[strLinks.length - 1].split("-");
        String strDate = strDates[2] + "/" + strDates[1] + "/" + strDates[3].substring(2);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        LocalDate localDate = LocalDate.parse(strDate, dateTimeFormatter);

        return localDate.toString();
    }

    /**
     * Parse rss data
     *
     * @param results String
     * @return XoSoKienThiet
     */
    public List<String> string2KQXSDescription(String results) {
        // Reflection Description
        List<String> prizeList = new ArrayList<>();

        // ?????B: 55600???1: 59302???2: 78836 - 71711???3: 57669 - 79931 - 24351 - 86322 - 54511 - 71826???4: 6225 - 6043 - 3742 - 0666???5: 0314 - 6945 - 0521 - 6066 - 8579 - 0910???6: 203 - 330 - 633???7: 04 - 70 - 40 - 37
        String[] split = results.split("\n");
        int indexPrizeName = 0;
        for (String item: split) {
            if (indexPrizeName > 8) {
                break;
            }

            if (!StringUtils.hasText(item)) {
                continue;
            }

            // index of prizeName
            String[] resultWithTitlePrize = item.split(":");

            if (resultWithTitlePrize.length < 2
                    || (!StringUtils.hasText(resultWithTitlePrize[0])
                        && (!StringUtils.hasText(resultWithTitlePrize[1])))
            ) {
                continue;
            }

            String prize = resultWithTitlePrize[1];
            prizeList.add(prize.replaceAll("\\s+",""));

            // GiaiTam
            if (resultWithTitlePrize.length == 3 && indexPrizeName == 7
                    && StringUtils.hasText(resultWithTitlePrize[2])) {
                prizeList.add(resultWithTitlePrize[2].trim());
            }
            indexPrizeName++;
        }

        return prizeList;
    }

    /**
     * Parse rss data in case <description> tag contains multiple results.
     * Example: https://xskt.com.vn/rss-feed/mien-nam-xsmn.rss
     *
     * @param description contains multiple results
     * @return List<XoSoKienThiet> list of prize for each result
     */
    public List<List<String>> multipleString2KQXSDescription(String description) {
        // Reflection Description
        List<List<String>> companyWithPrizeList = new ArrayList<>();

        //  [C???n Th??] ??B: 414303 1: 51374 2: 50151 3: 51102 - 31421 4: 77132 - 16282 - 27680 - 24815 - 84724 - 87059 - 08557 5: 2523 6: 6215 - 4816 - 7933 7: 2228: 06 [?????ng Nai] ??B: 279699 1: 13499 2: 07745 3: 05120 - 77404 4: 05993 - 53444 - 48080 - 89559 - 16888 - 23744 - 12345 5: 7193 6: 7558 - 6461 - 6842 7: 0658: 32 [S??c Tr??ng] ??B: 454847 1: 72330 2: 42590 3: 26544 - 70144 4: 86931 - 79675 - 09519 - 85255 - 58821 - 60418 - 11558 5: 2962 6: 6470 - 6472 - 0714 7: 1278: 47
        String[] splitByCompanyName = description.split("\\[");
        int indexPrizeName = 0;
        for (String item: splitByCompanyName) {
            // item = C???n Th??] ??B: 414303 1: 51374 2: 50151 3: 51102 - 31421 4: 77132 - 16282 - 27680 - 24815 - 84724 - 87059 - 08557 5: 2523 6: 6215 - 4816 - 7933 7: 2228: 06
            if (!StringUtils.hasText(item)) {
                continue;
            }

            // result = ["C???n Th??"," ??B: 414303 1: 51374 2: 50151 3: 51102 - 31421 4: 77132 - 16282 - 27680 - 24815 - 84724 - 87059 - 08557 5: 2523 6: 6215 - 4816 - 7933 7: 2228: 06"]
            String[] parseIntoResult = item.split("\\]");
            if (parseIntoResult.length < 2
                    || (!StringUtils.hasText(parseIntoResult[0])
                    && !StringUtils.hasText(parseIntoResult[1]))
            ) {
                continue;
            }
            String companyName = parseIntoResult[0];
            String result = parseIntoResult[1];

            companyWithPrizeList.add(string2KQXSDescription(result));

            indexPrizeName++;
        }

        return companyWithPrizeList;
    }

    /**
     * Parse company name from title of rss link
     * Convert non ASCII to ASCII
     * 
     * @param title String Unicode
     * @return String
     */
    public String parseCompanyNameFromTitleLink(String title) {
        String name = title.split("RSS feed x??? s??? ")[1];
        name = name.replace(" ", "");
        name = Normalizer.normalize(name, Normalizer.Form.NFD);
        name = name.replaceAll("??", "D");
        name = name.replaceAll("\\P{InBasic_Latin}", "");

        return name;
    }

    /**
     * Is outdate from date?
     *
     * @param strDate String
     * @return boolean
     */
    public boolean isOutDate(String strDate) {
        // over 30 days: now > date + 30
        LocalDate now = LocalDate.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(strDate, dateTimeFormatter);

        return now.isAfter(date.plusDays(30));
    }

    /**
     * Compare with prize's result.
     *
     * @param no String input
     * @param compareResult prize's result
     * @return boolean true: is winning prize
     */
    public boolean isWinningPrize(String no, String compareResult) {
        String subNo = no;
        int endOfNumberComparing = compareResult.split("-")[0].length();

        // Get subString of No to compare with prize's result
        if (subNo.length() >= endOfNumberComparing) {
            subNo = subNo.substring(subNo.length() - endOfNumberComparing);
        } else {
            return false; // subNo doesn't have length enough to compare
        }

        String[] arrCompareResult = compareResult.split("-");
        for (String result : arrCompareResult) {
            if (result.equals(subNo)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is valid number of parameter
     *
     * @param parameters String[]
     * @param iValidNumber int
     * @return boolean
     */
    public boolean isValidNumberOfParameter(String[] parameters, int iValidNumber) {
        int currValid = 0;
        for (String parameter : parameters) {
            if (StringUtils.hasText(parameter)) {
                currValid++;
            }
        }

        return currValid >= iValidNumber;
    }
}
