package org.jboss.resteasy.core;

import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Can inject maps.
 */
public class MapFormInjector extends AbstractCollectionFormInjector<Map>
{

   private final StringParameterInjector keyInjector;

   /**
    * Constructor.
    */
   public MapFormInjector(Class collectionType, Class keyType, Class valueType, String prefix, ResteasyProviderFactory factory)
   {
      super(collectionType, valueType, prefix, Pattern.compile("^" + prefix + "\\[([0-9a-zA-Z_\\-\\.~]+)\\]"), factory);
      keyInjector = new StringParameterInjector(keyType, keyType, null, Form.class, null, null, new Annotation[0], factory);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Map createInstance(Class collectionType)
   {
      if (collectionType.isAssignableFrom(LinkedHashMap.class))
      {
         return new LinkedHashMap();
      }
      if (collectionType.isAssignableFrom(TreeMap.class))
      {
         return new TreeMap();
      }
      throw new RuntimeException("Unsupported collectionType: " + collectionType);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void addTo(Map collection, String key, Object value)
   {
      collection.put(keyInjector.extractValue(key), value);
   }
}