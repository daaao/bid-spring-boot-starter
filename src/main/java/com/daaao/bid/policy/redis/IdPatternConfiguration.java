package com.daaao.bid.policy.redis;

import com.daaao.bid.policy.fallback.DefaultIdFallback;
import com.daaao.bid.policy.fallback.IdFallback;
import org.springframework.util.Assert;

import java.time.format.DateTimeFormatter;

/**
 * @author hao
 */
public class IdPatternConfiguration {
    String key;
    DateTimeFormatter dateTimeFormatter;
    Integer digits;
    Integer increment;
    Long initial;
    boolean needPrefix;
    boolean isResetDaily;
    IdFallback idFallback;

    public IdPatternConfiguration(String key, DateTimeFormatter dateTimeFormatter, Integer digits, Integer increment, Long initial, boolean needPrefix, boolean isResetDaily, IdFallback idFallback) {
        this.key = key;
        this.dateTimeFormatter = dateTimeFormatter;
        this.digits = digits;
        this.increment = increment;
        this.initial = initial;
        this.needPrefix = needPrefix;
        this.isResetDaily=isResetDaily;
        this.idFallback = idFallback;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public void setDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public Integer getDigits() {
        return digits;
    }

    public void setDigits(Integer digits) {
        this.digits = digits;
    }

    public Integer getIncrement() {
        return increment;
    }

    public void setIncrement(Integer increment) {
        this.increment = increment;
    }

    public Long getInitial() {
        return initial;
    }

    public void setInitial(Long initial) {
        this.initial = initial;
    }

    public IdFallback getIdFallback() {
        return idFallback;
    }

    public void setIdFallback(IdFallback idFallback) {
        this.idFallback = idFallback;
    }

    public boolean isNeedPrefix() {
        return needPrefix;
    }

    public void setNeedPrefix(boolean needPrefix) {
        this.needPrefix = needPrefix;
    }

    public boolean isResetDaily() {
        return isResetDaily;
    }

    public void setResetDaily(boolean resetDaily) {
        isResetDaily = resetDaily;
    }

    public static IdPatternConfigurationBuilder builder() {
        return new IdPatternConfigurationBuilder();
    }

    public static class IdPatternConfigurationBuilder {
        String key;
        DateTimeFormatter dateTimeFormatter;
        Integer digits = 0;
        Integer increment = 1;
        Long initial = -1L;
        boolean needPrefix = false;
        boolean isResetDaily = true;
        IdFallback idFallback = new DefaultIdFallback();

        public IdPatternConfigurationBuilder() {
        }

        public IdPatternConfigurationBuilder(String key) {
            this.key = key;
        }

        public IdPatternConfigurationBuilder(String key, DateTimeFormatter dateTimeFormatter, Integer digits) {
            this.key = key;
            this.dateTimeFormatter = dateTimeFormatter;
            this.digits = digits;
        }


        public IdPatternConfigurationBuilder(String key, DateTimeFormatter dateTimeFormatter, Integer digits, Integer increment, Long initial, IdFallback idFallback) {
            this.key = key;
            this.dateTimeFormatter = dateTimeFormatter;
            this.digits = digits;
            this.increment = increment;
            this.initial = initial;
            this.idFallback = idFallback;
        }

        public IdPatternConfigurationBuilder(String key, DateTimeFormatter dateTimeFormatter, Integer digits, Integer increment, Long initial, boolean needPrefix, boolean isResetDaily, IdFallback idFallback) {
            this.key = key;
            this.dateTimeFormatter = dateTimeFormatter;
            this.digits = digits;
            this.increment = increment;
            this.initial = initial;
            this.needPrefix = needPrefix;
            this.isResetDaily = isResetDaily;
            this.idFallback = idFallback;
        }

        public IdPatternConfigurationBuilder key(String key) {
            Assert.notNull(key, "key must not be null!");
            this.key = key;
            return this;
        }

        public IdPatternConfigurationBuilder dateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
            this.dateTimeFormatter = dateTimeFormatter;
            return this;
        }

        public IdPatternConfigurationBuilder digits(Integer digits) {
            this.digits = digits;
            return this;
        }

        public IdPatternConfigurationBuilder increment(Integer increment) {
            this.increment = increment;
            return this;
        }

        public IdPatternConfigurationBuilder initial(Long initial) {
            this.initial = initial;
            return this;
        }

        public IdPatternConfigurationBuilder idFallback(IdFallback idFallback) {
            this.idFallback = idFallback;
            return this;
        }

        public IdPatternConfigurationBuilder needPrefix(boolean needPrefix) {
            this.needPrefix = needPrefix;
            return this;
        }

        public IdPatternConfigurationBuilder isResetDaily(boolean isResetDaily) {
            this.isResetDaily = isResetDaily;
            return this;
        }

        public IdPatternConfiguration build() {
            return new IdPatternConfiguration(this.key, this.dateTimeFormatter, this.digits, this.increment, this.initial, this.needPrefix, this.isResetDaily, this.idFallback);
        }
    }
}
