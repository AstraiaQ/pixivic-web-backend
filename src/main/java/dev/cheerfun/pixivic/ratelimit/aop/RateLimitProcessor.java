package dev.cheerfun.pixivic.ratelimit.aop;

import dev.cheerfun.pixivic.ratelimit.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;


/**
 * @author echo huang
 * @version 1.0
 * @date 2019-07-13 15:12
 * @description 限流监听器
 */
@Aspect
@Component
@Slf4j
public class RateLimitProcessor {


    @Resource(name = "limitRedisTemplate")
    private RedisTemplate<String, Integer> redisTemplate;

    @Pointcut(value = "@annotation(dev.cheerfun.pixivic.ratelimit.annotation.RateLimit)")
    public void pointCut() {
    }

    @Around(value = "pointCut()")
    public Object handleRateLimiter(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj = null;
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        RateLimit annotation = methodSignature.getMethod().getAnnotation(RateLimit.class);
        long limitNum = annotation.limitNum();
        long sec = annotation.sec();
        //TODO userId
        String userId = "userId";
        ValueOperations<String, Integer> tokenBucket = redisTemplate.opsForValue();
        Integer maxLimit = tokenBucket.get(userId);
        if (maxLimit == null) {
            //fixme 这里会有并发问题，俩个服务同时获取一个桶，该桶为空因此默认只取第一个桶
            tokenBucket.setIfAbsent(userId, 1, sec, TimeUnit.SECONDS);
            obj = joinPoint.proceed();
        } else if (limitNum > maxLimit) {
            tokenBucket.increment(userId);
            obj = joinPoint.proceed();
        } else {
            throw new RuntimeException("休息一会再来");
        }
        return obj;
    }
}
