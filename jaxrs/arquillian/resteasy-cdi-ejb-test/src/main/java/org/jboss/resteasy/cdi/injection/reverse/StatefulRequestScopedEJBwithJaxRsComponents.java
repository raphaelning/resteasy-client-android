package org.jboss.resteasy.cdi.injection.reverse;

import static org.jboss.resteasy.cdi.injection.reverse.ReverseInjectionResource.NON_CONTEXTUAL;

import java.util.HashMap;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.jboss.resteasy.cdi.injection.BookReader;
import org.jboss.resteasy.cdi.injection.BookResource;
import org.jboss.resteasy.cdi.injection.BookWriter;

/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 * Copyright May 24, 2012
 */
@Stateful
@RequestScoped
public class StatefulRequestScopedEJBwithJaxRsComponents implements StatefulRequestScopedEJBwithJaxRsComponentsInterface
{
   private static HashMap<String, HashMap<String,Object>> store = new HashMap<String, HashMap<String,Object>>();
   private static int constructions;
   private static int destructions;
   
   @Inject int secret;
   
   static public int getConstructions()
   {
      return constructions;
   }
   
   static public int getDestructions()
   {
      return destructions;
   }
   
   @PostConstruct
   public void postConstruct()
   {
      constructions++;
      log.info(this + " secret: " + secret);
   }
   
   @PreDestroy
   public void preDestroy()
   {
      destructions++;
   }
   
   @Inject private Logger log;
   @Inject private BookResource resource;
   @Inject private BookReader reader;
   @Inject private BookWriter writer;
   
   @Override
   public void setUp(String key)
   {
      log.info("entering StatefulRequestScopedEJBwithJaxRsComponents.setUp()");
      HashMap<String, Object> substore = new HashMap<String, Object>();
      substore.put("secret", resource.theSecret());
      substore.put(BookResource.BOOK_READER, reader);
      substore.put(BookResource.BOOK_WRITER, writer);
      store.put(key, substore);
   }
   
   /**
    * This is a SFSB.  See discussion in EJBHolder.test().
    * 
    * If NON_CONTEXTUAL.equals(key), then this bean was obtained from JNDI, and
    * it is not a CDI contextual object.  It follows that it is not request
    * scoped, which means it will not be recreated, with new injections, upon a
    * second invocation.
    * 
    * Otherwise, it will be recreated, and CDI will redo the injections.
    */
   @Override
   public boolean test(String key)
   {
      log.info("entering StatefulRequestScopedEJBwithJaxRsComponents.test(" + key + ")");
      HashMap<String, Object> substore = store.get(key);
      int savedSecret = Integer.class.cast(substore.get("secret"));
      log.info("stored resource secret = resource secret: " + (savedSecret == resource.theSecret()));
      log.info("stored reader = reader:                   " + (substore.get(BookResource.BOOK_READER) == reader));
      log.info("stored writer = writer:                   " + (substore.get(BookResource.BOOK_WRITER) == writer));
      
      boolean result = true;
      result &= reader == substore.get(BookResource.BOOK_READER); // application scoped
      result &= writer == substore.get(BookResource.BOOK_WRITER); // application scoped
      if (NON_CONTEXTUAL.equals(key))
      {
         result &= resource.theSecret() == savedSecret;           // request scope not applicable
      }
      else
      {
         result &= resource.theSecret() != savedSecret;           // request scoped
      }
      return result;
   }
   
   @Override
   public Class<?> theClass()
   {
      return StatefulRequestScopedEJBwithJaxRsComponents.class;
   }
   
   @Override
   public boolean theSame(EJBInterface ejb)
   {
      if (ejb == null) return false;
      Class<?> c = ejb.theClass();
      if (!StatefulRequestScopedEJBwithJaxRsComponents.class.equals(c))
      {
         log.info(ejb + " not instanceof StatefulRequestScopedEJBwithJaxRsComponents: " + c);
         return false;
      }
      log.info(this.secret + " " + ejb.theSecret());
      return this.secret == ejb.theSecret();
   }
   
   @Override
   public int theSecret()
   {
      return secret;
   }
}

