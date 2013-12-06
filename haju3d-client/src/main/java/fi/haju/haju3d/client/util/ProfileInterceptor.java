package fi.haju.haju3d.client.util;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;

public class ProfileInterceptor implements MethodInterceptor {

  Logger log = Logger.getLogger(getClass());

  public Object invoke(final MethodInvocation invocation) throws Throwable {
    String method = invocation.getMethod().getName();
    long start = System.currentTimeMillis();
    Object result = invocation.proceed();
    long end = System.currentTimeMillis();
    log.info("Method " + method + " took " + (end - start) + " ms");
    return result;
  }

}