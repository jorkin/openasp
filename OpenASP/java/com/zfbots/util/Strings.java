package com.zfbots.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

public class Strings
{
  public static boolean isNull(String paramString)
  {
    return (paramString == null) || (paramString.length() == 0);
  }

  public static int asc(String paramString)
  {
    return paramString.charAt(0);
  }

  public static String mid(String paramString, int paramInt1, int paramInt2)
  {
    if (paramString == null)
      return "";
    int i = paramString.length();
    if (paramInt1 > i)
      return "";
    if (paramInt1 + paramInt2 > i)
      paramInt2 = i - paramInt1 + 1;
    return paramString.substring(paramInt1 - 1, paramInt1 - 1 + paramInt2);
  }

  public static String mid(String paramString, int paramInt)
  {
    if (paramString == null)
      return "";
    int i = paramString.length();
    if (paramInt > i)
      return "";
    return paramString.substring(paramInt - 1, i);
  }

  public static String mid(String paramString1, int paramInt1, int paramInt2, String paramString2)
  {
    if (paramString1 == null)
      return "";
    StringBuffer localStringBuffer = new StringBuffer(paramString1);
    int i = paramString1.length();
    if ((paramInt1 + paramInt2 > i) || (paramInt2 < 0))
      paramInt2 = i - paramInt1 + 1;
    paramInt1--;
    int j = Math.min(paramInt2, paramString2.length());
    int k = paramInt1;
    for (int m = 0; m < j; m++)
    {
      localStringBuffer.setCharAt(k, paramString2.charAt(m));
      k++;
    }
    return localStringBuffer.toString();
  }

  public static String replaceMid(String paramString1, int paramInt1, int paramInt2, String paramString2)
  {
    if (paramString1 == null)
      return "";
    int i = paramString1.length();
    return left(paramString1, paramInt1 - 1) + paramString2 + right(paramString1, i - paramInt1 - paramInt2 + 1);
  }

  public static String replace(String paramString1, String paramString2, String paramString3)
  {
    return replace(paramString1, paramString2, paramString3, 1, -1);
  }

  public static String replace(String paramString1, String paramString2, String paramString3, int paramInt1, int paramInt2)
  {
    if (paramString1 == null)
      return "";
    StringBuffer localStringBuffer = new StringBuffer(paramString1.length());
    int i = i = paramString2.length();
    int j = paramInt1 - 1;
    int k = paramString1.indexOf(paramString2, j);
    while ((k >= 0) && (paramInt2 != 0))
    {
      localStringBuffer.append(paramString1.substring(j, k));
      localStringBuffer.append(paramString3);
      j = k + i;
      k = paramString1.indexOf(paramString2, j);
      paramInt2--;
    }
    localStringBuffer.append(paramString1.substring(j));
    return localStringBuffer.toString();
  }

  public static String replaceOccurrence(String paramString1, String paramString2, String paramString3, int paramInt)
  {
    if (paramString1 == null)
      return "";
    int i = i = paramString2.length();
    int j = paramString1.indexOf(paramString2, 0);
    int k = 0;
    while (j >= 0)
    {
      k++;
      if (k >= paramInt)
        break;
      j = paramString1.indexOf(paramString2, j + i);
    }
    if (k == paramInt)
      return replaceMid(paramString1, j + 1, i, paramString3);
    return paramString1;
  }

  public static String left(String paramString, int paramInt)
  {
    if (paramString == null)
      return "";
    if (paramInt > paramString.length())
      return paramString;
    return paramString.substring(0, paramInt);
  }

  public static String right(String paramString, int paramInt)
  {
    if (paramString == null)
      return "";
    int i = paramString.length();
    if (i - paramInt < 0)
      return paramString;
    return paramString.substring(i - paramInt);
  }

  public static String toLowerCase(String paramString)
  {
    if (paramString == null)
      return "";
    return paramString.toLowerCase();
  }

  public static String toUpperCase(String paramString)
  {
    if (paramString == null)
      return "";
    return paramString.toUpperCase();
  }

  public static String toProperCase(String paramString)
  {
    if (paramString == null)
      return "";
    StringBuffer localStringBuffer = new StringBuffer(paramString.toLowerCase());
    int i = localStringBuffer.length();
    int j = 1;
    for (int k = 0; k < i; k++)
    {
      char c = localStringBuffer.charAt(k);
      if (j != 0)
      {
        localStringBuffer.setCharAt(k, Character.toUpperCase(c));
        j = 0;
      }
      if (c != ' ')
        continue;
      j = 1;
    }
    return localStringBuffer.toString();
  }

  public static String trimLeft(String paramString)
  {
    if (paramString == null)
      return "";
    int i = paramString.length();
    int j = 0;
    for (j = 0; (j < i) && (paramString.charAt(j) == ' '); j++);
    return j > 0 ? paramString.substring(j, i) : paramString;
  }

  public static String trimRight(String paramString)
  {
    if (paramString == null)
      return "";
    int i = 0;
    for (i = paramString.length(); (i > 0) && (paramString.charAt(i - 1) == ' '); i--);
    return i < paramString.length() ? paramString.substring(0, i) : paramString;
  }

  public static String trim(String paramString)
  {
    if (paramString == null)
      return "";
    return trimLeft(trimRight(paramString));
  }

  public static String trimAll(String paramString)
  {
    if (paramString == null)
      return "";
    paramString = paramString.trim();
    try
    {
      return paramString.replaceAll("\\s+", " ");
    }
    catch (Exception localException)
    {
    }
    return paramString;
  }

  public static String clean(String paramString)
  {
    if (paramString == null)
      return "";
    StringBuffer localStringBuffer = new StringBuffer(paramString);
    int i = localStringBuffer.length();
    for (int j = i - 1; j >= 0; j--)
    {
      if (!Character.isISOControl(localStringBuffer.charAt(j)))
        continue;
      localStringBuffer.deleteCharAt(j);
    }
    return localStringBuffer.toString();
  }

  public static int len(String paramString)
  {
    if (paramString == null)
      return 0;
    return paramString.length();
  }

  public static int find(int paramInt, String paramString1, String paramString2, boolean paramBoolean)
  {
    if ((paramString1 == null) || (paramString2 == null))
      return 0;
    if (paramBoolean)
    {
      paramString1 = paramString1.toLowerCase();
      paramString2 = paramString2.toLowerCase();
    }
    return paramString1.indexOf(paramString2, paramInt - 1) + 1;
  }

  public static int find(int paramInt, String paramString1, String paramString2)
  {
    if ((paramString1 == null) || (paramString2 == null))
      return 0;
    return paramString1.indexOf(paramString2, paramInt - 1) + 1;
  }

  public static int find(String paramString1, String paramString2)
  {
    if ((paramString1 == null) || (paramString2 == null))
      return 0;
    return paramString1.indexOf(paramString2) + 1;
  }

  public static int findReverse(String paramString1, String paramString2, int paramInt, boolean paramBoolean)
  {
    if ((paramString1 == null) || (paramString2 == null))
      return -1;
    if (paramInt < 0)
      paramInt = paramString1.length();
    if (paramBoolean)
    {
      paramString1 = paramString1.toLowerCase();
      paramString2 = paramString2.toLowerCase();
    }
    return paramString1.lastIndexOf(paramString2, paramInt - 1) + 1;
  }

  public static int compare(String paramString1, String paramString2)
  {
    if (paramString1 == paramString2)
      return 0;
    if (paramString1 == null)
      return -1;
    if (paramString2 == null)
      return 1;
    int i = paramString1.compareTo(paramString2);
    if (i == 0)
      return 0;
    return i > 0 ? 1 : -1;
  }

  public static String space(int paramInt)
  {
    return fill(paramInt, ' ');
  }

  public static String fill(int paramInt, char paramChar)
  {
    char[] arrayOfChar = new char[paramInt];
    for (int i = 0; i < paramInt; i++)
      arrayOfChar[i] = paramChar;
    return new String(arrayOfChar);
  }

  public static String fill(int paramInt, String paramString)
  {
    return fill(paramInt, paramString.charAt(0));
  }

  public static String repeat(String paramString, int paramInt)
  {
    StringBuffer localStringBuffer = new StringBuffer();
    for (int i = 0; i < paramInt; i++)
      localStringBuffer.append(paramString);
    return localStringBuffer.toString();
  }

  public static String formatNumber(double paramDouble, int paramInt, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    return formatDecimal(NumberFormat.getInstance(), paramDouble, paramInt, paramBoolean1, paramBoolean2, paramBoolean3);
  }

  public static String formatPercent(double paramDouble, int paramInt, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    return formatDecimal(NumberFormat.getPercentInstance(), paramDouble, paramInt, paramBoolean1, paramBoolean2, paramBoolean3);
  }

  public static String formatCurrency(double paramDouble, int paramInt, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    return formatDecimal(NumberFormat.getCurrencyInstance(), paramDouble, paramInt, paramBoolean1, paramBoolean2, paramBoolean3);
  }

  private static String formatDecimal(NumberFormat paramNumberFormat, double paramDouble, int paramInt, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3)
  {
    if (paramInt >= 0)
    {
      paramNumberFormat.setMinimumFractionDigits(paramInt);
      paramNumberFormat.setMaximumFractionDigits(paramInt);
    }
    paramNumberFormat.setGroupingUsed(paramBoolean3);
    if (paramBoolean1);
    paramNumberFormat.setMinimumIntegerDigits(1);
    String str = paramNumberFormat.format(paramDouble);
    int i;
    if (!paramBoolean1)
    {
      i = str.indexOf("0.");
      str = str.substring(0, i) + str.substring(i + 1);
    }
    if (paramBoolean2)
    {
      i = str.indexOf("-");
      if (i >= 0)
        str = str.substring(0, i) + "(" + str.substring(i + 1) + ")";
    }
    return str;
  }

  public static String join(Object[] paramArrayOfObject, String paramString)
  {
    StringBuffer localStringBuffer = new StringBuffer();
    for (int i = 0; i < paramArrayOfObject.length; i++)
    {
      if (i > 0)
        localStringBuffer.append(paramString);
      localStringBuffer.append(paramArrayOfObject[i]);
    }
    return localStringBuffer.toString();
  }

  public static String join(Variant paramVariant, String paramString)
  {
    if (paramVariant.isArray())
      return join((Object[])paramVariant.toObject(), paramString);
    return paramVariant.toString();
  }

  public static String[] split(String paramString1, String paramString2, int paramInt)
  {
    if (paramInt == 0)
      return null;
    StringSplitter localStringSplitter = new StringSplitter(paramString1, paramString2);
    Vector localVector = new Vector();
    while ((localStringSplitter.hasMoreTokens()) && (paramInt != 0))
    {
      localVector.addElement(localStringSplitter.nextToken());
      paramInt--;
    }
    if (localStringSplitter.hasMoreTokens())
    {
    	Object localObject;
      for (localObject = (String)localVector.lastElement(); localStringSplitter.hasMoreTokens(); localObject = (String)localObject + "," + localStringSplitter.nextToken());
      localVector.setElementAt(localObject, localVector.size() - 1);
    }
    Object[] localObject = new String[localVector.size()];
    localVector.copyInto(localObject);
    return (String[])localObject;
  }

  public static String format(double paramDouble, String paramString)
  {
    if (paramString == null)
      return String.valueOf(paramDouble);
    String str = paramString.toUpperCase();
    Object localObject;
    if (str.equals("GENERAL NUMBER"))
    {
      localObject = NumberFormat.getNumberInstance();
      ((NumberFormat)localObject).setGroupingUsed(false);
    }
    else if (str.equals("CURRENCY"))
    {
      localObject = NumberFormat.getCurrencyInstance();
    }
    else if (str.equals("FIXED"))
    {
      localObject = NumberFormat.getNumberInstance();
      ((NumberFormat)localObject).setMinimumFractionDigits(2);
      ((NumberFormat)localObject).setMaximumFractionDigits(2);
      ((NumberFormat)localObject).setMinimumIntegerDigits(1);
      ((NumberFormat)localObject).setGroupingUsed(false);
    }
    else if (str.equals("STANDARD"))
    {
      localObject = NumberFormat.getNumberInstance();
      ((NumberFormat)localObject).setMinimumFractionDigits(2);
      ((NumberFormat)localObject).setMaximumFractionDigits(2);
      ((NumberFormat)localObject).setMinimumIntegerDigits(1);
    }
    else if (str.equals("PERCENT"))
    {
      localObject = NumberFormat.getPercentInstance();
      ((NumberFormat)localObject).setMinimumFractionDigits(2);
      ((NumberFormat)localObject).setMaximumFractionDigits(2);
    }
    else if (str.equals("SCIENTIFIC"))
    {
      localObject = new DecimalFormat("0.##E00");
    }
    else
    {
      if (str.equals("YES/NO"))
        return paramDouble == 0.0D ? "No" : "Yes";
      if (str.equals("TRUE/FALSE"))
        return paramDouble == 0.0D ? "False" : "True";
      if (str.equals("ON/OFF"))
        return paramDouble == 0.0D ? "Off" : "On";
      localObject = new DecimalFormat(paramString);
    }
    return (String)((NumberFormat)localObject).format(paramDouble);
  }

  private static boolean isNumericFormat(String paramString)
  {
    if (paramString == null)
      return false;
    String str = paramString.toUpperCase();
    return (str.equals("GENERAL NUMBER")) || (str.equals("CURRENCY")) || (str.equals("FIXED")) || (str.equals("STANDARD")) || (str.equals("PERCENT")) || (str.equals("SCIENTIFIC")) || (str.equals("YES/NO")) || (str.equals("TRUE/FALSE")) || (str.equals("ON/OFF"));
  }

  public static String format(String paramString1, String paramString2)
  {
    if (paramString2 == null)
      return paramString1;
    int i = 0;
    int j = 0;
    int k = 1;
    if (paramString2.indexOf('!') >= 0)
      k = 0;
    StringBuffer localStringBuffer = new StringBuffer();
    String str = paramString1;
    int m = 0;
    char c;
    if (k != 0)
    {
      int n = 0;
      for (int i1 = 0; i1 < paramString2.length(); i1++)
      {
        c = paramString2.charAt(i1);
        if ((c != '@') && (c != '&'))
          continue;
        n++;
      }
      n -= paramString1.length();
      for (m = 0; (m < paramString2.length()) && (n > 0); m++)
      {
        c = paramString2.charAt(m);
        if ((c == '&') || (c == '@'))
        {
          if (c == '@')
            localStringBuffer.append(' ');
          n--;
        }
        else
        {
          if ((c == '<') || (c == '>') || (c == '!'))
            continue;
          localStringBuffer.append(c);
        }
      }
    }
    int n = 0;
    int i1 = paramString1.length();
    while (m < paramString2.length())
    {
      c = paramString2.charAt(m);
      if (c == '@')
        localStringBuffer.append(n < i1 ? paramString1.charAt(n++) : ' ');
      else if (c == '&')
      {
        if (n < i1)
          localStringBuffer.append(paramString1.charAt(n++));
      }
      else if (c == '<')
        j = 1;
      else if (c == '>')
        i = 1;
      else if (c != '!')
        localStringBuffer.append(c);
      m++;
    }
    if (paramString1.length() > n)
      localStringBuffer.append(paramString1.substring(n));
    str = localStringBuffer.toString();
    if (i != 0)
      str = str.toUpperCase();
    if (j != 0)
      str = str.toLowerCase();
    return str;
  }

  public static String format(Variant paramVariant, String paramString)
  {
    if (paramVariant.isNull())
      return "";
    if ((paramVariant.isNumeric()) || (isNumericFormat(paramString)))
      return format(paramVariant.toDouble(), paramString);
    if (paramVariant.isDate())
      return DateTime.format(paramVariant.toDate(), paramString, 0);
    return format(paramVariant.toString(), paramString);
  }
}