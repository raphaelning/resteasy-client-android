package org.jboss.resteasy.cdi;

import javax.enterprise.inject.spi.BeanManager;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Utility methods for detecting CDI scopes and JAX-RS components
 * @author Jozef Hartinger
 *
 */
public class Utils
{
   /**
    * Finds out if a given class is decorated with JAX-RS annotations.
    * Interfaces of the class are not scanned for JAX-RS annotations.
    * 
    * @param clazz
    * @return true if a given interface has @Path annotation or if any of its
    *         methods is decorated with @Path annotation or a request method
    *         designator.
    */
   public static boolean isJaxrsAnnotatedClass(Class<?> clazz)
   {
      if (clazz.isAnnotationPresent(Path.class))
      {
         return true;
      }
      if (clazz.isAnnotationPresent(Provider.class))
      {
         return true;
      }
      for (Method method : clazz.getMethods())
      {
         if (method.isAnnotationPresent(Path.class))
         {
            return true;
         }
         for (Annotation annotation : method.getAnnotations())
         {
            if (annotation.annotationType().isAnnotationPresent(HttpMethod.class))
            {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Returns true if and only if the given class is a JAX-RS root resource or a
    * sub-resource. The class itself as well as its interfaces are scanned for
    * JAX-RS annotations.
    */
   public static boolean isJaxrsResource(Class<?> clazz)
   {
      if (isJaxrsAnnotatedClass(clazz))
      {
         return true;
      }
      for (Class<?> intf : clazz.getInterfaces())
      {
         if (isJaxrsAnnotatedClass(intf))
         {
            return true;
         }
      }
      return false;
   }

   /**
    * Find out if a given class is a JAX-RS component
    * 
    * @return true if and only if a give class is a JAX-RS resource, provider or
    *         javax.ws.rs.core.Application subclass.
    */
   public static boolean isJaxrsComponent(Class<?> clazz)
   {
      return ((clazz.isAnnotationPresent(Provider.class)) || (isJaxrsResource(clazz)) || (Application.class.isAssignableFrom(clazz)));
   }

   /**
    * Find out if a given class has is explicitly bound to a scope.
    * 
    * @return true if and only if a given class is annotated with a scope
    *         annotation or with a stereotype which (transitively) declares a
    *         scope
    */
   public static boolean isScopeDefined(Class<?> clazz, BeanManager manager)
   {
      for (Annotation annotation : clazz.getAnnotations())
      {
         if (manager.isScope(annotation.annotationType()))
         {
            return true;
         }
         if (manager.isStereotype(annotation.annotationType()))
         {
            if (isScopeDefined(annotation.annotationType(), manager))
            {
               return true;
            }
         }
      }
      return false;
   }
}
