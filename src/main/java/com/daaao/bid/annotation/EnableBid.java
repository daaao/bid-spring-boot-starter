package com.daaao.bid.annotation;

import com.daaao.bid.BidAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author hao
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(BidAutoConfiguration.class)
public @interface EnableBid {
}
