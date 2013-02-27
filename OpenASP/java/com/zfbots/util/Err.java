package com.zfbots.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Err
{
  private static RuntimeError error = null;
  private static RuntimeError emptyError = new RuntimeError(0, null, "no error");
  private static ThreadLocal threadError = null;
  private static boolean usingThreadLocal = false;

  public static RuntimeError getError()
  {
    RuntimeError localRuntimeError = error;
    if (usingThreadLocal)
      localRuntimeError = (RuntimeError)threadError.get();
    return localRuntimeError == null ? emptyError : localRuntimeError;
  }

  public static void set(Exception paramException)
  {
    RuntimeError localRuntimeError = null;
    if ((paramException instanceof RuntimeError))
      localRuntimeError = (RuntimeError)paramException;
    else if (paramException != null)
      localRuntimeError = new RuntimeError(paramException);
    if (usingThreadLocal)
      threadError.set(localRuntimeError);
    else
      error = localRuntimeError;
  }

  public static void set(Exception paramException, String paramString)
  {
    set(paramException);
    if (paramString != null)
    {
      StringWriter localStringWriter = new StringWriter();
      paramException.printStackTrace(new PrintWriter(localStringWriter));
      String str = localStringWriter.toString();
      int i = str.indexOf(paramString);
      if ((i < 0) || ((i = str.indexOf('\n', i)) < 0))
        System.out.println(str);
      else
        System.out.println(str.substring(0, i));
    }
  }

  public static String getDescription()
  {
    RuntimeError localRuntimeError = getError();
    if (localRuntimeError == null)
      return "";
    return localRuntimeError.getDescription();
  }

  public static void raise(int paramInt1, String paramString1, String paramString2, String paramString3, int paramInt2)
    throws RuntimeError
  {
    throw new RuntimeError(paramInt1, paramString1, paramString2);
  }

  public static void raise(int paramInt, String paramString1, String paramString2)
    throws RuntimeError
  {
    throw new RuntimeError(paramInt, paramString1, paramString2);
  }

  public static void clear()
  {
    set(null);
  }

  public static void printStackTrace()
  {
    try
    {
      throw new Throwable("stack trace");
    }
    catch (Throwable localThrowable)
    {
      localThrowable.printStackTrace();
    }
  }

  static
  {
    try
    {
      threadError = new ThreadLocal();
      usingThreadLocal = true;
    }
    catch (Throwable localThrowable)
    {
    }
  }
}