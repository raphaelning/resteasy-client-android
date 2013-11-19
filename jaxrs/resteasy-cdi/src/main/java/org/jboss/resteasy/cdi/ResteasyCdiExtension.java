package org.jboss.resteasy.cdi;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.util.GetRestful;

import javax.decorator.Decorator;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.util.AnnotationLiteral;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * This Extension handles default scopes for discovered JAX-RS components. It
 * also observes ProcessInjectionTarget event and wraps InjectionTargets
 * representing JAX-RS components within JaxrsInjectionTarget. Furthermore, it
 * builds the sessionBeanInterface map which maps Session Bean classes to a
 * local interface. This map is used in CdiInjectorFactory during lookup of
 * Sesion Bean JAX-RS components.
 *
 * @author Jozef Hartinger
 *
 */
public class ResteasyCdiExtension implements Extension
{
   private BeanManager beanManager;
   private static final String JAVAX_EJB_STATELESS = "javax.ejb.Stateless";
   private static final String JAVAX_EJB_SINGLETON = "javax.ejb.Singleton";

   private final Logger log = Logger.getLogger(ResteasyCdiExtension.class);

   // Scope literals
   public static final Annotation requestScopedLiteral = new AnnotationLiteral<RequestScoped>()
   {
      private static final long serialVersionUID = 3381824686081435817L;
   };
   public static final Annotation applicationScopedLiteral = new AnnotationLiteral<ApplicationScoped>()
   {
      private static final long serialVersionUID = -8211157243671012820L;
   };

   private Map<Class<?>, Type> sessionBeanInterface = new HashMap<Class<?>, Type>();

   /**
    * Obtain BeanManager reference for future use.
    */
   public void observeBeforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager beanManager)
   {
      this.beanManager = beanManager;
   }

   /**
    * Set a default scope for each CDI bean which is a JAX-RS Resource, Provider
    * or Application subclass.
    *
    */
   public <T> void observeResources(@Observes ProcessAnnotatedType<T> event, BeanManager beanManager)
   {
       if (this.beanManager == null) {
           // this may happen if Solder Config receives BBD first
           this.beanManager = beanManager;
       }

      AnnotatedType<T> type = event.getAnnotatedType();

      if (!type.getJavaClass().isInterface())
      {
         for (Annotation annotation : type.getAnnotations())
         {
            Class<?> annotationType = annotation.annotationType();
            if (annotationType.getName().equals(JAVAX_EJB_STATELESS) || annotationType.getName().equals(JAVAX_EJB_SINGLETON))
            {
               log.debug("Bean {0} is a SLSB or Singleton. Leaving scope unmodified.", type.getJavaClass());
               return; // Do not modify scopes of SLSBs and Singletons
            }
         }
         if (type.isAnnotationPresent(Provider.class))
         {
            log.debug("Discovered CDI bean which is a JAX-RS provider {0}.", type.getJavaClass().getCanonicalName());
            event.setAnnotatedType(wrapAnnotatedType(type, applicationScopedLiteral));
         }
         else if (GetRestful.isRootResource(type.getJavaClass()) && !type.isAnnotationPresent(Decorator.class))
         {
            log.debug("Discovered CDI bean which is a JAX-RS resource {0}.", type.getJavaClass().getCanonicalName());
            event.setAnnotatedType(wrapAnnotatedType(type, requestScopedLiteral));
         }
         else if (Application.class.isAssignableFrom(type.getJavaClass()))
         {
            log.debug("Discovered CDI bean which is javax.ws.rs.core.Application subclass {0}.", type.getJavaClass().getCanonicalName());
            event.setAnnotatedType(wrapAnnotatedType(type, applicationScopedLiteral));
         }
      }
   }

   protected <T> AnnotatedType<T> wrapAnnotatedType(AnnotatedType<T> type, Annotation scope)
   {
      if (Utils.isScopeDefined(type.getJavaClass(), beanManager))
      {
         log.debug("Bean {0} has a scope defined.", type.getJavaClass());
         return type; // leave it as it is
      }
      else
      {
         log.debug("Bean {0} does not have the scope defined. Binding to {1}.", type.getJavaClass(), scope);
         return new JaxrsAnnotatedType<T>(type, scope);
      }
   }

   /**
    * Wrap InjectionTarget of JAX-RS components within JaxrsInjectionTarget
    * which takes care of JAX-RS property injection.
    */
   public <T> void observeInjectionTarget(@Observes ProcessInjectionTarget<T> event)
   {
      if (event.getAnnotatedType() == null)
      { // check for resin's bug http://bugs.caucho.com/view.php?id=3967
         log.warn("ProcessInjectionTarget.getAnnotatedType() returned null. As a result, JAX-RS property injection will not work.");
         return;
      }

      if (Utils.isJaxrsComponent(event.getAnnotatedType().getJavaClass()))
      {
         event.setInjectionTarget(wrapInjectionTarget(event));
      }
   }

   protected <T> InjectionTarget<T> wrapInjectionTarget(ProcessInjectionTarget<T> event)
   {
      return new JaxrsInjectionTarget<T>(event.getInjectionTarget(), event.getAnnotatedType().getJavaClass());
   }

   /**
    * Observes ProcessSessionBean events and creates a (Bean class -> Local
    * interface) map for Session beans with local interfaces. This map is
    * necessary since RESTEasy identifies a bean class as JAX-RS components
    * while CDI requires a local interface to be used for lookup.
    */
   public <T> void observeSessionBeans(@Observes ProcessSessionBean<T> event)
   {
      Bean<Object> sessionBean = event.getBean();

      if (Utils.isJaxrsComponent(sessionBean.getBeanClass()))
      {
         addSessionBeanInterface(sessionBean);
      }
   }

   private void addSessionBeanInterface(Bean<?> bean)
   {
      for (Type type : bean.getTypes())
      {
         if ((type instanceof Class<?>) && ((Class<?>) type).isInterface())
         {
            Class<?> clazz = (Class<?>) type;
            if (Utils.isJaxrsAnnotatedClass(clazz))
            {
               sessionBeanInterface.put(bean.getBeanClass(), type);
               log.debug("{0} will be used for {1} lookup", type, bean.getBeanClass());
               return;
            }
         }
      }
      log.debug("No lookup interface found for {0}", bean.getBeanClass());
   }

   public Map<Class<?>, Type> getSessionBeanInterface()
   {
      return sessionBeanInterface;
   }
}
