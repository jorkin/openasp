package com.zfbots.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormatSymbols;
import java.util.Locale;

public class ListFormat
{
  private HashList values = new HashList();
  private int dataValueType = 0;

  public int size()
  {
    return this.values.size();
  }

  private Variant makeDataValue(Object paramObject)
  {
    Variant localVariant = new Variant(paramObject);
    if (this.dataValueType != 0)
      try
      {
        localVariant.changeType(this.dataValueType);
      }
      catch (Exception localException)
      {
      }
    return localVariant;
  }

  public void setDataValueType(Class paramClass)
  {
    if (paramClass == null)
      this.dataValueType = 0;
    else
      this.dataValueType = Variant.getVarType(paramClass);
  }

  public int getIndex(Object paramObject)
  {
    return this.values.getIndex(paramObject);
  }

  public Object get(int paramInt)
  {
    return this.values.get(paramInt);
  }

  public Object get(Object paramObject)
  {
    if (this.dataValueType != 0)
    {
      Variant localVariant = new Variant(paramObject);
      try
      {
        localVariant.changeType(this.dataValueType);
      }
      catch (Exception localException)
      {
      }
      if ((paramObject == null) && (localVariant.isNumeric()))
        return null;
      paramObject = localVariant.toObject();
    }
    return this.values.get(paramObject);
  }

  public Object getDataValue(int paramInt)
  {
    Object localObject = this.values.getKey(paramInt);
    if ((localObject instanceof Variant))
      return ((Variant)localObject).toObject();
    return localObject;
  }

  public Object set(int paramInt, Object paramObject)
  {
    return this.values.set(paramInt, paramObject);
  }

  public Object set(Object paramObject1, Object paramObject2)
  {
    return this.values.set(makeDataValue(paramObject1), paramObject2);
  }

  public void add(Object paramObject1, Object paramObject2)
  {
    this.values.add(paramObject2, makeDataValue(paramObject1));
  }

  public void add(int paramInt, Object paramObject1, Object paramObject2)
  {
    this.values.add(paramObject2, makeDataValue(paramObject1), paramInt);
  }

  public void add(ResultSet paramResultSet)
    throws SQLException
  {
    while (paramResultSet.next())
      add(paramResultSet.getObject(1), paramResultSet.getObject(2));
  }

  public void add(Connection paramConnection, String paramString)
    throws SQLException
  {
    ResultSet localResultSet = paramConnection.createStatement().executeQuery(paramString);
    add(localResultSet);
  }

  public Object remove(int paramInt)
  {
    return this.values.remove(paramInt);
  }

  public Object removeDataValue(Object paramObject)
  {
    return this.values.removeKey(paramObject);
  }

  public boolean remove(Object paramObject)
  {
    return this.values.remove(paramObject);
  }

  public void clear()
  {
    this.values.clear();
  }

  public boolean containsDataValue(Object paramObject)
  {
    return this.values.containsKey(paramObject);
  }

  public boolean containsDisplayValue(Object paramObject)
  {
    return this.values.containsValue(paramObject);
  }

  public static ListFormat getMonthNames(Locale paramLocale)
  {
    ListFormat localListFormat = new ListFormat();
    DateFormatSymbols localDateFormatSymbols = paramLocale == null ? new DateFormatSymbols() : new DateFormatSymbols(paramLocale);
    String[] arrayOfString = localDateFormatSymbols.getMonths();
    for (int i = 0; i < arrayOfString.length; i++)
      localListFormat.add(new Integer(i), arrayOfString[i]);
    return localListFormat;
  }

  public static ListFormat getWeekdayNames(Locale paramLocale)
  {
    ListFormat localListFormat = new ListFormat();
    DateFormatSymbols localDateFormatSymbols = paramLocale == null ? new DateFormatSymbols() : new DateFormatSymbols(paramLocale);
    String[] arrayOfString = localDateFormatSymbols.getWeekdays();
    for (int i = 0; i < arrayOfString.length; i++)
      localListFormat.add(new Integer(i), arrayOfString[i]);
    return localListFormat;
  }
}