# 背景



全局唯一ID是我们在设计分布式架构中必须要考虑到的问题，如何能快速方便拿到一个全局唯一的序号ID也是我们经常思考的，在开发过程中，产品或业务人员也提出了一些特性要求，比如模块+时间+自增序号，类似单据编号必须为RD202009080001按天递增，诸如此类， 考虑到此类需求会经常遇到，所以有必要提取为公共组件。

# 需求

1. 全局唯一

2. 支持高并发，高性能

3. 能够体现一定业务属性，符合业务特性

4. 高可靠，容错单点故障，支持降级

5. 递增



# 方案对比分析



**常见的分布式全局唯一ID方案如下：**



| 方案       | 描述                                                         | 优点                                                         | 缺点                                                         |
| :--------- | :----------------------------------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| 基于数据库 | 基于数据库自动增长机制主要思路是采用数据库自增ID + replace_into实现唯一ID的获取 | 简单、可控 ID号单调自增，可以实现一些对ID有特殊要求的业务    | 数据库压力大，性能有限（可通过DB集群设置不同步长改善，即部署N台数据库实例，每台设置成不同的初始值，自增步长为机器的台数）信息不安全：在某些情况会暴露业务数据，如订单号采用自动增长，很容易得出下一个订单号，甚至可计算出订单量（自增的都会有这个问题，自行取舍）依赖数据库 |
| UUID       | 在一台机器在同一时间中生成的数字在所有机器中都是唯一的UUID由以下几部分的组合： （1）当前日期和时间。 （2）时钟序列。 （3）全局唯一的IEEE机器识别号，如果有网卡，从网卡MAC地址获得，没有网卡以其他方式获得。 | 性能非常高：本地生成，没有网络消耗不需要其他依赖             | 不易于存储：UUID太长，16字节128位，通常以36长度的字符串表示，很多场景不适用。 信息不安全：基于MAC地址生成UUID的算法可能会造成MAC地址泄露，这个漏洞曾被用于寻找梅丽莎病毒的制作者位置。 ID作为主键时在特定的环境会存在一些问题，比如做DB主键的场景下，需要做索引的情况下，UUID就非常不适用不利于索引，作为主键建立索引查询效率低 |
| 基于Redis  | 通过Redis的原子操作INCR和INCRBY实现递增                      | 不依赖于数据库，灵活方便，性能高。数字ID天然排序，对分页或者需要排序的结果很有帮助。使用Redis集群也可以防止单点故障的问题 | 依赖第三方组件Redis，增加系统复杂度。需要编码和配置的工作量比较大。 |
| Snowflake  | snowflake 是 twitter 开源的分布式ID生成算法，其核心思想为，一个long型的ID：41 bit 作为毫秒数 - 41位的长度可以使用69年10 bit 作为机器编号 （5个bit是数据中心，5个bit的机器ID） - 10位的长度最多支持部署1024个节点12 bit 作为毫秒内序列号 - 12位的计数顺序号支持每个节点每毫秒产生4096个ID序号 | 简单高效，生成速度快。 时间戳在高位，自增序列在低位，整个ID是趋势递增的，按照时间有序递增。 灵活度高，可以根据业务需求，调整bit位的划分，满足不同的需求。不需要其他依赖，使用方便 | 强依赖机器的时钟，如果服务器时钟回拨，会导致重复ID生成。在分布式环境上，每个服务器的时钟不可能完全同步，有时会出现不是全局递增的情况。不同机器配置不同worker id麻烦 |
| Flicker    | Flicker在解决全局ID生成方案里就采用了MySQL自增长ID的机制（auto_increment + replace into + MyISAM）扩展：为解决单点问题，启用多台服务器，如MySQL，利用给字段设置auto_increment_increment和auto_increment_offset来保证ID自增（如通过设置起始值与步长，生成奇偶数ID） | 充分借助数据库的自增ID机制，提供高可靠性，生成的ID有序       | 强依赖DB，当DB异常时，整个系统不可用ID发号性能瓶颈限制在单台MySQL的读写性能水平扩展困难（定义好了起始值，步长和机器台数之后，如果要添加机器就比较麻烦 |
| 美团Leaf   | 美团的Leaf分布式ID生成系统，在Flicker策略与Snowflake算法的基础上做了两套优化的方案：Leaf-segment数据库方案（相比Flicker方案每次都要读取数据库，该方案改用proxy server批量获取，且做了双buffer的优化）与Leaf-snowflake方案（主要针对时钟回拨问题做了特殊处理。若发生时钟回拨则拒绝发号，并进行告警） | 全局唯一，高可用、高性能用zookeeper解决了各个服务器时钟回拨的问题，弱依赖zookeeper | 依赖第三方组件，如zookeeper                                  |
| 百度Uid    | UidGenerator是Java实现的, 基于Snowflake算法的唯一ID生成器。UidGenerator以组件形式工作在应用项目中, 支持自定义workerId位数和初始化策略, 从而适用于docker等虚拟化环境下实例自动重启、漂移等场景。 | 全局唯一，高可用、高性能解决了始终回拨的问题                 | 内置WorkerID分配器, 依赖数据库，启动阶段通过DB进行分配; 如自定义实现, 则DB非必选依赖（用后即弃） |
| 其他       | 利用zookeeper生成唯一ID、MongoDB（Document）全局唯一ID、滴滴的tinyid等等 |                                                              |                                                              |
|            |                                                              |                                                              |                                                              |



### **总结：**

总的来看，方案主要分为两种：第一有中心（如数据库，包括MYSQL，REDIS等），其中可以会利用事先的预约来实现集群（起始步长）。第二种就是无中心，通过生成足够散落的数据，来确保无冲突（如UUID等）。



#### 中心化方案：

##### 优点：

- 数据长度相对小一些；
- 数据可以实现自增趋势等。

##### 缺点：

- 并发瓶颈处理；
- 集群需要实现约定；横向扩展困难（当然有的方案看起来后两者没有那么问题，是因为这些方案利用其技术特性，早就一定程度上解决了这些问题，如Redis的横向扩展等）。



#### 非中心化方案：

##### 优点：

- 实现简单（因为不需要与其他节点存在这方面的约定，耦合）；
- 不会出现中心节点带来的性能瓶颈；
- 扩展性较高（扩展的局限往往集中于数据的离散问题）。

##### 缺点：

- 数据长度较长（毕竟就是通过这一特性来实现无冲突的）；
- 无法实现数据的自增长（毕竟是随机的）；



# BID的方案

BID的方案是基于上述方案分析后，提供两种生成全局唯一ID的方式，集成后可通过配置灵活选择使用。

具体两种方式如下：

## 一、基于Redis的全局唯一ID实现符合业务特性的方案

根据业务特性，以及性能方面的考虑，**采用基于Redis的全局唯一ID实现方案，简称RedisID**

> 业务特性是指产品或业务人员要求数据编号必须符合一定的规则，比如要求订单单据编号必须为业务标识+日期+序列，并要求按天重置（OD202011030001、OD202011040001），人员编号为标识+序列号（UN00000001）等等诸如此类。

BID正是基于此需求定制的组件，满足以下特性：

1. 全局唯一、基于Redis性能高

2. 支持根据业务特性生成，如有序自增、可配置是否需要业务模块前缀、定制日期格式、是否需要按天重置、定制生成的序列ID位数自动补位等

3. 支持Redis多数据源，可通过独立部署Redis服务器的方式隔离Redis业务数据，避免ID序列被清空失效，可通过Redis集群解决单点故障问题，实现高可用。

4. 内置服务降级策略， 也支持自定义降价策略，当Redis生成ID服务不可用时，会采取服务降级策略生成。
5. 基于Lua脚本实现Next Id获取不会出现并发问题。
6. 基于spring-boot-starter方式封装，接入方便。



## 二、封装了百度UID，提供雪花ID的实现

百度UID解决了雪花ID在分布式集群环境下WorkerID分配的问题、时钟问题、支持超高并发、去中心化等，基于spring-boot-starter封装后比原项目接入更简单方便。

**注意：有些往往在网上随便找了一个简单的工具类，就在项目中直接工具类生成雪花ID。实际上这是有问题的，雪花ID有两个很重要的参数datacenterId、workerId，而这些工具类中这些参数是直接写死的，这会导致生成的ID在分布式集群环境下并不一定是全局唯一的。包括mybatis-plus中@TableId(type = IdType.ASSIGN_ID)主键策略用的雪花ID 需要的workid，datacenterid是根据网卡硬件地址生成这也有问题，也会出现重复，参考：https://github.com/baomidou/mybatis-plus/issues/3170**



基于 [百度UidGenerator](https://github.com/baidu/uid-generator), 做了以下改动：

- 改造为spring-boot-starter的形式，不用部署为分布式，直接建表、在项目中引入，即可使用
- 针对时钟回拨，提供了修正选项（默认启用，可通过配置关闭），小于阈值直接休眠，大于阈值更改机器号
- 对机器id用尽提供了复用策略：取余
- 解除id位数限制，由“必须64位”改为“不大于64位”，可根据需要获取更短id

参数均可通过Spring进行自定义，默认参数为：

- delta seconds (30 bits)
  当前时间，相对于时间基点"2021-02-08"的增量值，单位：秒，最多可支持约34年，超出抛异常
- worker id (16 bits)
  机器id，最多可支持约6.5w次机器启动。内置实现为在启动时由数据库分配，默认分配策略为用后即弃，当前复用策略为取余。
- sequence (7 bits)
  每秒下的并发序列，7 bits可支持每秒128个并发，超出128则等待下一秒

默认参数下，初始id长度为12，最终随时间增加，最长到16位



该实现参考：https://github.com/RogerJTX/uid-generator-spring-boot-starter



## 接入指南

### 1.配置Maven仓库地址

redis的全局唯一ID组件包上传到了公司统一的Maven仓库中，需要在Maven的setting.xml文件或项目pom.xml配置Maven仓库地址



> ```
> 
>  ```

### 2.引入BID组件依赖

在项目pom.xml文件中引入组件依赖



> ```
>         <dependency>
>             <groupId>com.daaao</groupId>
>             <artifactId>bid-spring-boot-starter</artifactId>
>             <version>1.0.0</version>
>             <exclusions>
>                 <exclusion>
>                     <artifactId>*</artifactId>
>                     <groupId>org.mybatis</groupId>
>                 </exclusion>
>                 <exclusion>
>                     <artifactId>*</artifactId>
>                     <groupId>mysql</groupId>
>                 </exclusion>
>             </exclusions>
>         </dependency>
>         
>         UID需要依赖mysql数据库、mybatis，若项目已有这些依赖请将其排除掉，其他数据库如Oracle未测试过。
> ```



### 3.启用BID组件

通过在SpringBootApplication类上面添加@EnableBid注解来启用BID，这是必要步骤。

```java
@EnableBid
@SpringBootApplication
public class BidDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(BidDemoApplication.class, args);
    }
}
```



### 4.组件配置

使用之前需在application.yml文件中配置如下配置：

**完整配置：**

```yaml
spring:
  bid:
    appName: TEST-BID  #应用名称，当多个使用BID的项目共用同一个Redis服务时，RedisID用于隔离不同的应用、项目，非必填，不配置时取spring.application.name参数
    redis:
      enable: true #是否启用RedisID，默认false，非必填
      host: 127.0.0.1 #redis id生成服务的redis服务器配置信息，非必填，不配置时取spring.redis.host
      port: 6379 #非必填，不配置时取spring.redis.port
      password: #非必填，不配置时取spring.redis.password
      database: 1 #非必填，默认为0，建议配置与项目应用使用的database配置不同的database区别开来进行隔离
      timeout: 6000  # 连接超时时长（毫秒），非必填
      lettuce: #非必填
        commandTimeout: 1   #秒s ，非必填
        shutdownTimeout: 100  #毫秒ms，非必填 
        pool:
          max-active: 32 # 连接池最大连接数（使用负值表示没有限制），非必填
          max-wait: 300 # 连接池最大阻塞等待时间（使用负值表示没有限制），非必填
          max-idle: 16 # 连接池中的最大空闲连接，非必填
          min-idle: 8 # 连接池中的最小空闲连接，非必填
      cluster: #集群配置信息，该节点可不配置，留待后续redis扩展集群使用，目前只有单机，非必填
        max-redirects: 3  # 获取失败 最大重定向次数，非必填
        timeout: 1000 #，非必填
        #nodes: 127.0.0.1:6379 #非必填，配置时将使用redis集群，不配置则使用上面配置的redis单机
    uid:
      enable: false #是否启用UID （可生成雪花ID）
      timeBits: 29   # 时间位, 默认:30
      workerBits: 17 # 机器位, 默认:24
      seqBits: 9     # 序列位, 默认:9
      epochStr: "2021-02-08"   # 初始时间, 默认:"2021-02-08"
      enableBackward: true    # 是否容忍时钟回拨, 默认:true
      maxBackwardSeconds: 1    # 时钟回拨最长容忍时间（秒）, 默认:1
      cached: # CachedUidGenerator相关参数
        boostPower: 3          # RingBuffer size扩容参数, 可提高UID生成的吞吐量, 默认:3
        paddingFactor: 50      # 指定何时向RingBuffer中填充UID, 取值为百分比(0, 100), 默认为50
        #scheduleInterval: 60    # 默认:不配置此项, 即不使用Schedule线程. 如需使用, 请指定Schedule线程时间间隔, 单位:秒
```



> 上述配置说明：
>
> 1、整体配置分为两部分：a.若需要使用ReidsID则配置spring.bid.redis部分，b.若需要使用雪花ID则配置spring.bid.uid部分
>
> 2、关于为什么弄了个跟spring.redis一样的配置，多次一举？：考虑到我们有时会直接去将Redis上面的缓存清空，而BID使用Redis会对序列进行递增，会生成一些key，而这些key是不能删除的，否则会导致序列重置出问题，所以为了避免风险，做了Redis多数据源，将BID使用的redis与应用本身使用的redis进行隔离，可以使用不同的Redis服务器，你可以是多个项目都用同一个Redis服务器来专门给BID使用，而应用使用另外的redis服务器。当然如果嫌麻烦bid也可以与项目本身使用同一个redis服务器，可配置不同的database隔离。
>
> 3、**UID启用使用雪花ID时，一定要先在项目数据库中使用WORKER_NODE.sql 脚本先创建分配节点的数据库表。**
>
> 4、UID的一些配置可参考：https://github.com/baidu/uid-generator/blob/master/README.zh_cn.md
>
> 



**简化配置：**

1、RedisID模式

```yaml
spring:
  bid:
    redis:
      enable: true #redis id生成服务的redis服务器配置信息
      database: 3
```

2、UID模式（雪花ID）

```yaml
spring:
  bid:
    uid:
      enable: true #是否启用UID （可生成雪花ID）
      timeBits: 29   # 时间位
      workerBits: 17 # 机器位
      seqBits: 9     # 序列位
```

> 使用UID一定要先在项目数据库中使用WORKER_NODE.sql 脚本先创建分配节点的数据库表。如果是微服务项目有几个库就需要在几个库中都创建这个表。
>
> 两种ID模式可同时使用。



## 使用指南



### 一、基于Redis的全局唯一ID实现符合业务特性的方案



#### 1.配置RedisID 的生成规则

```java
package com.example.bid.config;


import com.daaao.bid.policy.fallback.IdFallback;
import com.daaao.bid.policy.redis.IdPatternConfiguration;
import com.daaao.bid.policy.redis.RedisIdTemplate;
import com.daaao.bid.policy.redis.SimpleDateFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hao
 * @version 1.0
 * @date 2021/1/19 14:30
 */
@Configuration
public class RedisIdPatternConfiguration {

    @Bean
    public RedisIdTemplate fullRedisIdTemplate() {
//        全部参数
        IdPatternConfiguration redisIdConfig = IdPatternConfiguration.builder()
                //redis的key名称，同时也是ID前缀（如果需要拼接前缀的话）
                .key("DEMO12")
                //日期格式，不传默认不拼接日期
                .dateTimeFormatter(SimpleDateFormatter.FORMATTER_DAY)
                //redis生成ID的位数，如果不配置默认补位，如配置5，此时Redis序列值为“6”小于5位数，则生成的ID为00006
                .digits(6)
                //redis递增步长，如当前序列值为5，配置increment=5则下次序列为10
                .increment(1)
                //redis序列初始值，不配置不覆盖redis的值，若配置值小于redis服务器序列值，则不覆盖，大于则从initial的设置的值开始递增
                .initial(0L)
                //是否需要前缀，搭配key使用，默认false不拼接前缀
                .needPrefix(false)
                //是否需要每天重置序列，默认true，必须搭配配置dateTimeFormatter参数一起使用，为true的话序列会每天重置为initial的参数值，false不重置
                .isResetDaily(true)
                //ID生成的服务降级策略，当Redis不能提供服务或其他异常错误导致生成失败时ID如何生成，默认降级策略为随机数+UUID，具体看源码
                .idFallback(new IdFallback() {
                    @Override
                    public String nextId() {
                        return "id11111";
                    }
                })
                .build();
        return new RedisIdTemplate(redisIdConfig);
    }
}
```



#### 2.调用生成ID方法

> ```
> @Resource(name = "fullRedisIdTemplate")
> private RedisIdTemplate fullRedisIdTemplate;
> 
> 
> //在需要生成ID的地方调用
> System.out.println("id9====" + fullRedisIdTemplate.next());
> ```



对于更复杂的多个不同ID的生成规则，可以配置多个不同的RedisIdTemplate的实例Bean注入来实现，也可以通过重载方法传递不同的参数来实现。

**方法一：通过配置多个RedisIdTemplate来生成**

先定义规则：

> ```
> @Configuration
> public class IdConfig {
> 
>     @Bean
>     public RedisIdTemplate zzRedisIdTemplate() {
> //        字段“组织编码”由系统自动生成。编码规则为：zz+年月日（20190901）+三位数（001）
>         IdPatternConfiguration redisIdConfig = IdPatternConfiguration.builder()
>                 .key("zz")
>                 .dateTimeFormatter(SimpleDateFormatter.FORMATTER_DAY)
>                 .digits(3)
>                 .build();
>         return new RedisIdTemplate(redisIdConfig);
>     }
> 
>     @Bean
>     public RedisIdTemplate gysRedisIdTemplate() {
> //        供应商编码：系统自动生成，编码规则：gys+年月日（20190908）+四位数（0001）
>         IdPatternConfiguration redisIdConfig = IdPatternConfiguration.builder()
>                 .key("gys")
>                 .dateTimeFormatter(SimpleDateFormatter.FORMATTER_DAY)
>                 .digits(4)
>                 .build();
>         return new RedisIdTemplate(redisIdConfig);
>     }
> 
>     @Bean
>     public RedisIdTemplate jsRedisIdTemplate() {
> //        角色编码：js+yyyyMMddHHmmssSSS
>         IdPatternConfiguration redisIdConfig = IdPatternConfiguration.builder()
>                 .key("js")
>                 .dateTimeFormatter(SimpleDateFormatter.FORMATTER_MILLISECOND)
>                 .digits(2)
>                 .build();
>         return new RedisIdTemplate(redisIdConfig);
>     }
> 
>     @Bean
>     public RedisIdTemplate fullRedisIdTemplate() {
> //        全部参数
>         IdPatternConfiguration redisIdConfig = IdPatternConfiguration.builder()
>                 //redis的key名称，同时也是ID前缀（如果需要拼接前缀的话）
>                 .key("keyName")
>                 //日期格式，不传默认不拼接日期
>                 .dateTimeFormatter(SimpleDateFormatter.FORMATTER_SECOND)
>                 //redis生成ID的位数，如果不配置默认补位，如配置5，此时Redis序列值为“6”小于5位数，则生成的ID为00006
>                 .digits(10)
>                 //redis递增步长，如当前序列值为5，配置increment=5则下次序列为10
>                 .increment(5)
>                 //redis序列初始值，不配置不覆盖redis的值，若配置值小于redis服务器序列值，则不覆盖，大于则从initial的设置的值开始递增
>                 .initial(500L)
>                 //是否需要前缀，搭配key使用，默认false不拼接前缀
>                 .needPrefix(false)
>                 //ID生成的服务降级策略，当Redis不能提供服务或其他异常错误导致生成失败时ID如何生成，默认降级策略为随机数+UUID，具体看源码
>                 .idFallback(new IdFallback() {
>                     @Override
>                     public String nextId() {
>                         return "id11111";
>                     }
>                 })
>                 .build();
>         return new RedisIdTemplate(redisIdConfig);
>     }
> }
> ```



调用：

> ```
> @Resource(name = "zzRedisIdTemplate")
> private RedisIdTemplate zzRedisIdTemplate;
> 
> @Resource(name = "gysRedisIdTemplate")
> private RedisIdTemplate gysRedisIdTemplate;
> 
> @Resource(name = "jsRedisIdTemplate")
> private RedisIdTemplate jsRedisIdTemplate;
> 
> @Resource(name = "fullRedisIdTemplate")
> private RedisIdTemplate fullRedisIdTemplate;
> System.out.println("id6====" + zzRedisIdTemplate.next());
> System.out.println("id7====" + gysRedisIdTemplate.next());
> System.out.println("id8====" + jsRedisIdTemplate.next());
> System.out.println("id9====" + fullRedisIdTemplate.next());
> ```



**方法二：通过默认内置RedisIdTemplate调用不同方法来生成**

> ```
> @Autowired
> private RedisIdTemplate redisIdTemplate;
> 
> 
> //调用
> String id5 = redisIdTemplate.next("ROLEID4", SimpleDateFormatter.FORMATTER_MILLISECOND, 6, 5, 100L, false, true, null);
> ```



生成ID的重载方法有很多，可以自行选择，但必须要传key名称。

如果只需要使用Redis完成自增序列，也可直接使用RedisIdWorker类

> ```
> @Autowired
> private RedisIdWorker redisIdWorker;
> Long id = redisIdWorker.incr("SAAS-ZZ-UUID-TEST",5,56L);
> ```



#### 3.服务降级策略

有时Redis服务可能出现问题，导致ID无法生成，这就需要一个降级策略来避免这种问题，BID生成的服务降级策略可以通过实现IdFallback类来自定义。如默认内置的服务降级策略实现：

> ```
> @Component
> public class DefaultIdFallback implements IdFallback {
> 
>     @Override
>     public String nextId() {
>         return String.valueOf(PhoenixUuid.uuid());
>     }
> }
> ```

默认的降级策略为：4为随机数+UUID的hashCode生成，也会根据needPrefix 参数来判断是否需要拼接前缀。



#### 4.RedisID参数说明

ID生成规则配置IdPatternConfiguration的参数如下：

| 参数名称              | 含义说明                                                     | 是否必须 | 默认值                 | 备注                                                         |
| :-------------------- | :----------------------------------------------------------- | :------- | :--------------------- | :----------------------------------------------------------- |
| **key**               | redis的key名称，同时也是ID前缀（如果需要拼接前缀的话）       | 是       |                        |                                                              |
| **dateTimeFormatter** | 日期格式，不传默认不拼接日期                                 | 否       | 默认为空不拼接日期     | 可使用SimpleDateFormatter类，里面配置了三个常用的日期格式：yyyyMMdd、yyyyMMddHHmmss、yyyyMMddHHmmssSSS |
| **digits**            | `redis生成ID的位数，如果不配置默认补位，如配置5，此时Redis序列值为“6”小于5位数，则生成的ID为00006` | 否       | 默认为0不补位          |                                                              |
| **increment**         | `redis递增步长，如当前序列值为5，配置increment=5则下次序列为10` | 否       | 默认为1                |                                                              |
| **initial**           | `redis序列初始值，不配置不覆盖redis的值，若配置值小于redis服务器序列值，则不覆盖，大于则从initial的设置的值开始递增` | 否       | 默认为1                |                                                              |
| **needPrefix**        | `是否需要前缀，搭配key使用，默认为false不拼接key配置的值，拼接需要配置为true` | 否       | 默认false不拼接前缀    |                                                              |
| **idFallback**        | `ID生成的服务降级策略，当Redis不能提供服务或其他异常错误导致生成失败时ID如何生成，默认降级策略为随机数+UUID，具体看源码` | 否       | 默认采用4位随机数+UUID |                                                              |
| isResetDaily          | 是否需要每天重置序列，默认true，必须搭配配置dateTimeFormatter参数一起使用，为true的话序列会每天重置为initial的参数值，false不重置 | 否       | 默认true，按天重置序列 |                                                              |

#### 5.测试

通过本地线程模拟并发测试，无重复ID记录

> ```
> // 请求总数
>     public static int clientTotal = 10000;
>     // 同时并发执行的线程数
>     public static int threadTotal = 10000;
>     @Test
>     public void testGetNextSeqWithConcurrency() throws  Exception{
>         final Set<String> seqSet = new ConcurrentSkipListSet<>();
> 
>         ExecutorService executorService = Executors.newCachedThreadPool();
>         //信号量，此处用于控制并发的线程数
>         final Semaphore semaphore = new Semaphore(threadTotal);
>         //闭锁，可实现计数器递减
>         final CountDownLatch countDownLatch = new CountDownLatch(clientTotal);
>         for (int i = 0; i < clientTotal ; i++) {
>             executorService.execute(() -> {
>                 try {
>                     //执行此方法用于获取执行许可，当总计未释放的许可数不超过200时，
>                     //允许通行，否则线程阻塞等待，直到获取到许可。
>                     semaphore.acquire();
> //                    add();
>                     String seq = zzRedisIdTemplate.next();
>                     if (!seqSet.add(seq)) {
>                         System.out.println(seq);
>                         fail();
>                     }
>                     //释放许可
>                     semaphore.release();
>                 } catch (Exception e) {
>                     //log.error("exception", e);
>                     e.printStackTrace();
>                 }
>                 //闭锁减一
>                 countDownLatch.countDown();
>             });
>         }
>         countDownLatch.await();//线程阻塞，直到闭锁值为0时，阻塞才释放，继续往下执行
>         executorService.shutdown();
>         assertEquals(seqSet.size(), clientTotal);
>     }
> ```





### 二、封装了百度UID，提供雪花ID的实现

#### 1.创建worker_node数据库表

通过如下脚本，在各数据库中创建WORKER_NODE表

```
DROP TABLE IF EXISTS WORKER_NODE;
CREATE TABLE WORKER_NODE
(
ID BIGINT NOT NULL AUTO_INCREMENT COMMENT 'auto increment id',
HOST_NAME VARCHAR(64) NOT NULL COMMENT 'host name',
PORT VARCHAR(64) NOT NULL COMMENT 'port',
TYPE INT NOT NULL COMMENT 'node type: CONTAINER(1), ACTUAL(2), FAKE(3)',
LAUNCH_DATE DATE NOT NULL COMMENT 'launch date',
MODIFIED TIMESTAMP NOT NULL COMMENT 'modified time',
CREATED TIMESTAMP NOT NULL COMMENT 'created time',
PRIMARY KEY(ID)
)
 COMMENT='DB WorkerID Assigner for UID Generator',ENGINE = INNODB;
```

该表用于生成雪花ID时，每次服务实例节点workerID的分配。



#### 2.配置ID的生成参数

以下为可选配置, 如未指定将采用默认值

```
    uid:
      enable: false #是否启用UID （可生成雪花ID）
      timeBits: 29   # 时间位, 默认:30
      workerBits: 17 # 机器位, 默认:24
      seqBits: 9     # 序列位, 默认:9
      epochStr: "2021-02-08"   # 初始时间, 默认:"2021-02-08"
      enableBackward: true    # 是否容忍时钟回拨, 默认:true
      maxBackwardSeconds: 1    # 时钟回拨最长容忍时间（秒）, 默认:1
      cached: # CachedUidGenerator相关参数
        boostPower: 3          # RingBuffer size扩容参数, 可提高UID生成的吞吐量, 默认:3
        paddingFactor: 50      # 指定何时向RingBuffer中填充UID, 取值为百分比(0, 100), 默认为50
        #scheduleInterval: 60    # 默认:不配置此项, 即不使用Schedule线程. 如需使用, 请指定Schedule线程时间间隔, 单位:秒
```

**可选实现**

选用CachedUidGenerator时，可以选择实现“拒绝策略”的拓展

- 拒绝策略: 当环已满, 无法继续填充时
  默认无需指定, 将丢弃Put操作, 仅日志记录. 如有特殊需求, 请实现RejectedPutBufferHandler接口(支持Lambda表达式)
- 拒绝策略: 当环已空, 无法继续获取时
  默认无需指定, 将记录日志, 并抛出UidGenerateException异常. 如有特殊需求, 请实现RejectedTakeBufferHandler接口(支持Lambda表达式)



#### 3.开始使用

UidGenerator接口提供了 UID 生成和解析的方法，提供了两种实现:

- DefaultUidGenerator
  实时生成
- CachedUidGenerator
  生成一次id之后，按序列号+1生成一批id，缓存，供之后请求

如对UID生成性能有很高要求, 请使用CachedUidGenerator

```
//@Resource
//private UidGenerator defaultUidGenerator;

@Resource
private UidGenerator cachedUidGenerator;

@Test
public void testSerialGenerate() {
    // Generate UID
    long uid = cachedUidGenerator.getUID();

    // Parse UID into [Timestamp, WorkerId, Sequence]
    // {"UID":"450795408770","timestamp":"2019-02-20 14:55:39","workerId":"27","sequence":"2"}
    System.out.println(cachedUidGenerator.parseUID(uid));

}
```

### 

#### 4.参考文档

https://github.com/baidu/uid-generator/blob/master/README.zh_cn.md

https://github.com/RogerJTX/uid-generator-spring-boot-starter



### 三、最佳实践



新建一个BID类：

```
package com.example.bid.config;

import com.daaao.bid.policy.redis.RedisIdTemplate;
import org.apache.commons.lang.StringUtils;

@Slf4j
public class Bid {

    public static long next(){
        try {
            DefaultUidGenerator defaultUidGenerator = ApplicationContextHolder.getBean("defaultUidGenerator", DefaultUidGenerator.class);
            return defaultUidGenerator.getUID();
        }catch (Exception e){
            log.error("uid生成唯一ID失败，采用服务降级策略[redis]生成", e);
            RedisIdTemplate redisIdTemplate = ApplicationContextHolder.getBean("fullRedisIdTemplate", RedisIdTemplate.class);
            return Long.parseLong(redisIdTemplate.next());
        }
    }

    public static String nextId(){
        return String.valueOf(next());
    }

    /**
     * 获取具备日期格式的ID
     * @param key 模块前缀，隔离作用
     * @return 具备日期格式的ID
     */
    public static String formatId(String key){
        RedisIdTemplate redisIdTemplate = ApplicationContextHolder.get().getBean("fullRedisIdTemplate", RedisIdTemplate.class);
        if(StringUtils.isEmpty(key)){
            return redisIdTemplate.next();
        }
        return redisIdTemplate.next(key);
    }

    /**
     * 获取具备日期格式的ID
     * @return 具备日期格式的ID
     */
    public static String formatId(){
        return formatId(null);
    }
}
```



ApplicationContextHolder源码：

```
package com.example.bid.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextHolder implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(ApplicationContextHolder.class);
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.applicationContext = applicationContext;
    }

    //获取applicationContext
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static <T> T getBean(Class<T> requiredType) {
        if (requiredType == null) {
            return null;
        } else {
            try {
                return getApplicationContext().getBean(requiredType);
            } catch (BeansException var2) {
                log.warn("Spring bean not found! type: {}", requiredType.getName());
                return null;
            }
        }
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        return getApplicationContext().getBean(name, requiredType);
    }

    public static boolean containsBean(String name) {
        return getApplicationContext().containsBean(name);
    }

    public static boolean isSingleton(String name) {
        return getApplicationContext().isSingleton(name);
    }

    public static Class<? extends Object> getType(String name) {
        return getApplicationContext().getType(name);
    }


}
```



结合Mybatis-plus使用，自定义ID生成策略：

```
@Component
public class BidIdentifierGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        return Bid.next();
    }
}
```



也可以在需要获取ID的地方手动获取：

```
调用Bid.nextId()即可

System.out.println(Bid.nextId());
```



参考DEMO：https://gitee.com/blind/bid-demo



## DEMO源码

源代码：https://gitee.com/blind/bid-demo



# 相关资料

分布式全局唯一ID生成策略 https://youzhixueyuan.com/how-to-generate-distributed-unique-id.html

Leaf——美团点评分布式ID生成系统 https://tech.meituan.com/2017/04/21/mt-leaf.html

UidGenerator分布式ID服务 https://github.com/baidu/uid-generator/blob/master/README.zh_cn.md

分布式全局序列ID方案之Flicker优化方案 https://nicky-chen.github.io/2018/09/25/id-flicker/

分布式唯一id：snowflake算法思考 https://www.cnblogs.com/jiangxinlingdu/p/8440413.html