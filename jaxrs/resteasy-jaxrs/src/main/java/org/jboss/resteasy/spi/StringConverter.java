package org.jboss.resteasy.spi;

/**
 * Implement this interface and annotate your class with @Provider to provide marshalling and unmarshalling
 * of string-based, @HeaderParam, @MatrixParam, @QueryParam, and/or @PathParam injected values.
 * <p/>
 * Use this when toString(), valueOf, and/or constructor(String) can not satisfy your marshalling requirements.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public interface StringConverter<T>
{
   T fromString(String str);

   String toString(T value);
}
