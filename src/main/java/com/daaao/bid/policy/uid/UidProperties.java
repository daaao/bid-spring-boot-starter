package com.daaao.bid.policy.uid;

import com.daaao.bid.property.BidProperty;
import com.daaao.bid.policy.uid.utils.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

@ConfigurationProperties(prefix = "spring.bid.uid")
@ConditionalOnProperty(name = BidProperty.BID_UID_ENABLE, havingValue = "true")
public class UidProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(UidProperties.class);

    public UidProperties() {
        LOGGER.info("UidProperties initializing...");
    }

    private boolean enable = false;

    /**
     * 时间增量值占用位数。当前时间相对于时间基点的增量值，单位为秒
     */
    private int timeBits = 30;

    /**
     * 工作机器ID占用的位数
     */
    private int workerBits = 24;

    /**
     * 序列号占用的位数
     */
    private int seqBits = 9;

    /**
     * 时间基点. 例如 2019-02-20 (毫秒: 1550592000000)
     */
    private String epochStr = "2021-02-08";

    /**
     * 时间基点对应的毫秒数
     */
    private long epochSeconds = TimeUnit.MILLISECONDS.toSeconds(1612713600000L);

    /**
     * 是否容忍时钟回拨, 默认:true
     */
    private boolean enableBackward = true;

    /**
     * 时钟回拨最长容忍时间（秒）
     */
    private long maxBackwardSeconds = 1L;

    public int getTimeBits() {
        return timeBits;
    }

    public void setTimeBits(int timeBits) {
        if (timeBits > 0) {
            this.timeBits = timeBits;
        }
    }

    public int getWorkerBits() {
        return workerBits;
    }

    public void setWorkerBits(int workerBits) {
        if (workerBits > 0) {
            this.workerBits = workerBits;
        }
    }

    public int getSeqBits() {
        return seqBits;
    }

    public void setSeqBits(int seqBits) {
        if (seqBits > 0) {
            this.seqBits = seqBits;
        }
    }

    public String getEpochStr() {
        return epochStr;
    }

    public void setEpochStr(String epochStr) {
        if (StringUtils.isNotBlank(epochStr)) {
            this.epochStr = epochStr;
            this.epochSeconds = TimeUnit.MILLISECONDS.toSeconds(DateUtils.parseByDayPattern(epochStr).getTime());
        }
    }

    public long getEpochSeconds() {
        return epochSeconds;
    }

    public boolean isEnableBackward() {
        return enableBackward;
    }

    public void setEnableBackward(boolean enableBackward) {
        this.enableBackward = enableBackward;
    }

    public long getMaxBackwardSeconds() {
        return maxBackwardSeconds;
    }

    public void setMaxBackwardSeconds(long maxBackwardSeconds) {
        this.maxBackwardSeconds = maxBackwardSeconds;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
