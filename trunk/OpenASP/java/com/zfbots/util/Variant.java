package com.zfbots.util;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;

public class Variant
  implements Cloneable, Serializable
{
  private Object val;
  private short vartype;
  public static final short EMPTY = 0;
  public static final short NULL = 1;
  public static final short BYTE = 2;
  public static final short SHORT = 3;
  public static final short INTEGER = 4;
  public static final short ERROR = 5;
  public static final short LONG = 6;
  public static final short FLOAT = 7;
  public static final short DOUBLE = 8;
  public static final short DECIMAL = 10;
  public static final short BOOLEAN = 11;
  public static final short VARIANT = 12;
  public static final short OBJECT = 13;
  public static final short DATE = 14;
  public static final short STRING = 15;
  public static final short ARRAY = 8192;
  private static final int adNumeric = 131;
  private static final int adDBDate = 133;
  private static final int adDBTime = 134;
  private static final int adDBTimeStamp = 135;
  private static final int adChar = 129;
  private static final int adVarChar = 200;
  private static final int adLongVarChar = 201;
  private static final int adBinary = 128;
  private static final int adVarBinary = 204;
  private static final int adLongVarBinary = 205;

  public Variant()
  {
    this.vartype = 0;
    this.val = null;
  }

  public Variant(int paramInt)
  {
    set(paramInt);
  }

  public Variant(double paramDouble)
  {
    set(paramDouble);
  }

  public Variant(Object paramObject)
  {
    set(paramObject);
  }

  public Variant(Object paramObject, int paramInt)
  {
    set(paramObject, paramInt);
  }

  public Variant(boolean paramBoolean)
  {
    set(paramBoolean);
  }

  public Variant(String paramString)
  {
    set(paramString);
  }

  public Variant(java.util.Date paramDate)
  {
    set(paramDate);
  }

  public Variant(int paramInt1, int paramInt2)
  {
    set(paramInt1, paramInt2);
  }

  public int compareTo(Variant paramVariant)
  {
    if ((this.vartype == 14) || (paramVariant.vartype == 14))
      return DateTime.compare(toDate(), paramVariant.toDate());
    if ((this.vartype == 15) || (paramVariant.vartype == 15))
      return toString().compareTo(paramVariant.toString());
    double d1 = toDouble();
    double d2 = paramVariant.toDouble();
    if (d1 > d2)
      return 1;
    return d1 < d2 ? -1 : 0;
  }

  public int compareTo(Object paramObject)
  {
    if (this.val == paramObject)
      return 0;
    if (paramObject == null)
      return 1;
    if ((paramObject instanceof Variant))
      return compareTo((Variant)paramObject);
    if (((this.val instanceof BigDecimal)) || ((paramObject instanceof BigDecimal)))
      return toDecimal().compareTo(Obj.toDecimal(paramObject));
    if (((this.val instanceof Number)) || ((paramObject instanceof Number)))
    {
      double d1 = toDouble();
      double d2 = Obj.toDouble(paramObject);
      if (d1 < d2)
        return -1;
      if (d1 > d2)
        return 1;
      return 0;
    }
    if (((this.val instanceof String)) || ((paramObject instanceof String)))
      return toString().compareTo(Obj.toString(paramObject));
    if (this.val.equals(paramObject))
      return 0;
    return 1;
  }

  public boolean equals(Object paramObject)
  {
    if (this == paramObject)
      return true;
    if ((paramObject == null) && (this.val == null))
      return true;
    if (this.val == null)
      return toString().equals(paramObject.toString());
    if (paramObject == null)
      return false;
    if (this.val.equals(paramObject))
      return true;
    if ((paramObject instanceof java.util.Date))
      return toDate().equals(paramObject);
    if ((paramObject instanceof Calendar))
      return toDate().equals(((Calendar)paramObject).getTime());
    if ((paramObject instanceof String))
      return toString().equals(paramObject);
    if (((paramObject instanceof Number)) && ((this.val instanceof Number)))
      return toDouble() == ((Number)paramObject).doubleValue();
    if ((paramObject instanceof Variant))
    {
      Variant localVariant = (Variant)paramObject;
      if (localVariant.val == null)
        return toString().equals(localVariant.toString());
      if (this.val.equals(localVariant.val))
        return true;
      if ((this.vartype == 14) || (localVariant.vartype == 14))
        return toDate().equals(localVariant.toDate());
      if (((this.val instanceof Number)) && ((localVariant.val instanceof Number)))
        return toDouble() == localVariant.toDouble();
    }
    return false;
  }

  public boolean equals(int paramInt)
  {
    return equals(new Integer(paramInt));
  }

  public boolean equals(double paramDouble)
  {
    return equals(new Double(paramDouble));
  }

  public int hashCode()
  {
    if (this.val == null)
      return 0;
    return this.val.hashCode();
  }

  public final int getVarType()
  {
    return this.vartype;
  }

  public static int getVarType(Class paramClass)
  {
    int i = 13;
    if (paramClass == Integer.class)
      i = 4;
    else if (paramClass == String.class)
      i = 15;
    else if (paramClass == Double.class)
      i = 8;
    else if (paramClass == Boolean.class)
      i = 11;
    else if (paramClass == Variant.class)
      i = 12;
    else if (paramClass == java.util.Date.class)
      i = 14;
    else if (paramClass == Calendar.class)
      i = 14;
    else if (paramClass == Float.class)
      i = 7;
    else if (paramClass == Byte.class)
      i = 2;
    else if (paramClass == Short.class)
      i = 3;
    else if (paramClass == Long.class)
      i = 6;
    else if (paramClass == BigDecimal.class)
      i = 10;
    else if (paramClass == Throwable.class)
      i = 5;
    return i;
  }

  public static int getVarType(Object paramObject)
  {
    int i = 13;
    if (paramObject == null)
      return i;
    if ((paramObject instanceof String))
    {
      i = 15;
    }
    else if ((paramObject instanceof java.util.Date))
    {
      i = 14;
    }
    else if ((paramObject instanceof Calendar))
    {
      i = 14;
    }
    else if ((paramObject instanceof Number))
    {
      if ((paramObject instanceof Integer))
        i = 4;
      else if ((paramObject instanceof Double))
        i = 8;
      else if ((paramObject instanceof Float))
        i = 7;
      else if ((paramObject instanceof Byte))
        i = 2;
      else if ((paramObject instanceof Short))
        i = 3;
      else if ((paramObject instanceof BigDecimal))
        i = 10;
      else
        i = 6;
    }
    else if ((paramObject instanceof Variant))
    {
      Variant localVariant = (Variant)paramObject;
      i = localVariant.vartype;
    }
    else if ((paramObject instanceof Boolean))
    {
      i = 11;
    }
    else if ((paramObject instanceof byte[]))
    {
      i = 8194;
    }
    else if ((paramObject instanceof String[]))
    {
      i = 8207;
    }
    else if ((paramObject instanceof int[]))
    {
      i = 8196;
    }
    else if ((paramObject instanceof double[]))
    {
      i = 8200;
    }
    else if ((paramObject instanceof Variant[]))
    {
      i = 8204;
    }
    else if ((paramObject instanceof Object[]))
    {
      i = 8205;
    }
    else if ((paramObject instanceof float[]))
    {
      i = 8199;
    }
    else if ((paramObject instanceof short[]))
    {
      i = 8195;
    }
    else if ((paramObject instanceof String[][]))
    {
      i = 8207;
    }
    else if ((paramObject instanceof int[][]))
    {
      i = 8196;
    }
    else if ((paramObject instanceof double[][]))
    {
      i = 8200;
    }
    else if ((paramObject instanceof Variant[][]))
    {
      i = 8204;
    }
    return i;
  }

  public static String typeName(int paramInt)
  {
    if ((paramInt & 0x2000) == 8192)
      return typeName(paramInt & 0xFF) + "()";
    switch (paramInt)
    {
    case 0:
      return "Empty";
    case 1:
      return "Null";
    case 4:
      return "Integer";
    case 6:
      return "Long";
    case 7:
      return "Float";
    case 8:
      return "Double";
    case 14:
      return "Date";
    case 15:
      return "String";
    case 13:
      return "Object";
    case 5:
      return "Error";
    case 11:
      return "Boolean";
    case 12:
      return "Variant";
    case 10:
      return "BigDecimal";
    case 2:
      return "Byte";
    case 3:
      return "Short";
    case 9:
    }
    return "Empty";
  }

  public final String getTypeName()
  {
    if (this.vartype == 13)
    {
      if (this.val == null)
        return "Nothing";
      String str = this.val.getClass().getName();
      int i = str.lastIndexOf('.');
      if (i < 0)
        return str;
      return str.substring(i + 1);
    }
    return typeName(this.vartype);
  }

  public void changeType(int paramInt)
    throws Exception
  {
    if (paramInt == this.vartype)
      return;
    switch (paramInt)
    {
    case 0:
      return;
    case 1:
      return;
    case 13:
      return;
    case 5:
      return;
    case 201:
      return;
    case 205:
      return;
    case 4:
      set(toInt());
      return;
    case 6:
      set(toLong());
      return;
    case 7:
      set(toFloat());
      return;
    case 8:
      set(toDouble());
      return;
    case 14:
      set(toDate());
      return;
    case 15:
    case 129:
    case 200:
      set(toString());
      return;
    case 11:
      set(toBoolean());
      return;
    case 10:
    case 131:
      set(toDecimal());
      return;
    case 2:
      set(toByte());
      return;
    case 3:
      set(toShort());
      return;
    case 128:
    case 204:
    case 8194:
      if (!(this.val instanceof byte[]))
        set(toString().getBytes(), 8194);
      return;
    case 133:
      if (!(this.val instanceof java.sql.Date))
        set(new java.sql.Date(toDateNew().getTime()));
      return;
    case 135:
      if (!(this.val instanceof Timestamp))
        set(new Timestamp(toDateNew().getTime()));
      return;
    case 134:
      if (!(this.val instanceof Time))
        set(new Time(toDateNew().getTime()));
      return;
    }
    throw new Exception("Can't changeType from " + typeName(this.vartype) + " to a " + typeName(paramInt));
  }

  public final void set(Object paramObject)
  {
    if ((paramObject instanceof Variant))
    {
      Variant localVariant = (Variant)paramObject;
      this.val = localVariant.val;
      this.vartype = localVariant.vartype;
    }
    else
    {
      this.val = paramObject;
      this.vartype = (short)getVarType(paramObject);
    }
  }

  public final void set(Object paramObject, int paramInt)
  {
    this.val = paramObject;
    this.vartype = (short)paramInt;
  }

  public final void set(java.util.Date paramDate)
  {
    this.val = paramDate;
    if (paramDate == null)
      this.vartype = 1;
    else
      this.vartype = 14;
  }

  public final void set(boolean paramBoolean)
  {
    this.val = new Boolean(paramBoolean);
    this.vartype = 11;
  }

  public final void set(byte paramByte)
  {
    this.val = new Byte(paramByte);
    this.vartype = 2;
  }

  public final void set(short paramShort)
  {
    this.val = new Short(paramShort);
    this.vartype = 3;
  }

  public final void set(int paramInt)
  {
    this.val = new Integer(paramInt);
    this.vartype = 4;
  }

  public final void set(int paramInt1, int paramInt2)
  {
    this.val = new Integer(paramInt1);
    this.vartype = (short)paramInt2;
  }

  public final void set(long paramLong)
  {
    this.val = new Long(paramLong);
    this.vartype = 6;
  }

  public final void set(BigDecimal paramBigDecimal)
  {
    this.val = paramBigDecimal;
    this.vartype = 10;
  }

  public final void set(float paramFloat)
  {
    this.val = new Float(paramFloat);
    this.vartype = 7;
  }

  public final void set(double paramDouble)
  {
    this.val = new Double(paramDouble);
    this.vartype = 8;
  }

  public final void set(String paramString)
  {
    this.val = paramString;
    if (paramString == null)
      this.vartype = 1;
    else
      this.vartype = 15;
  }

  public final Variant setNull()
  {
    this.val = null;
    this.vartype = 1;
    return this;
  }

  public final boolean toBoolean()
  {
    return Obj.toBoolean(this.val);
  }

  public final int toInt()
  {
    return Obj.toInt(this.val);
  }

  public final long toLong()
  {
    return Obj.toLong(this.val);
  }

  public final double toDouble()
  {
    return Obj.toDouble(this.val);
  }

  public final BigDecimal toDecimal()
  {
    return Obj.toDecimal(this.val);
  }

  public final String toString()
  {
    return Obj.toString(this.val);
  }

  public final byte[] toBytes()
  {
    if (this.val == null)
      return null;
    if ((this.val instanceof byte[]))
      return (byte[])this.val;
    if ((this.val instanceof InputStream))
      try
      {
        InputStream localInputStream = (InputStream)this.val;
        byte[] arrayOfByte = new byte[localInputStream.available()];
        localInputStream.read(arrayOfByte);
        return arrayOfByte;
      }
      catch (Exception localException)
      {
        System.out.println("Variant.toBytes(InputStream): " + localException);
      }
    return this.val.toString().getBytes();
  }

  public final byte toByte()
  {
    return (byte)toInt();
  }

  public final short toShort()
  {
    return (short)toInt();
  }

  public final float toFloat()
  {
    return (float)toDouble();
  }

  public final java.util.Date toDate()
  {
    try
    {
      switch (this.vartype)
      {
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
        return DateTime.toDate(toInt());
      case 7:
      case 8:
      case 10:
        return DateTime.toDate(toDouble());
      case 15:
        return DateTime.toDate((String)this.val);
      case 13:
      case 14:
        if ((this.val instanceof java.util.Date))
          return (java.util.Date)this.val;
        if ((this.val instanceof Calendar))
          return ((Calendar)this.val).getTime();
      case 9:
      case 11:
      case 12:
      }
    }
    catch (Exception localException)
    {
    }
    return null;
  }

  public final java.util.Date toDateNew()
  {
    java.util.Date localDate = toDate();
    if (localDate == null)
      return new java.util.Date();
    return localDate;
  }

  public final Object toObject()
  {
    return this.val;
  }

  public final boolean isArray()
  {
    return (this.vartype & 0x2000) == 8192;
  }

  public final boolean isDate()
  {
    if ((this.val instanceof java.util.Date))
      return true;
    if (this.vartype == 14)
      return true;
    if ((this.val instanceof Calendar))
      return true;
    if ((this.val instanceof String))
    {
      java.util.Date localDate = toDate();
      if (localDate != null)
        return true;
    }
    return false;
  }

  public final boolean isEmpty()
  {
    return this.vartype == 0;
  }

  public final boolean isError()
  {
    return this.vartype == 5;
  }

  public final boolean isNull()
  {
    return this.vartype == 1;
  }

  public static boolean isNumeric(Class paramClass)
  {
    return isNumericType(getVarType(paramClass));
  }

  private static boolean isNumericType(int paramInt)
  {
    return (paramInt >= 2) && (paramInt <= 10);
  }

  public final boolean isNumeric()
  {
    if ((this.val instanceof Number))
      return true;
    if (isNumericType(this.vartype))
      return true;
    if ((this.val instanceof String))
    {
      String str = (String)this.val;
      try
      {
        str = Obj.trimNumericString(str);
        NumberFormat localNumberFormat = NumberFormat.getNumberInstance();
        ParsePosition localParsePosition = new ParsePosition(0);
        Number localNumber = localNumberFormat.parse(str, localParsePosition);
        return (localNumber != null) && (localParsePosition.getIndex() >= str.length());
      }
      catch (Exception localException)
      {
      }
    }
    return false;
  }

  public final boolean isObject()
  {
    return this.vartype == 13;
  }

  public Object clone()
  {
    try
    {
      Variant localVariant = (Variant)super.clone();
      return localVariant;
    }
    catch (CloneNotSupportedException localCloneNotSupportedException)
    {
    }
    return null;
  }

  public final Variant invoke(String paramString, Variant[] paramArrayOfVariant)
    throws IllegalAccessException, InvocationTargetException
  {
    return invoke(this.val, paramString, paramArrayOfVariant);
  }

  public static Variant invoke(Object paramObject, String paramString, Variant[] paramArrayOfVariant)
    throws IllegalAccessException, InvocationTargetException
  {
    if (paramObject == null)
      return new Variant().setNull();
    Class localClass1 = null;
    if ((paramObject instanceof Class))
      localClass1 = (Class)paramObject;
    else
      localClass1 = paramObject.getClass();
    Method[] arrayOfMethod = localClass1.getMethods();
    if (arrayOfMethod != null)
      for (int i = 0; i < arrayOfMethod.length; i++)
      {
        Method localMethod = arrayOfMethod[i];
        if (!localMethod.getName().equals(paramString))
          continue;
        int j = paramArrayOfVariant == null ? 0 : paramArrayOfVariant.length;
        Class[] arrayOfClass = localMethod.getParameterTypes();
        Object[] arrayOfObject = new Object[arrayOfClass.length];
        Variant localVariant = null;
        for (int k = 0; k < arrayOfClass.length; k++)
        {
          if (k >= j)
            localVariant = new Variant();
          else
            localVariant = paramArrayOfVariant[k];
          Object localObject = localVariant.toObject();
          Class localClass2 = arrayOfClass[k];
          if ((localObject == null) || (localObject.getClass() != localClass2))
            if (localClass2 == String.class)
              localObject = localVariant.toString();
            else if (localClass2 == Integer.TYPE)
              localObject = new Integer(localVariant.toInt());
            else if (localClass2 == Double.TYPE)
              localObject = new Double(localVariant.toDouble());
            else if (localClass2 == Float.TYPE)
              localObject = new Float(localVariant.toFloat());
            else if (localClass2 == Boolean.TYPE)
              localObject = new Boolean(localVariant.toBoolean());
            else if (localClass2 == Byte.TYPE)
              localObject = new Byte(localVariant.toByte());
            else if (localClass2 == Short.TYPE)
              localObject = new Short(localVariant.toShort());
            else
              localObject = localVariant.toObject();
          arrayOfObject[k] = localObject;
        }
        return new Variant(localMethod.invoke(paramObject, arrayOfObject));
      }
    return (Variant)null;
  }

  public final void setValue(String paramString, Variant paramVariant)
    throws IllegalAccessException, NoSuchFieldException
  {
    setValue(this.val, paramString, paramVariant);
  }

  public static void setValue(Object paramObject, String paramString, Variant paramVariant)
    throws IllegalAccessException, NoSuchFieldException
  {
    try
    {
      Field localField = paramObject.getClass().getField(paramString);
      if (localField.getType() == Variant.class)
        localField.set(paramObject, paramVariant);
      else
        localField.set(paramObject, paramVariant.val);
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      Variant[] arrayOfVariant = new Variant[1];
      arrayOfVariant[0] = paramVariant;
      try
      {
        invoke(paramObject, "set" + paramString, arrayOfVariant);
      }
      catch (InvocationTargetException localInvocationTargetException)
      {
        throw localNoSuchFieldException;
      }
    }
  }

  public final Variant getValue(String paramString)
    throws IllegalAccessException, NoSuchFieldException
  {
    return getValue(this.val, paramString);
  }

  public static Variant getValue(Object paramObject, String paramString)
    throws IllegalAccessException, NoSuchFieldException
  {
    try
    {
      Field localField = paramObject.getClass().getField(paramString);
      return new Variant(localField.get(paramObject));
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      try
      {
        return invoke(paramObject, "get" + paramString, null);
      }
      catch (InvocationTargetException localInvocationTargetException)
      {
      }
      throw localNoSuchFieldException;
    }    
  }

  public final Variant getValue(String paramString, int[] paramArrayOfInt)
    throws IllegalAccessException, NoSuchFieldException
  {
    int i = 0;
    Field localField = this.val.getClass().getField(paramString);
    Object localObject = localField.get(this.val);
    for (i = 0; i < paramArrayOfInt.length; i++)
      localObject = Array.get(localObject, paramArrayOfInt[i]);
    if ((localObject instanceof Variant))
      return (Variant)localObject;
    return new Variant(localObject);
  }

  public final Variant getValueAt(int paramInt)
  {
    Object localObject = Array.get(this.val, paramInt);
    if ((localObject instanceof Variant))
      return (Variant)localObject;
    return new Variant(localObject);
  }

  public final Variant getValueAt(int[] paramArrayOfInt)
  {
    int i = 0;
    Object localObject = this.val;
    for (i = 0; i < paramArrayOfInt.length; i++)
      localObject = Array.get(localObject, paramArrayOfInt[i]);
    if ((localObject instanceof Variant))
      return (Variant)localObject;
    return new Variant(localObject);
  }

  public final void setValueAt(int paramInt, Variant paramVariant)
  {
    Class localClass = null;
    localClass = this.val.getClass().getComponentType();
    if (localClass == Variant.class)
    {
      Object localObject = Array.get(this.val, paramInt);
      if ((localObject instanceof Variant))
        ((Variant)localObject).set(paramVariant);
      else
        Array.set(this.val, paramInt, new Variant(paramVariant));
    }
    else
    {
      Array.set(this.val, paramInt, paramVariant.val);
    }
  }

  public final void setValueAt(int[] paramArrayOfInt, Variant paramVariant)
  {
    Class localClass = null;
    int i = 0;
    Object localObject1 = this.val;
    for (i = 0; i < paramArrayOfInt.length - 1; i++)
      localObject1 = Array.get(localObject1, paramArrayOfInt[i]);
    localClass = localObject1.getClass().getComponentType();
    if (localClass == Variant.class)
    {
      Object localObject2 = Array.get(localObject1, paramArrayOfInt[i]);
      if ((localObject2 instanceof Variant))
        ((Variant)localObject2).set(paramVariant);
      else
        Array.set(localObject1, paramArrayOfInt[i], new Variant(paramVariant));
    }
    else
    {
      Array.set(localObject1, paramArrayOfInt[i], paramVariant.val);
    }
  }

  private static int combineArithVarTypes(Variant paramVariant1, Variant paramVariant2)
  {
    int i = paramVariant1.vartype;
    int j = paramVariant2.vartype;
    int k = i > j ? i : j;
    if (k == 12)
      if (i == 12)
        k = j;
      else
        k = i;
    if ((i == 15) || (j == 15))
    {
      k = 15;
      if ((paramVariant1.isNumeric()) && (paramVariant2.isNumeric()))
        if (isNumericType(i))
          k = i;
        else if (isNumericType(j))
          k = j;
        else
          k = 8;
    }
    if ((k < 2) || (k > 10))
      if ((i == 14) || (j == 14))
        k = 14;
      else if (isNumericType(i))
        k = i;
      else
        k = j;
    switch (k)
    {
    case 2:
    case 3:
    case 11:
      k = 4;
      break;
    case 7:
      k = 8;
    case 4:
    case 5:
    case 6:
    case 8:
    case 9:
    case 10:
    }
    return k;
  }

  public final Variant add(Variant paramVariant)
  {
    int i = combineArithVarTypes(this, paramVariant);
    if (i == 15)
      return new Variant(toString() + paramVariant.toString());
    switch (i)
    {
    case 4:
      return new Variant(toInt() + paramVariant.toInt());
    case 6:
      return new Variant(toLong() + paramVariant.toLong());
    case 8:
      return new Variant(toDouble() + paramVariant.toDouble());
    case 10:
      return new Variant(toDecimal().add(paramVariant.toDecimal()));
    case 14:
      return new Variant(this.vartype == 14 ? DateTime.add(toDate(), paramVariant.toDouble()) : DateTime.add(paramVariant.toDate(), toDouble()));
    case 5:
    case 7:
    case 9:
    case 11:
    case 12:
    case 13:
    }
    return this;
  }

  public final Variant subtract(Variant paramVariant)
  {
    int i = combineArithVarTypes(this, paramVariant);
    switch (i)
    {
    case 4:
      return new Variant(toInt() - paramVariant.toInt());
    case 6:
      return new Variant(toLong() - paramVariant.toLong());
    case 8:
      return new Variant(toDouble() - paramVariant.toDouble());
    case 10:
      return new Variant(toDecimal().subtract(paramVariant.toDecimal()));
    case 14:
      return new Variant(DateTime.subtract(toDate(), paramVariant.toDouble()));
    case 5:
    case 7:
    case 9:
    case 11:
    case 12:
    case 13:
    }
    return this;
  }

  public final Variant multiply(Variant paramVariant)
  {
    int i = combineArithVarTypes(this, paramVariant);
    switch (i)
    {
    case 4:
      return new Variant(toInt() * paramVariant.toInt());
    case 6:
      return new Variant(toLong() * paramVariant.toLong());
    case 8:
      return new Variant(toDouble() * paramVariant.toDouble());
    case 10:
      return new Variant(toDecimal().multiply(paramVariant.toDecimal()));
    case 5:
    case 7:
    case 9:
    }
    return this;
  }

  public final Variant divide(Variant paramVariant)
  {
    int i = combineArithVarTypes(this, paramVariant);
    switch (i)
    {
    case 4:
      return new Variant(toInt() / paramVariant.toInt());
    case 6:
      return new Variant(toLong() / paramVariant.toLong());
    case 8:
      return new Variant(toDouble() / paramVariant.toDouble());
    case 10:
      return new Variant(toDecimal().divide(paramVariant.toDecimal(), 6));
    case 5:
    case 7:
    case 9:
    }
    return this;
  }

  public final Variant negate()
  {
    switch (this.vartype)
    {
    case 2:
    case 3:
    case 4:
      return new Variant(-toInt());
    case 6:
      return new Variant(-toLong());
    case 7:
    case 8:
      return new Variant(-toDouble());
    case 10:
      return new Variant(toDecimal().negate());
    case 5:
    case 9:
    }
    return this;
  }
}