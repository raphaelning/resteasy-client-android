package org.jboss.resteasy.spi.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MethodParameter extends Parameter
{
   protected Annotation[] annotations = {};
   protected ResourceLocator locator;

   protected MethodParameter(ResourceLocator locator, Class<?> type, Type genericType,Annotation[] annotations)
   {
      super(locator.getResourceClass(), type, genericType);
      this.annotations = annotations;
      this.locator = locator;
   }

   @Override
   public AccessibleObject getAccessibleObject()
   {
      return locator.method;
   }

   public Annotation[] getAnnotations()
   {
      return annotations;
   }
}
