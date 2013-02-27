package com.zfbots.util;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;

public class Obj
{
  static NumberFormat numberFormat = NumberFormat.getInstance();
  static NumberFormat percentFormat = NumberFormat.getPercentInstance();
  static NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
  static NumberFormat integerFormat = null;

  public static int compare(Object paramObject1, Object paramObject2)
  {
    return compare(paramObject1, paramObject2, false);
  }

  public static int compare(Object paramObject1, Object paramObject2, boolean paramBoolean)
  {
    if (paramObject1 == paramObject2)
      return 0;
    if (paramObject1 == null)
      return -1;
    if (paramObject2 == null)
      return 1;
    if ((paramObject1 instanceof Variant))
      paramObject1 = ((Variant)paramObject1).toObject();
    if ((paramObject2 instanceof Variant))
      paramObject2 = ((Variant)paramObject2).toObject();
    if (((paramObject1 instanceof Date)) || ((paramObject2 instanceof Date)) || ((paramObject1 instanceof Calendar)) || ((paramObject2 instanceof Calendar)))
    {
      Date localDate1 = toDate(paramObject1);
      Date localDate2 = toDate(paramObject2);
      if ((localDate1 != null) && (localDate2 != null))
      {
        long l1 = localDate1.getTime();
        long l2 = localDate2.getTime();
        return l1 < l2 ? -1 : l1 > l2 ? 1 : 0;
      }
      return -1;
    }
    if (((paramObject1 instanceof BigDecimal)) || ((paramObject2 instanceof BigDecimal)))
      return toDecimal(paramObject1).compareTo(toDecimal(paramObject2));
    if (((paramObject1 instanceof Number)) || ((paramObject2 instanceof Number)))
    {
      double d1 = toDouble(paramObject1);
      double d2 = toDouble(paramObject2);
      return d1 < d2 ? -1 : d1 > d2 ? 1 : 0;
    }
    if (((paramObject1 instanceof Boolean)) || ((paramObject2 instanceof Boolean)))
    {
      boolean bool1 = toBoolean(paramObject1);
      boolean bool2 = toBoolean(paramObject2);
      return bool1 ? 1 : bool1 == bool2 ? 0 : -1;
    }
    if (((paramObject1 instanceof String)) || ((paramObject2 instanceof String)))
    {
      if (paramBoolean)
        return toString(paramObject1).compareTo(toString(paramObject2));
      return toString(paramObject1).compareToIgnoreCase(toString(paramObject2));
    }
    try
    {
      if ((paramObject1 instanceof Comparable))
        return ((Comparable)paramObject1).compareTo(paramObject2);
      if ((paramObject2 instanceof Comparable))
        return -((Comparable)paramObject2).compareTo(paramObject1);
    }
    catch (Exception localException)
    {
    }
    if (paramObject1.equals(paramObject2))
      return 0;
    return 1;
  }

  public static Number toNumber(Object paramObject)
  {
    if ((paramObject instanceof Number))
      return (Number)paramObject;
    if ((paramObject instanceof Boolean))
      return ((Boolean)paramObject).booleanValue() ? new Integer(1) : new Integer(0);
    if ((paramObject instanceof Date))
      return new Long(((Date)paramObject).getTime());
    if (paramObject == null)
      return null;
    String str;
    if ((paramObject instanceof String))
      str = ((String)paramObject).trim();
    else
      str = paramObject.toString();
    try
    {
      return parseNumber(str);
    }
    catch (ParseException localParseException)
    {
    }
    return null;
  }

  public static int toInt(Object paramObject)
  {
    if ((paramObject instanceof Number))
      return (int)Math.rint(((Number)paramObject).doubleValue());
    if ((paramObject instanceof Boolean))
      return ((Boolean)paramObject).booleanValue() ? -1 : 0;
    if ((paramObject instanceof Date))
      return (int)Math.rint(DateTime.toDouble((Date)paramObject));
    if ((paramObject instanceof Calendar))
      return (int)Math.rint(DateTime.toDouble(((Calendar)paramObject).getTime()));
    if (paramObject == null)
      return 0;
    String str;
    if ((paramObject instanceof String))
      str = trimNumericString((String)paramObject);
    else
      str = paramObject.toString();
    try
    {
      return Integer.parseInt(str);
    }
    catch (NumberFormatException localNumberFormatException)
    {
    }
    return (int)makeDouble(str);
  }

  public static long toLong(Object paramObject)
  {
    if ((paramObject instanceof Number))
      return ((Number)paramObject).longValue();
    if ((paramObject instanceof Boolean))
      return ((Boolean)paramObject).booleanValue() ? -1L : 0L;
    if ((paramObject instanceof Date))
      return Math.round(DateTime.toDouble((Date)paramObject));
    if ((paramObject instanceof Calendar))
      return Math.round(DateTime.toDouble(((Calendar)paramObject).getTime()));
    if (paramObject == null)
      return 0L;
    String str;
    if ((paramObject instanceof String))
      str = trimNumericString((String)paramObject);
    else
      str = paramObject.toString();
    try
    {
      return Long.parseLong(str);
    }
    catch (NumberFormatException localNumberFormatException)
    {
    }
    return (long)makeDouble(str);
  }

  public static double toDouble(Object paramObject)
  {
    if ((paramObject instanceof Number))
      return ((Number)paramObject).doubleValue();
    if ((paramObject instanceof Date))
      return DateTime.toDouble((Date)paramObject);
    if ((paramObject instanceof Calendar))
      return DateTime.toDouble(((Calendar)paramObject).getTime());
    if ((paramObject instanceof Boolean))
      return ((Boolean)paramObject).booleanValue() ? -1.0D : 0.0D;
    if (paramObject == null)
      return 0.0D;
    String str;
    if ((paramObject instanceof String))
      str = trimNumericString((String)paramObject);
    else
      str = paramObject.toString();
    return makeDouble(str);
  }

  public static BigDecimal toDecimal(Object paramObject)
  {
    try
    {
      if ((paramObject instanceof BigDecimal))
        return (BigDecimal)paramObject;
      if ((paramObject instanceof Number))
      {
        if ((paramObject instanceof Long))
          return BigDecimal.valueOf(((Number)paramObject).longValue());
        return new BigDecimal(((Number)paramObject).doubleValue());
      }
      if (paramObject == null)
        return new BigDecimal(0.0D);
      String str;
      if ((paramObject instanceof String))
        str = trimNumericString((String)paramObject);
      else
        str = paramObject.toString();
      if (str.length() > 0)
        return new BigDecimal(str);
    }
    catch (NumberFormatException localNumberFormatException)
    {
    }
    return new BigDecimal(0.0D);
  }

  public static boolean toBoolean(Object paramObject)
  {
    if (paramObject == null)
      return false;
    if ((paramObject instanceof String))
    {
      String str = ((String)paramObject).trim();
      if (str.length() == 0)
        return false;
      if (str.equalsIgnoreCase("true"))
        return true;
    }
    double d = toDouble(paramObject);
    return d != 0.0D;
  }

  public static Date toDate(Object paramObject)
  {
    try
    {
      if ((paramObject instanceof Date))
        return (Date)paramObject;
      if ((paramObject instanceof Calendar))
        return ((Calendar)paramObject).getTime();
      if ((paramObject instanceof Number))
        return DateTime.toDate(((Number)paramObject).doubleValue());
      if ((paramObject instanceof String))
        return DateTime.toDate((String)paramObject);
    }
    catch (Exception localException)
    {
    }
    return null;
  }

  public static Date toDateNew(Object paramObject)
  {
    Date localDate = toDate(paramObject);
    if (localDate == null)
      return new Date();
    return localDate;
  }

  public static String toString(Object paramObject)
  {
    if (paramObject == null)
      return "";
    if ((paramObject instanceof String))
      return (String)paramObject;
    if ((paramObject instanceof Boolean))
      return ((Boolean)paramObject).booleanValue() ? "true" : "false";
    if ((paramObject instanceof byte[]))
      return new String((byte[])paramObject);
    if ((paramObject instanceof Date))
      return DateTime.format((Date)paramObject);
    if ((paramObject instanceof Calendar))
      return DateTime.format(((Calendar)paramObject).getTime());
    if ((paramObject instanceof InputStream))
      try
      {
        InputStream localInputStream = (InputStream)paramObject;
        byte[] arrayOfByte = new byte[localInputStream.available()];
        localInputStream.read(arrayOfByte);
        return new String(arrayOfByte);
      }
      catch (Exception localException1)
      {
        System.out.println("Obj.toString(InputStream): " + localException1);
      }
    if ((paramObject instanceof Reader))
      try
      {
        Reader localReader = (Reader)paramObject;
        char[] arrayOfChar = new char[64];
        StringBuffer localStringBuffer = new StringBuffer();
        int i;
        while ((i = localReader.read(arrayOfChar)) > 0)
          localStringBuffer.append(arrayOfChar, 0, i);
        return localStringBuffer.toString();
      }
      catch (Exception localException2)
      {
        System.out.println("Obj.toString(Reader): " + localException2);
      }
    return paramObject.toString();
  }

  static String trimNumericString(String paramString)
  {
    paramString = paramString.trim();
    if ((paramString.length() > 0) && (paramString.charAt(0) == '$'))
      paramString = paramString.substring(1).trim();
    paramString = Strings.replace(paramString, ",", "");
    return paramString;
  }

  private static double makeDouble(String paramString)
  {
    try
    {
      return Double.parseDouble(paramString);
    }
    catch (Throwable localThrowable)
    {
      return Double.valueOf(paramString).doubleValue();
    }
    //return 0.0D;
  }

  public static final Number parseNumber(String paramString)
    throws ParseException
  {
    if ((paramString == null) || (paramString.length() == 0))
      return null;
    ParsePosition localParsePosition;
    try
    {
      return new Integer(paramString);
    }
    catch (NumberFormatException localNumberException)
    {
      NumberFormat localNumberFormat = null;
      localParsePosition = new ParsePosition(0);
      int i = paramString.length();
      for (int j = 0; j < 4; j++)
      {
        switch (j)
        {
        case 0:
          localNumberFormat = integerFormat;
          break;
        case 1:
          localNumberFormat = numberFormat;
          break;
        case 2:
          localNumberFormat = percentFormat;
          break;
        case 3:
          localNumberFormat = currencyFormat;
        }
        localParsePosition.setIndex(0);
        Number localNumber = localNumberFormat.parse(paramString, localParsePosition);
        if ((localNumber != null) && (localParsePosition.getIndex() >= i))
          return localNumber;
      }
    }
    throw new ParseException("Unparseable number: \"" + paramString + "\"", localParsePosition.getIndex());
  }

  public static final Object parseObject(Object paramObject)
  {
    if (paramObject == null)
      return null;
    if (((paramObject instanceof Number)) || ((paramObject instanceof Boolean)) || ((paramObject instanceof Date)))
      return paramObject;
    String str;
    if ((paramObject instanceof String))
      str = ((String)paramObject).trim();
    else
      str = paramObject.toString();
    try
    {
      return parseNumber(str);
    }
    catch (ParseException localParseException2)
    {
      if (str.equalsIgnoreCase("false"))
        return new Boolean(false);
      if (str.equalsIgnoreCase("true"))
        return new Boolean(true);
      try
      {
        return DateTime.toDate(str);
      }
      catch (ParseException localParseException3)
      {
      }
    }
    return paramObject;
  }

  static
  {
    try
    {
      integerFormat = NumberFormat.getIntegerInstance();
    }
    catch (Throwable localThrowable)
    {
      integerFormat = NumberFormat.getInstance();
    }
  }
}