package com.daaao.bid.policy.redis;

import java.time.format.DateTimeFormatter;

/**
 * @author hao
 */
public class SimpleDateFormatter {
    public static final String YYYY_MM_DD = "yyyyMMdd";
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyyMMddHHmmss";
    public static final String YYYY_MM_DD_HH_MM_SS_SSS = "yyyyMMddHHmmssSSS";
    public static final DateTimeFormatter FORMATTER_DAY = DateTimeFormatter.ofPattern(YYYY_MM_DD);
    public static final DateTimeFormatter FORMATTER_SECOND = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);
    public static final DateTimeFormatter FORMATTER_MILLISECOND = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS_SSS);
}
