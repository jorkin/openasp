package com.zfbots.util;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Random;

public class VB
{
  public static final String copyright = "Copyright 2013 zfbots Inc.";
  private static Random randomNumberGenerator;
  private static double lastRandom;

  public static final Variant Choose(int paramInt, Variant paramVariant1, Variant paramVariant2, Variant paramVariant3)
  {
    switch (paramInt)
    {
    case 1:
      return paramVariant1;
    case 2:
      return paramVariant2;
    case 3:
      return paramVariant3;
    }
    return null;
  }

  public static final Variant Choose(int paramInt, Variant paramVariant1, Variant paramVariant2, Variant paramVariant3, Variant paramVariant4, Variant paramVariant5, Variant paramVariant6, Variant paramVariant7, Variant paramVariant8, Variant paramVariant9, Variant paramVariant10)
  {
    switch (paramInt)
    {
    case 1:
      return paramVariant1;
    case 2:
      return paramVariant2;
    case 3:
      return paramVariant3;
    case 4:
      return paramVariant4;
    case 5:
      return paramVariant5;
    case 6:
      return paramVariant6;
    case 7:
      return paramVariant7;
    case 8:
      return paramVariant8;
    case 9:
      return paramVariant9;
    case 10:
      return paramVariant10;
    }
    return null;
  }

  public static final Variant Switch(boolean paramBoolean1, Variant paramVariant1, boolean paramBoolean2, Variant paramVariant2, boolean paramBoolean3, Variant paramVariant3)
  {
    if (paramBoolean1)
      return paramVariant1;
    if (paramBoolean2)
      return paramVariant2;
    if (paramBoolean3)
      return paramVariant3;
    return null;
  }

  public static final Variant Switch(boolean paramBoolean1, Variant paramVariant1, boolean paramBoolean2, Variant paramVariant2, boolean paramBoolean3, Variant paramVariant3, boolean paramBoolean4, Variant paramVariant4, boolean paramBoolean5, Variant paramVariant5, boolean paramBoolean6, Variant paramVariant6, boolean paramBoolean7, Variant paramVariant7)
  {
    if (paramBoolean1)
      return paramVariant1;
    if (paramBoolean2)
      return paramVariant2;
    if (paramBoolean3)
      return paramVariant3;
    if (paramBoolean4)
      return paramVariant4;
    if (paramBoolean5)
      return paramVariant5;
    if (paramBoolean6)
      return paramVariant6;
    if (paramBoolean7)
      return paramVariant7;
    return null;
  }

  public static final void Assert(boolean paramBoolean)
    throws RuntimeException
  {
    if (!paramBoolean)
      throw new RuntimeException("Assertion failed");
  }

  public static final void Assert(double paramDouble)
    throws RuntimeException
  {
    if (paramDouble == 0.0D)
      throw new RuntimeException("Assertion failed");
  }

  public static boolean matches(String paramString1, String paramString2)
  {
    return paramString1.matches(paramString2);
  }

  public static Variant nz(Variant paramVariant1, Variant paramVariant2)
  {
    if ((paramVariant1 == null) || (paramVariant1.isNull()))
    {
      if (paramVariant2 == null)
      {
        if ((paramVariant1 == null) || (paramVariant2.isNull()) || (paramVariant1.isNumeric()))
          return new Variant(0);
        return new Variant("");
      }
      return paramVariant2;
    }
    return paramVariant1;
  }

  public static Variant nz(Variant paramVariant)
  {
    return nz(paramVariant, null);
  }

  public static synchronized double rnd()
  {
    if (randomNumberGenerator == null)
      randomNumberGenerator = new Random();
    return VB.lastRandom = randomNumberGenerator.nextDouble();
  }

  public static synchronized double rnd(int paramInt)
  {
    if (randomNumberGenerator == null)
      randomNumberGenerator = new Random();
    if (paramInt == 0)
      return lastRandom;
    if (paramInt < 0)
      randomNumberGenerator.setSeed(Math.abs(paramInt));
    return rnd();
  }

  public static void randomize()
  {
    randomNumberGenerator = new Random();
  }

  public static void randomize(int paramInt)
  {
    randomNumberGenerator = new Random(paramInt);
  }

  public static double fix(double paramDouble)
  {
    if (paramDouble < 0.0D)
      return -Math.floor(-paramDouble);
    return Math.floor(paramDouble);
  }

  public static int upperBound(Variant paramVariant, int paramInt)
  {
    Object localObject = paramVariant.toObject();
    return upperBound(localObject, paramInt);
  }

  public static int upperBound(Object paramObject, int paramInt)
  {
    Object localObject = paramObject;
    while ((paramInt > 1) && (localObject != null))
    {
      localObject = Array.get(localObject, 0);
      paramInt--;
    }
    int i = Array.getLength(localObject);
    return i - 1;
  }

  private static String removeChar(String paramString, char paramChar)
  {
    StringBuffer localStringBuffer = new StringBuffer(paramString);
    for (int i = paramString.length() - 1; i >= 0; i--)
    {
      if (localStringBuffer.charAt(i) != paramChar)
        continue;
      localStringBuffer.deleteCharAt(i);
    }
    return localStringBuffer.toString();
  }

  public static String spc(int paramInt)
  {
    StringBuffer localStringBuffer = new StringBuffer();
    for (int i = 0; i < paramInt; i++)
      localStringBuffer.append(' ');
    return localStringBuffer.toString();
  }

  public static String tab(int paramInt)
  {
    return "\t";
  }

  public static double val(String paramString)
  {
    StringBuffer localStringBuffer = new StringBuffer();
    int i = paramString.length();
    for (int k = 0; k < i; k++)
    {
      int j = paramString.charAt(k);
      if (j == 32)
        continue;
      localStringBuffer.append(paramString.charAt(k));
    }
    double d = 0.0D;
    try
    {
      Number localNumber = Obj.numberFormat.parse(localStringBuffer.toString(), new ParsePosition(0));
      if (localNumber != null)
        return localNumber.doubleValue();
    }
    catch (Exception localException)
    {
    }
    return d;
  }

  public static Object copyArray(Object paramObject1, Object paramObject2)
  {
    if (paramObject1 == null)
      return paramObject2;
    int i = Math.min(Array.getLength(paramObject1), Array.getLength(paramObject2));
    System.arraycopy(paramObject1, 0, paramObject2, 0, i);
    return paramObject2;
  }

  public static Object copyArray(Object paramObject1, Object paramObject2, int paramInt)
  {
    if (paramObject1 == null)
      return paramObject2;
    if (paramInt == 1)
      return copyArray(paramObject1, paramObject2);
    try
    {
      int i = Math.min(Array.getLength(paramObject1), Array.getLength(paramObject2));
      for (int j = 0; j < i; j++)
        copyArray(Array.get(paramObject1, j), Array.get(paramObject2, j), paramInt - 1);
    }
    catch (Exception localException)
    {
    }
    return paramObject2;
  }

  private static Object getSharedInstance(Class paramClass)
  {
    if (paramClass == BigDecimal.class)
      return new BigDecimal(0.0D);
    if (paramClass == Date.class)
      return DateTime.EmptyDate;
    return null;
  }

  public static Object[] initArray(Object[] paramArrayOfObject, Class paramClass)
  {
    Object localObject = getSharedInstance(paramClass);
    int i = paramArrayOfObject.length;
    for (int j = 0; j < i; j++)
      try
      {
        paramArrayOfObject[j] = (localObject == null ? paramClass.newInstance() : localObject);
      }
      catch (Exception localException)
      {
      }
    return paramArrayOfObject;
  }

  public static Object[][] initArray(Object[][] paramArrayOfObject, Class paramClass)
  {
    Object localObject = getSharedInstance(paramClass);
    int i = paramArrayOfObject.length;
    int j = paramArrayOfObject[0].length;
    for (int k = 0; k < i; k++)
      for (int m = 0; m < j; m++)
        try
        {
          paramArrayOfObject[k][m] = (localObject == null ? paramClass.newInstance() : localObject);
        }
        catch (Exception localException)
        {
        }
    return paramArrayOfObject;
  }

  public static Object[][][] initArray(Object[][][] paramArrayOfObject, Class paramClass)
  {
    Object localObject = getSharedInstance(paramClass);
    int i = paramArrayOfObject.length;
    int j = paramArrayOfObject[0].length;
    int k = paramArrayOfObject[0][0].length;
    for (int m = 0; m < i; m++)
      for (int n = 0; n < j; n++)
        for (int i1 = 0; i1 < k; i1++)
          try
          {
            paramArrayOfObject[m][n][i1] = (localObject == null ? paramClass.newInstance() : localObject);
          }
          catch (Exception localException)
          {
          }
    return paramArrayOfObject;
  }

  public static Object[][][][] initArray(Object[][][][] paramArrayOfObject, Class paramClass)
  {
    Object localObject = getSharedInstance(paramClass);
    int i = paramArrayOfObject.length;
    int j = paramArrayOfObject[0].length;
    int k = paramArrayOfObject[0][0].length;
    int m = paramArrayOfObject[0][0][0].length;
    for (int n = 0; n < i; n++)
      for (int i1 = 0; i1 < j; i1++)
        for (int i2 = 0; i2 < k; i2++)
          for (int i3 = 0; i3 < m; i3++)
            try
            {
              paramArrayOfObject[n][i1][i2][i3] = (localObject == null ? paramClass.newInstance() : localObject);
            }
            catch (Exception localException)
            {
            }
    return paramArrayOfObject;
  }

  private static String stripQuotes(String paramString)
  {
    if ((paramString.charAt(0) == '"') && (paramString.charAt(paramString.length() - 1) == '"'))
      return paramString.substring(1, paramString.length() - 1);
    return paramString;
  }

  public static int shellExecute(String paramString1, String paramString2, String paramString3, String paramString4)
    throws Exception
  {
    String str2 = System.getProperty("os.name");
    String str1="";
    if (str2.startsWith("Windows"))
    {
      switch (str2.charAt(8))
      {
      case '2':
      case 'N':
      case 'X':
        str1 = "cmd /c start \"ShellExecute\" ";
        break;
      default:
        str1 = "start ";
      }
      String str3 = "";
      if ((paramString4 != null) && (paramString4.compareTo("") != 0))
      {
        str3 = str3 + stripQuotes(paramString4);
        if (!str3.endsWith("\\"))
          str3 = str3 + "\\";
      }
      str3 = str3 + stripQuotes(paramString2).trim();
      if (str3.indexOf(' ') > 0)
        str3 = '"' + str3 + '"';
      str1 = str1 + str3;
      if (paramString3 != null)
        str1 = str1 + " " + paramString3;
      Runtime.getRuntime().exec(str1);
    }
    else
    {
      throw new Exception("ShellExecute( " + paramString2 + " ) is only supported in Windows");
    }
    return 0;
  }

  /** @deprecated */
  public static double Val(String paramString)
  {
    return val(paramString);
  }

  /** @deprecated */
  public static double Fix(double paramDouble)
  {
    return fix(paramDouble);
  }

  /** @deprecated */
  public static int UBound(Variant paramVariant, int paramInt)
  {
    return upperBound(paramVariant, paramInt);
  }

  /** @deprecated */
  public static int UBound(Object paramObject, int paramInt)
  {
    return upperBound(paramObject, paramInt);
  }
}