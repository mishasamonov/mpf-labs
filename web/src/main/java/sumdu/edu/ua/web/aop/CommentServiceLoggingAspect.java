package sumdu.edu.ua.web.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Lab 10: logs arguments, duration and failures of CommentService.delete(...). */
@Aspect
@Component
public class CommentServiceLoggingAspect {

  private static final Logger log =
      LoggerFactory.getLogger(CommentServiceLoggingAspect.class);

  @Around("execution(* sumdu.edu.ua.core.service.CommentService.delete(..))")
  public Object logDeleteCall(ProceedingJoinPoint pjp) throws Throwable {
    long start = System.currentTimeMillis();

    Object[] args = pjp.getArgs();
    Long bookId = (Long) args[0];
    Long commentId = (Long) args[1];

    log.info("Calling CommentService.delete(bookId={}, commentId={})",
        bookId, commentId);

    try {
      Object result = pjp.proceed();
      long time = System.currentTimeMillis() - start;
      log.info("CommentService.delete(bookId={}, commentId={}) finished in {} ms",
          bookId, commentId, time);
      return result;
    } catch (Exception ex) {
      long time = System.currentTimeMillis() - start;
      log.warn("CommentService.delete(bookId={}, commentId={}) failed in {} ms: {}",
          bookId, commentId, time, ex.getMessage());
      throw ex; // важливо: не ковтати виняток
    }
  }
}
