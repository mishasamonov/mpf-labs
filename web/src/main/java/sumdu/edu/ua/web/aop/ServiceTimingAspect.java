package sumdu.edu.ua.web.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Lab 10 (extension): execution time of every method of the core service layer.
 * The outermost aspect, so the measured time includes the logging aspect and security checks.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceTimingAspect {
  private static final Logger log = LoggerFactory.getLogger(ServiceTimingAspect.class);

  @Around("execution(* sumdu.edu.ua.core.service..*(..))")
  public Object measure(ProceedingJoinPoint pjp) throws Throwable {
    String method = pjp.getSignature().getDeclaringType().getSimpleName() + "."
        + pjp.getSignature().getName();
    long start = System.nanoTime();
    boolean failed = true;
    try {
      Object result = pjp.proceed();
      failed = false;
      return result;
    } finally {
      long micros = (System.nanoTime() - start) / 1_000;
      log.info("[timing] {} {} in {}.{} ms", method, failed ? "failed" : "took",
          micros / 1000, String.format("%03d", micros % 1000));
    }
  }
}
