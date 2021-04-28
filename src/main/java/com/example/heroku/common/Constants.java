package com.example.heroku.common;

public class Constants {
    public enum SEARCH_STATUS {
        WIN,
        LOSE,
        OUT_DATE,
        NOT_PUBLISHED;
    }

    public static String[] WIN_PRIZE_NAME = { "Giải Đặc biệt"
        , "Giải Nhất"
        , "Giải Nhì"
        , "Giải Ba"
        , "Giải Tư"
        , "Giải Năm"
        , "Giải Sáu"
        , "Giải Bảy"
        , "Giải Tám"
        , "Chưa xác định"
    };

    public final static String SPLASH = "/";
    public final static String TBL_SCHEDULE = "tblSchedule";
    public final static String TBL_HISTORY = "tblHistory";
    public final static String LONG_DATE_FORMAT = "yyyy-MM-dd";
}
