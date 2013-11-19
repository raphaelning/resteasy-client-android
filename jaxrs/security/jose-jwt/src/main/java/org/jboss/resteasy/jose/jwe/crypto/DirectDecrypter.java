package org.jboss.resteasy.jose.jwe.crypto;


import org.jboss.resteasy.jose.Base64Url;
import org.jboss.resteasy.jose.jwe.Algorithm;
import org.jboss.resteasy.jose.jwe.EncryptionMethod;
import org.jboss.resteasy.jose.jwe.JWEHeader;

import javax.crypto.SecretKey;
import java.nio.charset.Charset;


/**
 * Direct decrypter with a
 * shared symmetric key. This class is thread-safe.
 * <p/>
 * <p>Supports the following JWE algorithms:
 * <p/>
 * <ul>
 * <li>DIR
 * </ul>
 * <p/>
 * <p>Supports the following encryption methods:
 * <p/>
 * <ul>
 * <li>A128CBC_HS256
 * <li>A256CBC_HS512
 * <li>A128GCM
 * <li>A256GCM
 * </ul>
 *
 * @author Vladimir Dzhuvinov
 * @version $version$ (2013-05-29)
 */
public class DirectDecrypter
{
   public static byte[] decrypt(final SecretKey key,
                                final JWEHeader readOnlyJWEHeader,
                                final String encodedHeader,
                                final String encryptedKey,
                                final String encodedIv,
                                final String encodedCipherText,
                                final String encodedAuthTag
                               )
   {

      // Validate required JWE parts
      if (encryptedKey != null)
      {

         throw new RuntimeException("Unexpected encrypted key, must be omitted");
      }

      if (encodedIv == null)
      {

         throw new RuntimeException("The initialization vector (IV) must not be null");
      }

      if (encodedAuthTag == null)
      {

         throw new RuntimeException("The authentication tag must not be null");
      }


      Algorithm alg = readOnlyJWEHeader.getAlgorithm();

      if (!alg.equals(Algorithm.dir))
      {

         throw new RuntimeException("Unsupported algorithm, must be \"dir\"");
      }

      // Compose the AAD
      byte[] aad =  encodedHeader.getBytes(Charset.forName("UTF-8"));
      byte[] iv = Base64Url.decode(encodedIv);
      byte[] cipherText = Base64Url.decode(encodedCipherText);
      byte[] authTag = Base64Url.decode(encodedAuthTag);

      // Decrypt the cipher text according to the JWE enc
      EncryptionMethod enc = readOnlyJWEHeader.getEncryptionMethod();

      byte[] plainText;

      if (enc.equals(EncryptionMethod.A128CBC_HS256) || enc.equals(EncryptionMethod.A256CBC_HS512))
      {

         plainText = AESCBC.decryptAuthenticated(key, iv, cipherText, aad, authTag);

      }
      else if (enc.equals(EncryptionMethod.A128GCM) || enc.equals(EncryptionMethod.A256GCM))
      {

         plainText = AESGCM.decrypt(key, iv, cipherText, aad, authTag);

      }
      else
      {

         throw new RuntimeException("Unsupported encryption method, must be A128CBC_HS256, A256CBC_HS512, A128GCM or A128GCM");
      }


      // Apply decompression if requested
      return DeflateHelper.applyDecompression(readOnlyJWEHeader.getCompressionAlgorithm(), plainText);
   }
}

