package com.zfbots.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

public class RuntimeError extends Exception
{
  protected int errorNumber = 1;
  protected String source = "";
  protected Throwable nativeError = null;
  protected String overriden_description = null;

  public RuntimeError()
  {
    super("");
    Err.set(this);
  }

  public RuntimeError(int paramInt, String paramString1, String paramString2)
  {
    super(paramString2);
    Err.set(this);
    this.errorNumber = paramInt;
    this.source = paramString1;
  }

  public RuntimeError(Throwable paramThrowable)
  {
    super(paramThrowable.getLocalizedMessage());
    this.nativeError = paramThrowable;
  }

  public String getDescription()
  {
    if (this.overriden_description != null)
      return this.overriden_description;
    String str = getLocalizedMessage();
    if (str == null)
    {
      if (this.nativeError == null)
        return getClass().getName();
      return this.nativeError.getClass().getName();
    }
    return str;
  }

  public void setDescription(String paramString)
  {
    this.overriden_description = paramString;
  }

  public String toString()
  {
    String str = null;
    if (this.nativeError == null)
      str = super.toString();
    else
      str = this.nativeError.toString();
    str = str + "\r\n Error Number: " + this.errorNumber + " Source: " + this.source;
    if (this.overriden_description != null)
      str = str + "\r\n Description: " + this.overriden_description;
    return str;
  }

  public int getNumber()
  {
    if ((this.errorNumber == 1) && (this.nativeError != null))
      return mapThrowableToNumber(this.nativeError);
    return this.errorNumber;
  }

  public void setNumber(int paramInt)
  {
    this.errorNumber = paramInt;
    if (paramInt == 0)
      Err.clear();
  }

  public String getSource()
  {
    return this.source;
  }

  public void setSource(String paramString)
  {
    this.source = paramString;
  }

  public void printStackTrace(PrintStream paramPrintStream)
  {
    if (this.nativeError == null)
      super.printStackTrace(paramPrintStream);
    else
      this.nativeError.printStackTrace(paramPrintStream);
  }

  public void printStackTrace(PrintWriter paramPrintWriter)
  {
    if (this.nativeError == null)
      super.printStackTrace(paramPrintWriter);
    else
      this.nativeError.printStackTrace(paramPrintWriter);
  }

  private static int mapThrowableToNumber(Throwable paramThrowable)
  {
    int i = 1;
    if ((paramThrowable instanceof IllegalArgumentException))
      i = 5;
    else if ((paramThrowable instanceof OutOfMemoryError))
      i = 7;
    else if ((paramThrowable instanceof IndexOutOfBoundsException))
      i = 9;
    else if ((paramThrowable instanceof ArithmeticException))
      i = 11;
    else if ((paramThrowable instanceof ClassCastException))
      i = 13;
    else if ((paramThrowable instanceof NumberFormatException))
      i = 13;
    else if ((paramThrowable instanceof StackOverflowError))
      i = 28;
    else if ((paramThrowable instanceof InternalError))
      i = 51;
    else if ((paramThrowable instanceof FileNotFoundException))
      i = 53;
    else if ((paramThrowable instanceof IOException))
      i = 57;
    else if ((paramThrowable instanceof NullPointerException))
      i = 91;
    return i;
  }
}