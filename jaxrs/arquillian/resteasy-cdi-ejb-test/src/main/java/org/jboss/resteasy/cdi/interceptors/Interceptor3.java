package org.jboss.resteasy.cdi.interceptors;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 * Copyright Jul 21, 2012
 */
@Interceptor
@MethodBinding
public class Interceptor3
{
   @Inject private Logger log;
   
   @AroundInvoke
   public Object intercept(InvocationContext ctx) throws Exception
   {
      log.info("*** Intercepting call in Interceptor3.intercept()");
      VisitList.add(this);
      Object result = ctx.proceed();
      log.info("*** Back from intercepting call in Interceptor3.intercept()");
      return result;
   }
}

