package com.zfbots.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTime
{
  public static final int USE_SYSTEM_DAY_OF_WEEK = 0;
  private static final double millisPerDay = 86400000.0D;
  private static DateFormat dateTimeFormatShort1 = DateFormat.getDateTimeInstance(3, 3);
  private static DateFormat dateTimeFormatShort2 = DateFormat.getDateTimeInstance(3, 2);
  private static DateFormat dateTimeFormatShort3 = DateFormat.getDateTimeInstance(3, 1);
  private static DateFormat dateTimeFormatShort4 = DateFormat.getDateTimeInstance(3, 0);
  private static DateFormat dateTimeFormatMed1 = DateFormat.getDateTimeInstance(2, 3);
  private static DateFormat dateTimeFormatMed2 = DateFormat.getDateTimeInstance(2, 2);
  private static DateFormat dateTimeFormatMed3 = DateFormat.getDateTimeInstance(2, 1);
  private static DateFormat dateTimeFormatMed4 = DateFormat.getDateTimeInstance(2, 1);
  private static DateFormat dateTimeFormatLong1 = DateFormat.getDateTimeInstance(1, 3);
  private static DateFormat dateTimeFormatLong2 = DateFormat.getDateTimeInstance(1, 2);
  private static DateFormat dateTimeFormatLong3 = DateFormat.getDateTimeInstance(1, 1);
  private static DateFormat dateTimeFormatLong4 = DateFormat.getDateTimeInstance(1, 0);
  private static DateFormat dateTimeFormatFull1 = DateFormat.getDateTimeInstance(0, 3);
  private static DateFormat dateTimeFormatFull2 = DateFormat.getDateTimeInstance(0, 2);
  private static DateFormat dateTimeFormatFull3 = DateFormat.getDateTimeInstance(0, 1);
  private static DateFormat dateTimeFormatFull4 = DateFormat.getDateTimeInstance(0, 0);
  private static DateFormat dateFormat1 = DateFormat.getDateInstance(3);
  private static DateFormat dateFormat2 = DateFormat.getDateInstance(2);
  private static DateFormat dateFormat3 = DateFormat.getDateInstance(1);
  private static DateFormat dateFormat4 = DateFormat.getDateInstance(0);
  private static DateFormat timeFormat1 = DateFormat.getTimeInstance(3);
  private static DateFormat timeFormat2 = DateFormat.getTimeInstance(2);
  private static DateFormat timeFormat3 = DateFormat.getTimeInstance(1);
  private static DateFormat timeFormat4 = DateFormat.getTimeInstance(0);
  private static DateFormat toStringFormat1 = new SimpleDateFormat("EEE MMM dd H:mm:ss zzz yyyy", Locale.US);
  private static DateFormat toStringFormat2 = new SimpleDateFormat("EEE MMM dd H:mm:ss yyyy", Locale.US);
  private static DateFormat db2Format1 = new SimpleDateFormat("yyyy-M-d H:mm:ss");
  private static DateFormat db2Format2 = new SimpleDateFormat("yyyy-M-d H:mm:ss.S");
  private static DateFormat dateTimeFormat1 = new SimpleDateFormat("M/d/yy H:mm");
  private static DateFormat dateTimeFormat2 = new SimpleDateFormat("M/d/yy H:mm:ss");
  private static DateFormat dateTimeFormat3 = new SimpleDateFormat("d-MMM-yy H:mm");
  private static DateFormat dateTimeFormat4 = new SimpleDateFormat("d-MMM-yy h:mm a");
  private static DateFormat dateTimeFormat5 = new SimpleDateFormat("d-MMM-yy H:mm:ss");
  private static DateFormat dateTimeFormat6 = new SimpleDateFormat("d-MMM-yy h:mm:ss a");
  private static DateFormat dateFormat5 = new SimpleDateFormat("M-d-yy");
  private static DateFormat dateFormat6 = new SimpleDateFormat("d-MMM-yy");
  private static DateFormat dateFormat7 = new SimpleDateFormat("d MMM yy");
  private static DateFormat dateFormat8 = new SimpleDateFormat("d.MMM.yy");
  private static DateFormat outputDateFormat = DateFormat.getDateInstance(3);
  private static DateFormat outputDateTimeFormat = DateFormat.getDateTimeInstance(3, 2);
  private static DateFormat outputTimeFormat = DateFormat.getTimeInstance(2);
  public static final Date EmptyDate = toDate(0.0D);
  private static int zoneOffset = 0;

  public static final Calendar getCalendar(Date paramDate)
  {
    Calendar localCalendar = Calendar.getInstance();
    if (paramDate != null)
      localCalendar.setTime(paramDate);
    return localCalendar;
  }

  public static final int compare(Date paramDate1, Date paramDate2)
  {
    long l1 = paramDate1.getTime();
    long l2 = paramDate2.getTime();
    if (l1 == l2)
      return 0;
    if (l1 < l2)
      return -1;
    return 1;
  }

  public static final int toInt(Date paramDate)
  {
    if (paramDate == null)
      return 0;
    return (int)Math.round(toDouble(paramDate));
  }

  public static final double toDouble(Date paramDate)
  {
    if (paramDate == null)
      return 0.0D;
    return (paramDate.getTime() + zoneOffset) / 86400000.0D + 25569.0D;
  }

  public static final Date toDate(double paramDouble)
  {
    return new Date(Math.round((paramDouble - 25569.0D) * 86400000.0D - zoneOffset));
  }

  public static final Date toDate(String paramString)
    throws ParseException
  {
    if ((paramString == null) || (paramString.length() == 0))
      return null;
    DateFormat localDateFormat = dateTimeFormatShort2;
    ParsePosition localParsePosition = new ParsePosition(0);
    int i = paramString.length();
    for (int j = 0; j < 38; j++)
    {
      switch (j)
      {
      case 0:
        localDateFormat = dateTimeFormatShort1;
        break;
      case 1:
        localDateFormat = dateTimeFormatShort2;
        break;
      case 2:
        localDateFormat = dateTimeFormat1;
        break;
      case 3:
        localDateFormat = dateFormat1;
        break;
      case 4:
        localDateFormat = dateFormat2;
        break;
      case 5:
        localDateFormat = dateFormat3;
        break;
      case 6:
        localDateFormat = timeFormat1;
        break;
      case 7:
        localDateFormat = timeFormat2;
        break;
      case 8:
        localDateFormat = dateTimeFormatMed1;
        break;
      case 9:
        localDateFormat = dateTimeFormatMed2;
        break;
      case 10:
        localDateFormat = dateTimeFormatMed3;
        break;
      case 11:
        localDateFormat = toStringFormat1;
        break;
      case 12:
        localDateFormat = toStringFormat2;
        break;
      case 13:
        localDateFormat = dateTimeFormatLong1;
        break;
      case 14:
        localDateFormat = dateTimeFormatLong2;
        break;
      case 15:
        localDateFormat = dateTimeFormatLong3;
        break;
      case 16:
        localDateFormat = dateTimeFormatFull1;
        break;
      case 17:
        localDateFormat = dateTimeFormatFull2;
        break;
      case 18:
        localDateFormat = dateTimeFormatFull3;
        break;
      case 19:
        localDateFormat = timeFormat3;
        break;
      case 20:
        localDateFormat = timeFormat4;
        break;
      case 21:
        localDateFormat = db2Format1;
        break;
      case 22:
        localDateFormat = db2Format2;
        break;
      case 23:
        localDateFormat = dateTimeFormatShort3;
        break;
      case 24:
        localDateFormat = dateTimeFormatShort4;
        break;
      case 25:
        localDateFormat = dateTimeFormatMed4;
        break;
      case 26:
        localDateFormat = dateTimeFormatLong4;
        break;
      case 27:
        localDateFormat = dateTimeFormatFull4;
        break;
      case 28:
        localDateFormat = dateTimeFormat2;
        break;
      case 29:
        localDateFormat = dateTimeFormat3;
        break;
      case 30:
        localDateFormat = dateTimeFormat4;
        break;
      case 31:
        localDateFormat = dateTimeFormat5;
        break;
      case 32:
        localDateFormat = dateTimeFormat6;
        break;
      case 33:
        localDateFormat = dateFormat4;
        break;
      case 34:
        localDateFormat = dateFormat5;
        break;
      case 35:
        localDateFormat = dateFormat6;
        break;
      case 36:
        localDateFormat = dateFormat7;
        break;
      case 37:
        localDateFormat = dateFormat8;
      }
      localParsePosition.setIndex(0);
      Date localDate = localDateFormat.parse(paramString, localParsePosition);
      if ((localDate != null) && (localParsePosition.getIndex() >= i))
        return localDate;
    }
    try
    {
      return toDate(Double.parseDouble(paramString));
    }
    catch (Exception localException)
    {
    }
    throw new ParseException("Unparseable date: " + paramString, 0);
  }

  public static final Date toDate2(String paramString)
  {
    try
    {
      return toDate(paramString);
    }
    catch (Exception localException)
    {
    }
    return null;
  }

  private static final boolean isTimeSpecified(Date paramDate)
  {
    Calendar localCalendar = Calendar.getInstance();
    if (paramDate != null)
      localCalendar.setTime(paramDate);
    int i = localCalendar.get(11);
    int j = localCalendar.get(12);
    int k = localCalendar.get(13);
    return (i != 0) || (j != 0) || (k != 0);
  }

  private static final boolean isDaySpecified(Date paramDate)
  {
    double d = toDouble(paramDate);
    return (int)d != 0;
  }

  public static Date now()
  {
    return new Date();
  }

  private static int dateField(String paramString)
  {
    paramString = paramString.toLowerCase();
    int i = 1;
    if (paramString.equals("yyyy"))
      i = 1;
    else if (paramString.equals("q"))
      i = 2;
    else if (paramString.equals("m"))
      i = 2;
    else if (paramString.equals("y"))
      i = 6;
    else if (paramString.equals("d"))
      i = 5;
    else if (paramString.equals("w"))
      i = 7;
    else if (paramString.equals("ww"))
      i = 3;
    else if (paramString.equals("h"))
      i = 11;
    else if (paramString.equals("n"))
      i = 12;
    else if (paramString.equals("s"))
      i = 13;
    return i;
  }

  public static Date add(String paramString, int paramInt, Date paramDate)
  {
    int i = dateField(paramString);
    Calendar localCalendar = getCalendar(paramDate);
    localCalendar.add(i, paramInt);
    if ((i == 2) && (paramString.equals("q")))
      localCalendar.add(i, paramInt * 2);
    return localCalendar.getTime();
  }

  public static Date add(int paramInt1, int paramInt2, Date paramDate)
  {
    Calendar localCalendar = getCalendar(paramDate);
    localCalendar.add(paramInt1, paramInt2);
    return localCalendar.getTime();
  }

  public static Date add(Date paramDate1, Date paramDate2)
  {
    return add(paramDate1, toDouble(paramDate2));
  }

  public static Date add(Date paramDate, double paramDouble)
  {
    Calendar localCalendar = getCalendar(paramDate);
    long l = Math.round(Math.floor(paramDouble));
    double d = paramDouble - l;
    localCalendar.add(5, (int)l);
    localCalendar.add(14, (int)Math.round(d * 86400000.0D));
    return localCalendar.getTime();
  }

  public static Date subtract(Date paramDate, double paramDouble)
  {
    return add(paramDate, -paramDouble);
  }

  public static int diff(String paramString, Date paramDate1, Date paramDate2)
  {
    double d1 = toDouble(paramDate1);
    double d2 = toDouble(paramDate2);
    int i = 0;
    if (paramString.equals("yyyy"))
      i = year(paramDate2) - year(paramDate1);
    else if (paramString.equals("q"))
      i = (int)(d2 / 365.0D * 4.0D + 1.0E-006D) - (int)(d1 / 365.0D * 4.0D + 1.0E-006D);
    else if (paramString.equals("m"))
      i = (int)(d2 / 365.0D * 12.0D + 1.0E-006D) - (int)(d1 / 365.0D * 12.0D + 1.0E-006D);
    else if ((paramString.equals("y")) || (paramString.equals("d")) || (paramString.equals("w")))
      i = (int)(d2 + 1.0E-006D) - (int)(d1 + 1.0E-006D);
    else if (paramString.equals("ww"))
      i = (int)(d2 * 52.0D / 365.0D + 1.0E-006D) - (int)(d1 * 52.0D / 365.0D + 1.0E-006D);
    else if (paramString.equals("h"))
      i = (int)(d2 * 24.0D + 1.0E-006D) - (int)(d1 * 24.0D + 1.0E-006D);
    else if (paramString.equals("n"))
      i = (int)(d2 * 24.0D * 60.0D + 1.0E-006D) - (int)(d1 * 24.0D * 60.0D + 1.0E-006D);
    else if (paramString.equals("s"))
      i = (int)((d2 * 24.0D * 60.0D * 60.0D) - (d1 * 24.0D * 60.0D * 60.0D));
    return i;
  }

  public static int diff2(Date paramDate1, Date paramDate2, String paramString)
  {
    double d1 = toDouble(paramDate1);
    double d2 = toDouble(paramDate2);
    int i = 0;
    if (paramString.equalsIgnoreCase("y"))
    {
      i = (int)((d2 - d1) / 365.0D + 1.0E-006D);
    }
    else if (paramString.equalsIgnoreCase("m"))
    {
      i = (int)((d2 - d1) / 365.0D * 12.0D + 1.0E-006D);
    }
    else if (paramString.equalsIgnoreCase("d"))
    {
      i = (int)(d2 + 1.0E-006D) - (int)(d1 + 1.0E-006D);
    }
    else if (paramString.equalsIgnoreCase("md"))
    {
      i = day(paramDate2) - day(paramDate1);
    }
    else
    {
      int j;
      if (paramString.equalsIgnoreCase("ym"))
      {
        j = month(paramDate1);
        int k = month(paramDate2);
        if (j > k)
          k += 12;
        i = k - j;
      }
      else if (paramString.equalsIgnoreCase("yd"))
      {
        j = dayOfYear(paramDate2);
        Calendar localCalendar = getCalendar(paramDate1);
        localCalendar.set(1, year(paramDate2));
        int m = localCalendar.get(6);
        if (m > j)
          j += 365;
        i = j - m;
      }
    }
    return i;
  }

  public static double diff(Date paramDate1, Date paramDate2)
  {
    double d1 = toDouble(paramDate1);
    double d2 = toDouble(paramDate2);
    return d2 - d1;
  }

  public static int year(Date paramDate)
  {
    Calendar localCalendar = getCalendar(paramDate);
    return localCalendar.get(1);
  }

  public static int month(Date paramDate)
  {
    Calendar localCalendar = getCalendar(paramDate);
    return localCalendar.get(2) + 1;
  }

  public static int day(Date paramDate)
  {
    Calendar localCalendar = getCalendar(paramDate);
    return localCalendar.get(5);
  }

  public static int dayOfYear(Date paramDate)
  {
    Calendar localCalendar = getCalendar(paramDate);
    return localCalendar.get(6);
  }

  public static int weekday(Date paramDate, int paramInt)
  {
    Calendar localCalendar = getCalendar(paramDate);
    int i = localCalendar.get(7);
    if (paramInt != 0)
    {
      i -= paramInt - 1;
      if (i < 1)
        i += 7;
    }
    return i;
  }

  public static int weekday2(Date paramDate, int paramInt)
  {
    Calendar localCalendar = getCalendar(paramDate);
    int i = localCalendar.get(7);
    if (paramInt >= 2)
    {
      i--;
      if (i < 1)
        i = 7;
      if (paramInt == 3)
        i--;
    }
    return i;
  }

  public static int weekOfYear(Date paramDate, int paramInt)
  {
    Calendar localCalendar = getCalendar(paramDate);
    if (paramInt == 1)
      localCalendar.setFirstDayOfWeek(1);
    else if (paramInt == 2)
      localCalendar.setFirstDayOfWeek(2);
    return localCalendar.get(3);
  }

  public static int hour(Date paramDate)
  {
    Calendar localCalendar = getCalendar(paramDate);
    return localCalendar.get(11);
  }

  public static int minute(Date paramDate)
  {
    Calendar localCalendar = getCalendar(paramDate);
    return localCalendar.get(12);
  }

  public static int second(Date paramDate)
  {
    Calendar localCalendar = getCalendar(paramDate);
    return localCalendar.get(13);
  }

  public static float timer()
  {
    Calendar localCalendar = Calendar.getInstance();
    return localCalendar.get(11) * 3600 + localCalendar.get(12) * 60 + localCalendar.get(13) + localCalendar.get(14) / 1000.0F;
  }

  public static Date dateSerial(int paramInt1, int paramInt2, int paramInt3)
  {
    Calendar localCalendar = Calendar.getInstance();
    localCalendar.set(paramInt1, paramInt2 - 1, paramInt3, 0, 0, 0);
    return localCalendar.getTime();
  }

  public static Date timeSerial(int paramInt1, int paramInt2, int paramInt3)
  {
    Calendar localCalendar = Calendar.getInstance();
    localCalendar.set(1899, 11, 30, paramInt1, paramInt2, paramInt3);
    return localCalendar.getTime();
  }

  public static int datePart(String paramString, Date paramDate, int paramInt1, int paramInt2)
  {
    int i = dateField(paramString);
    Calendar localCalendar = getCalendar(paramDate);
    if (paramInt1 != 0)
      localCalendar.setFirstDayOfWeek(paramInt1);
    int j = localCalendar.get(i);
    if ((i == 2) && (paramString.equals("q")))
      j /= 4;
    return j;
  }

  public static int datePart(String paramString, Date paramDate)
  {
    return datePart(paramString, paramDate, 0, 0);
  }

  /** @deprecated */
  public static String formatDateTime(Date paramDate)
  {
    return format(paramDate);
  }

  public static String format(Date paramDate)
  {
    if (paramDate == null)
      return "";
    boolean bool1 = isDaySpecified(paramDate);
    boolean bool2 = isTimeSpecified(paramDate);
    DateFormat localDateFormat;
    if ((bool1) && (bool2))
      localDateFormat = outputDateTimeFormat;
    else if (bool2)
      localDateFormat = outputTimeFormat;
    else
      localDateFormat = outputDateFormat;
    return localDateFormat.format(paramDate);
  }

  public static void setDefaultDateFormat(DateFormat paramDateFormat)
  {
    outputDateFormat = paramDateFormat;
  }

  public static void setDefaultTimeFormat(DateFormat paramDateFormat)
  {
    outputTimeFormat = paramDateFormat;
  }

  public static void setDefaultDateTimeFormat(DateFormat paramDateFormat)
  {
    outputDateTimeFormat = paramDateFormat;
  }

  public static String weekdayName(int paramInt1, boolean paramBoolean, int paramInt2)
  {
    Calendar localCalendar = Calendar.getInstance();
    if (paramInt2 != 0)
      localCalendar.setFirstDayOfWeek(paramInt2);
    localCalendar.set(7, paramInt1);
    SimpleDateFormat localSimpleDateFormat;
    if (paramBoolean)
      localSimpleDateFormat = new SimpleDateFormat("E");
    else
      localSimpleDateFormat = new SimpleDateFormat("EEEE");
    return localSimpleDateFormat.format(localCalendar.getTime());
  }

  public static String monthName(int paramInt, boolean paramBoolean)
  {
    Calendar localCalendar = Calendar.getInstance();
    localCalendar.set(1970, paramInt - 1, 1, 0, 0, 0);
    SimpleDateFormat localSimpleDateFormat;
    if (paramBoolean)
      localSimpleDateFormat = new SimpleDateFormat("MMM");
    else
      localSimpleDateFormat = new SimpleDateFormat("MMMM");
    return localSimpleDateFormat.format(localCalendar.getTime());
  }

  public static String format(Date paramDate, String paramString, int paramInt)
  {
    if (paramString == null)
      return paramDate.toString();
    if (paramDate == null)
      return "";
    String str = paramString.toUpperCase();
    if (str.equals("GENERAL DATE"))
      return format(paramDate);
    Object localObject;
    if (str.equals("LONG DATE"))
      localObject = dateFormat3;
    else if (str.equals("MEDIUM DATE"))
      localObject = new SimpleDateFormat("dd-MMM-yy");
    else if (str.equals("SHORT DATE"))
      localObject = dateFormat1;
    else if (str.equals("LONG TIME"))
      localObject = timeFormat2;
    else if (str.equals("MEDIUM TIME"))
      localObject = new SimpleDateFormat("hh:mm a");
    else if (str.equals("SHORT TIME"))
      localObject = new SimpleDateFormat("HH:mm");
    else
      localObject = new SimpleDateFormat(paramString);
    return (String)((DateFormat)localObject).format(paramDate);
  }

  public static String format(Date paramDate, String paramString)
  {
    return format(paramDate, paramString, 0);
  }

  public static String format(String paramString1, String paramString2)
  {
    try
    {
      return format(toDate(paramString1), paramString2, 0);
    }
    catch (Exception localException)
    {
    }
    return paramString1;
  }

  public static String format(Variant paramVariant, String paramString)
  {
    if (paramVariant.isNull())
      return "";
    return format(paramVariant.toDate(), paramString, 0);
  }

  static
  {
    Calendar localCalendar = Calendar.getInstance();
    zoneOffset = localCalendar.get(15) + localCalendar.get(16);
  }
}