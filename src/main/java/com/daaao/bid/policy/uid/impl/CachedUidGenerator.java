/*
 * Copyright (c) 2017 Baidu, Inc. All Rights Reserve.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.daaao.bid.policy.uid.impl;

import com.daaao.bid.policy.uid.BitsAllocator;
import com.daaao.bid.policy.uid.UidGenerator;
import com.daaao.bid.policy.uid.UidProperties;
import com.daaao.bid.policy.uid.exception.UidGenerateException;
import com.daaao.bid.property.BidProperty;
import com.daaao.bid.policy.uid.buffer.BufferPaddingExecutor;
import com.daaao.bid.policy.uid.buffer.RejectedPutBufferHandler;
import com.daaao.bid.policy.uid.buffer.RejectedTakeBufferHandler;
import com.daaao.bid.policy.uid.buffer.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cached implementation of {@link UidGenerator} extends
 * from {@link DefaultUidGenerator}, based on a lock free {@link RingBuffer}<p>
 * <p>
 * The spring properties you can specified as below:<br>
 * <li><b>boostPower:</b> RingBuffer size boost for a power of 2, Sample: boostPower is 3, it means the buffer size
 * will be <code>({@link BitsAllocator#getMaxSequence()} + 1) &lt;&lt;
 * {@link #boostPower}</code>, Default as {@value #DEFAULT_BOOST_POWER}
 * <li><b>paddingFactor:</b> Represents a percent value of (0 - 100). When the count of rest available UIDs reach the
 * threshold, it will trigger padding buffer. Default as{@link RingBuffer#DEFAULT_PADDING_PERCENT}
 * Sample: paddingFactor=20, bufferSize=1000 -> threshold=1000 * 20 /100, padding buffer will be triggered when tail-cursor<threshold
 * <li><b>scheduleInterval:</b> Padding buffer in a schedule, specify padding buffer interval, Unit as second
 * <li><b>rejectedPutBufferHandler:</b> Policy for rejected put buffer. Default as discard put request, just do logging
 * <li><b>rejectedTakeBufferHandler:</b> Policy for rejected take buffer. Default as throwing up an exception
 *
 * @author yutianbao
 */
//@Component
//@ConfigurationProperties(prefix = "uid.cached-uid-generator")
public class CachedUidGenerator extends DefaultUidGenerator implements DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedUidGenerator.class);
    private static final int DEFAULT_BOOST_POWER = 3;

    @Autowired
    private Environment environment;

    // --------------------- ???????????? begin ---------------------
    /**
     * RingBuffer size????????????, ?????????UID??????????????????.
     * ??????:3??? ???bufferSize=8192, ?????????bufferSize= 8192 << 3 = 65536
     */
    private int boostPower = DEFAULT_BOOST_POWER;
    /**
     * ???????????????RingBuffer?????????UID, ??????????????????(0, 100), ?????????50
     * ??????: bufferSize=1024, paddingFactor=50 -> threshold=1024 * 50 / 100 = 512.
     * ???????????????UID?????? < 512???, ????????????RingBuffer??????????????????
     */
    private int paddingFactor = RingBuffer.DEFAULT_PADDING_PERCENT;
    /**
     * ????????????RingBuffer????????????, ???Schedule?????????, ?????????????????????
     * ??????:???????????????, ????????????Schedule??????. ????????????, ?????????Schedule??????????????????, ??????:???
     */
    private Long scheduleInterval;
    // --------------------- ???????????? end -----------------------

    /** ????????????: ????????????, ?????????????????????
     ??????????????????, ?????????Put??????, ???????????????. ??????????????????, ?????????RejectedPutBufferHandler??????(??????Lambda?????????)??????@Autowired???????????? */
    @Autowired(required = false)
    private RejectedPutBufferHandler rejectedPutBufferHandler;

    /** ????????????: ????????????, ?????????????????????
     ??????????????????, ???????????????, ?????????UidGenerateException??????. ??????????????????, ?????????RejectedTakeBufferHandler??????(??????Lambda?????????)??????@Autowired???????????? */
    @Autowired(required = false)
    private RejectedTakeBufferHandler rejectedTakeBufferHandler;

    /**
     * RingBuffer
     */
    private RingBuffer ringBuffer;
    private BufferPaddingExecutor bufferPaddingExecutor;

    public CachedUidGenerator(UidProperties uidProperties) {
        super(uidProperties);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(!uidProperties.isEnable()){
            return;
        }
        // initialize workerId & bitsAllocator
        super.afterPropertiesSet();

        boostPower = environment.getProperty(BidProperty.BID_UID_CACHED_BOOST_POWER,Integer.class,DEFAULT_BOOST_POWER);
        paddingFactor = environment.getProperty(BidProperty.BID_UID_CACHED_PADDING_FACTOR,Integer.class,RingBuffer.DEFAULT_PADDING_PERCENT);
        scheduleInterval = environment.getProperty(BidProperty.BID_UID_CACHED_SCHEDULE_INTERVAL,Long.class,null);

        // initialize RingBuffer & RingBufferPaddingExecutor
        this.initRingBuffer();
        LOGGER.info("Initialized RingBuffer successfully.");
    }

    @Override
    public long getUID() {
        try {
            return ringBuffer.take();
        } catch (Exception e) {
            LOGGER.error("Generate unique id exception. ", e);
            throw new UidGenerateException(e);
        }
    }

    @Override
    public String parseUID(long uid) {
        return super.parseUID(uid);
    }

    @Override
    public void destroy() throws Exception {
        bufferPaddingExecutor.shutdown();
    }

    /**
     * Get the UIDs in the same specified second under the max sequence
     *
     * @param currentSecond
     * @return UID list, size of {@link BitsAllocator#getMaxSequence()} + 1
     */
    protected List<Long> nextIdsForOneSecond(long currentSecond) {
        // Initialize result list size of (max sequence + 1)
        int listSize = (int) bitsAllocator.getMaxSequence() + 1;
        List<Long> uidList = new ArrayList<>(listSize);

        // Allocate the first sequence of the second, the others can be calculated with the offset
        long firstSeqUid = bitsAllocator.allocate(currentSecond - uidProperties.getEpochSeconds(), workerId, 0L);
        for (int offset = 0; offset < listSize; offset++) {
            uidList.add(firstSeqUid + offset);
        }

        return uidList;
    }

    /**
     * Initialize RingBuffer & RingBufferPaddingExecutor
     */
    private void initRingBuffer() {
        // initialize RingBuffer
        int bufferSize = ((int) bitsAllocator.getMaxSequence() + 1) << boostPower;
        this.ringBuffer = new RingBuffer(bufferSize, paddingFactor);
        LOGGER.info("Initialized ring buffer size:{}, paddingFactor:{}", bufferSize, paddingFactor);

        // initialize RingBufferPaddingExecutor
        boolean usingSchedule = (scheduleInterval != null);
        this.bufferPaddingExecutor = new BufferPaddingExecutor(ringBuffer, this::nextIdsForOneSecond, usingSchedule);
        if (usingSchedule) {
            bufferPaddingExecutor.setScheduleInterval(scheduleInterval);
        }

        LOGGER.info("Initialized BufferPaddingExecutor. Using schdule:{}, interval:{}", usingSchedule, scheduleInterval);

        // set rejected put/take handle policy
        this.ringBuffer.setBufferPaddingExecutor(bufferPaddingExecutor);
        if (rejectedPutBufferHandler != null) {
            this.ringBuffer.setRejectedPutHandler(rejectedPutBufferHandler);
        }
        if (rejectedTakeBufferHandler != null) {
            this.ringBuffer.setRejectedTakeHandler(rejectedTakeBufferHandler);
        }

        // fill in all slots of the RingBuffer
        bufferPaddingExecutor.paddingBuffer();

        // start buffer padding threads
        bufferPaddingExecutor.start();
    }

    /**
     * Setters for spring property
     */
    public void setBoostPower(int boostPower) {
        Assert.isTrue(boostPower > 0, "Boost power must be positive!");
        this.boostPower = boostPower;
    }

    public void setPaddingFactor(int paddingFactor) {
        Assert.isTrue(paddingFactor > 0 && paddingFactor < 100, "padding factor must be in (0, 100)!");
        this.paddingFactor = paddingFactor;
    }

    public void setRejectedPutBufferHandler(RejectedPutBufferHandler rejectedPutBufferHandler) {
        Assert.notNull(rejectedPutBufferHandler, "RejectedPutBufferHandler can't be null!");
        this.rejectedPutBufferHandler = rejectedPutBufferHandler;
    }

    public void setRejectedTakeBufferHandler(RejectedTakeBufferHandler rejectedTakeBufferHandler) {
        Assert.notNull(rejectedTakeBufferHandler, "RejectedTakeBufferHandler can't be null!");
        this.rejectedTakeBufferHandler = rejectedTakeBufferHandler;
    }

    public void setScheduleInterval(long scheduleInterval) {
        Assert.isTrue(scheduleInterval > 0, "Schedule interval must positive!");
        this.scheduleInterval = scheduleInterval;
    }

}
