package org.jboss.resteasy.test.async.callback;

import javax.ws.rs.container.CompletionCallback;

public class SettingCompletionCallback implements CompletionCallback {

   private static String throwableName;
   public static final String NULL = "NULL";
   public static final String NONAME = "No name has been set yet";

   @Override
   public void onComplete(Throwable throwable) {
      throwableName = throwable == null ? NULL : throwable.getClass()
              .getName();
   }

   public static final String getLastThrowableName() {
      return throwableName;
   }

   public static final void resetLastThrowableName() {
      throwableName = NONAME;
   }

}
