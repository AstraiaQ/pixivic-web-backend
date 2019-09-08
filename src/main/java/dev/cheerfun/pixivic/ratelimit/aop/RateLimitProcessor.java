package dev.cheerfun.pixivic.ratelimit.aop;

import com.google.common.base.Preconditions;
import dev.cheerfun.pixivic.ratelimit.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author echo huang
 * @version 1.0
 * @date 2019-07-13 15:12
 * @description 限流监听器
 */
@Aspect
@Component
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RateLimitProcessor {


    @Resource(name = "limitRedisTemplate")
    private RedisTemplate<String, Integer> redisTemplate;

    /**
     * 令牌桶是否存在
     */
    private static final AtomicBoolean BUCKET_IS_EXSITS = new AtomicBoolean(false);

    @Pointcut(value = "@annotation(dev.cheerfun.pixivic.ratelimit.annotation.RateLimit)")
    public void pointCut() {
    }

    @Around(value = "pointCut()")
    public Object handleRateLimiter(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj;
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
            Boolean setResult = tokenBucket.setIfAbsent(userId, 1, sec, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(setResult)) {
                maxLimit = tokenBucket.get(userId);
                Preconditions.checkNotNull(maxLimit, String.format("userId:%s的令牌个数为空", maxLimit));
                if (limitNum > maxLimit) {
                    tokenBucket.increment(userId);
                } else {
                    throw new RuntimeException("休息一会再来");
                }
            }
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
