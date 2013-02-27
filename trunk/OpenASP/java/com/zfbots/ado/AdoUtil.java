package com.zfbots.ado;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Properties;

import com.zfbots.util.Variant;


public class AdoUtil
{
  public static void registerDriver(String paramString)
    throws AdoError
  {
    if ((paramString == null) || (paramString.equals("")))
      return;
    Driver localDriver = null;
    try
    {
      localDriver = (Driver)Class.forName(paramString).newInstance();
      DriverManager.registerDriver(localDriver);
    }
    catch (Exception localException)
    {
      throw new AdoError(localException, "error registering driver: " + paramString, 0);
    }
  }

  public static void createLogFile(String paramString)
  {
    try
    {
      Connection.setLogWriter(new PrintWriter(new FileWriter(paramString), true));
    }
    catch (Exception localException)
    {
      System.out.println("AdoUtil.createLogFile: " + localException);
    }
  }

  static void appendToData(Variant paramVariant1, Variant paramVariant2, InputStream paramInputStream)
    throws AdoError
  {
    InputStream localInputStream = null;
    Object localObject = paramVariant2.toObject();
    if (localObject == null)
      throw new AdoError(null, "Field value is null.", 30008);
    if ((localObject instanceof byte[]))
      localInputStream = getBuf(paramInputStream, (byte[])localObject);
    else if ((localObject instanceof String))
      localInputStream = getBuf(paramInputStream, ((String)localObject).getBytes());
    else
      AdoError.throwNotSupported("appendToData data: " + localObject);
    paramVariant1.set(localInputStream);
  }

  static InputStream getBuf(InputStream paramInputStream, byte[] paramArrayOfByte)
    throws AdoError
  {
    byte[] arrayOfByte1 = null;
    byte[] arrayOfByte2 = null;
    int i = ((byte[])paramArrayOfByte).length;
    int j = 0;
    int k = 0;
    ByteArrayInputStream localByteArrayInputStream = null;
    try
    {
      if (paramInputStream != null)
      {
        k = paramInputStream.available();
        arrayOfByte2 = new byte[k];
        paramInputStream.reset();
        paramInputStream.read(arrayOfByte2, 0, k);
      }
      j = i + k;
      arrayOfByte1 = new byte[j];
      if (arrayOfByte2 != null)
        System.arraycopy(arrayOfByte2, 0, arrayOfByte1, 0, k);
      System.arraycopy((byte[])paramArrayOfByte, 0, arrayOfByte1, k, i);
      localByteArrayInputStream = new ByteArrayInputStream(arrayOfByte1);
    }
    catch (IOException localIOException)
    {
      throw new AdoError(localIOException, "Error reading from input stream.", 0);
    }
    return localByteArrayInputStream;
  }

  private static ByteArrayInputStream getBAIStream(InputStream paramInputStream)
    throws IOException
  {
    ByteArrayInputStream localByteArrayInputStream = null;
    byte[] arrayOfByte = getBytesFromStream(paramInputStream);
    if ((arrayOfByte != null) && (arrayOfByte.length > 0))
      localByteArrayInputStream = new ByteArrayInputStream(arrayOfByte);
    return localByteArrayInputStream;
  }

  static byte[] getBytesFromStream(InputStream paramInputStream)
    throws IOException
  {
    Object localObject = null;
    if (paramInputStream != null)
    {
      byte[] arrayOfByte1 = new byte[1012];
      byte[] arrayOfByte2 = null;
      int i = 0;
      while (true)
      {
        int j = paramInputStream.read(arrayOfByte1);
        if (j <= 0)
          break;
        if (i > 0)
        {
          arrayOfByte2 = new byte[i];
          System.arraycopy(localObject, 0, arrayOfByte2, 0, i);
        }
        i += j;
        localObject = new byte[i];
        if (i > j)
          System.arraycopy(arrayOfByte2, 0, localObject, 0, arrayOfByte2.length);
        System.arraycopy(arrayOfByte1, 0, localObject, i - j, j);
      }
    }
    return localObject.toString().getBytes();
  }

  static void getColValue(ResultSet paramResultSet, int paramInt1, int paramInt2, Variant paramVariant)
    throws AdoError
  {
    try
    {
      Object localObject1;
      Object localObject2;
      switch (paramInt1)
      {
      case 1:
      case 12:
        localObject1 = paramResultSet.getString(paramInt2);
        if (localObject1 == null)
          break;
        paramVariant.set((String)localObject1);
        break;
      case -3:
      case -2:
        localObject1 = paramResultSet.getBytes(paramInt2);
        if (localObject1 == null)
          break;
        paramVariant.set(localObject1, 8194);
        break;
      case 4:
        paramVariant.set(paramResultSet.getInt(paramInt2));
        break;
      case 5:
        paramVariant.set(paramResultSet.getShort(paramInt2));
        break;
      case -6:
        paramVariant.set(paramResultSet.getByte(paramInt2));
        break;
      case -5:
        paramVariant.set(paramResultSet.getLong(paramInt2));
        break;
      case 6:
      case 8:
        paramVariant.set(paramResultSet.getDouble(paramInt2));
        break;
      case 7:
        paramVariant.set(paramResultSet.getFloat(paramInt2));
        break;
      case 2:
      case 3:
        localObject1 = paramResultSet.getString(paramInt2);
        if (localObject1 == null)
          break;
        localObject2 = new BigDecimal((String)localObject1);
        paramVariant.set(localObject2, 10);
        break;
      case -7:
        paramVariant.set(paramResultSet.getBoolean(paramInt2));
        break;
      case 9:
      case 91:
        localObject1 = paramResultSet.getDate(paramInt2);
        if (localObject1 == null)
          break;
        paramVariant.set((java.util.Date)localObject1);
        break;
      case 10:
      case 92:
        localObject1 = paramResultSet.getTime(paramInt2);
        if (localObject1 == null)
          break;
        paramVariant.set((java.util.Date)localObject1);
        break;
      case 11:
      case 93:
        localObject1 = paramResultSet.getTimestamp(paramInt2);
        if (localObject1 == null)
          break;
        paramVariant.set((java.util.Date)localObject1);
        break;
      case -4:
      case 2004:
        localObject1 = paramResultSet.getBinaryStream(paramInt2);
        if (localObject1 != null)
        {
          localObject2 = getBytesFromStream((InputStream)localObject1);
          paramVariant.set(localObject2);
        }
        else
        {
          paramVariant.setNull();
        }
        break;
      case -1:
      case 2005:
        localObject1 = paramResultSet.getCharacterStream(paramInt2);
        if (localObject1 != null)
        {
          char[] arrayOfChar = new char[64];
          StringBuffer localStringBuffer = new StringBuffer();
          int i;
          while ((i = ((Reader)localObject1).read(arrayOfChar)) > 0)
            localStringBuffer.append(arrayOfChar, 0, i);
          paramVariant.set(localStringBuffer.toString());
        }
        else
        {
          paramVariant.setNull();
        }
        break;
      default:
        localObject1 = paramResultSet.getString(paramInt2);
        if (localObject1 == null)
          break;
        paramVariant.set((String)localObject1);
      }
      if (paramResultSet.wasNull())
        paramVariant.setNull();
    }
    catch (Exception localException)
    {
      throw new AdoError(localException, "error fetching values. (in AdoUtil.getColValue)", 69004);
    }
  }

  static void getColValue(CallableStatement paramCallableStatement, int paramInt1, int paramInt2, Variant paramVariant)
    throws AdoError
  {
    try
    {
      Object localObject;
      switch (paramInt1)
      {
      case -1:
      case 1:
      case 12:
        localObject = paramCallableStatement.getString(paramInt2);
        if (localObject == null)
          break;
        paramVariant.set((String)localObject);
        break;
      case -3:
      case -2:
        localObject = paramCallableStatement.getBytes(paramInt2);
        if (localObject == null)
          break;
        paramVariant.set(localObject, 8194);
        break;
      case 4:
        paramVariant.set(paramCallableStatement.getInt(paramInt2));
        break;
      case 5:
        paramVariant.set(paramCallableStatement.getShort(paramInt2));
        break;
      case -6:
        paramVariant.set(paramCallableStatement.getByte(paramInt2));
        break;
      case -5:
        paramVariant.set(paramCallableStatement.getLong(paramInt2));
        break;
      case 6:
      case 8:
        paramVariant.set(paramCallableStatement.getDouble(paramInt2));
        break;
      case 7:
        paramVariant.set(paramCallableStatement.getFloat(paramInt2));
        break;
      case 2:
      case 3:
        localObject = paramCallableStatement.getString(paramInt2);
        if (localObject == null)
          break;
        BigDecimal localBigDecimal = new BigDecimal((String)localObject);
        paramVariant.set(localBigDecimal, 10);
        break;
      case -7:
        paramVariant.set(paramCallableStatement.getBoolean(paramInt2));
        break;
      case 9:
      case 91:
        localObject = paramCallableStatement.getDate(paramInt2);
        if (localObject == null)
          break;
        paramVariant.set((java.util.Date)localObject);
        break;
      case 10:
      case 92:
        localObject = paramCallableStatement.getTime(paramInt2);
        if (localObject == null)
          break;
        paramVariant.set((java.util.Date)localObject);
        break;
      case 11:
      case 93:
        localObject = paramCallableStatement.getTimestamp(paramInt2);
        if (localObject == null)
          break;
        paramVariant.set((java.util.Date)localObject);
        break;
      case -4:
        throw new AdoError(null, "Invalid datatype.", 70041);
      default:
        localObject = paramCallableStatement.getString(paramInt2);
        if (localObject == null)
          break;
        paramVariant.set((String)localObject);
      }
      if (paramCallableStatement.wasNull())
        paramVariant.setNull();
    }
    catch (Exception localException)
    {
      throw new AdoError(localException, "error fetching values.", 69004);
    }
  }

  static void getColValue(int paramInt, Field paramField, Variant paramVariant)
    throws AdoError
  {
    if (paramField.isNull())
    {
      paramVariant.setNull();
    }
    else
    {
      Variant localVariant = paramField.getValue();
      try
      {
        paramVariant.set(localVariant.toObject(), localVariant.getVarType());
        int i = getAdoType(paramInt);
        paramVariant.changeType(i);
      }
      catch (Exception localException)
      {
        throw new AdoError(localException, "error in AdoUtil.getColValue");
      }
    }
  }

  static void copyProps(Properties paramProperties1, Properties paramProperties2)
  {
    Enumeration localEnumeration = paramProperties1.propertyNames();
    while (localEnumeration.hasMoreElements())
    {
      String str = (String)localEnumeration.nextElement();
      paramProperties2.put(str, paramProperties1.get(str));
    }
  }

  public static java.sql.Date getDate(Variant paramVariant)
    throws AdoError
  {
    java.sql.Date localDate = null;
    Object localObject = paramVariant.toObject();
    if ((localObject instanceof java.sql.Date))
    {
      localDate = (java.sql.Date)localObject;
    }
    else if ((localObject instanceof Timestamp))
    {
      localDate = new java.sql.Date(((Timestamp)localObject).getTime());
    }
    else if ((localObject instanceof java.util.Date))
    {
      localDate = new java.sql.Date(((java.util.Date)localObject).getTime());
    }
    else if (paramVariant.getVarType() == 15)
    {
      java.util.Date localDate1 = paramVariant.toDate();
      if (localDate1 != null)
        localDate = new java.sql.Date(localDate1.getTime());
      else
        try
        {
          localDate = java.sql.Date.valueOf(paramVariant.toString());
        }
        catch (Exception localException)
        {
          throw new AdoError(localException, "Error during data type conversion.", 0);
        }
    }
    else if (paramVariant.getVarType() != 1)
    {
      throwIncompatibleDataType("DATE", paramVariant.getVarType());
    }
    return localDate;
  }

  public static Time getTime(Variant paramVariant)
    throws AdoError
  {
    Time localTime = null;
    Object localObject = paramVariant.toObject();
    if ((localObject instanceof Time))
    {
      localTime = (Time)localObject;
    }
    else if ((localObject instanceof Timestamp))
    {
      localTime = new Time(((Timestamp)localObject).getTime());
    }
    else if ((localObject instanceof java.util.Date))
    {
      localTime = new Time(((java.util.Date)localObject).getTime());
    }
    else if (paramVariant.getVarType() == 15)
    {
      java.util.Date localDate = paramVariant.toDate();
      if (localDate != null)
        localTime = new Time(localDate.getTime());
      else
        try
        {
          localTime = Time.valueOf(paramVariant.toString());
        }
        catch (Exception localException)
        {
          throw new AdoError(localException, "Error during data type conversion.", 0);
        }
    }
    else if (paramVariant.getVarType() != 1)
    {
      throwIncompatibleDataType("TIME", paramVariant.getVarType());
    }
    return localTime;
  }

  public static Timestamp getTimestamp(Variant paramVariant)
    throws AdoError
  {
    Timestamp localTimestamp = null;
    Object localObject = paramVariant.toObject();
    if ((localObject instanceof Timestamp))
    {
      localTimestamp = (Timestamp)localObject;
    }
    else if ((localObject instanceof java.sql.Date))
    {
      localTimestamp = new Timestamp(((java.sql.Date)localObject).getTime());
    }
    else if ((localObject instanceof java.util.Date))
    {
      localTimestamp = new Timestamp(((java.util.Date)localObject).getTime());
    }
    else if (paramVariant.getVarType() == 15)
    {
      java.util.Date localDate = paramVariant.toDate();
      if (localDate != null)
        localTimestamp = new Timestamp(localDate.getTime());
      else
        try
        {
          localTimestamp = Timestamp.valueOf(paramVariant.toString());
        }
        catch (Exception localException)
        {
          System.out.println("error converting: " + paramVariant.toString());
          throw new AdoError(localException, "Error during data type conversion.", 0);
        }
    }
    else if (paramVariant.getVarType() != 1)
    {
      throwIncompatibleDataType("TIMESTAMP", paramVariant.getVarType());
    }
    return localTimestamp;
  }

  static InputStream getStream(Variant paramVariant)
    throws IOException
  {
    Object localObject1 = null;
    Object localObject2 = paramVariant.toObject();
    if (localObject2 == null)
      return null;
    if ((localObject2 instanceof ByteArrayInputStream))
    {
      localObject1 = (InputStream)localObject2;
      ((InputStream)localObject1).reset();
    }
    else if ((localObject2 instanceof InputStream))
    {
      localObject1 = getBAIStream((InputStream)localObject2);
    }
    if (localObject1 == null)
    {
      byte[] arrayOfByte = paramVariant.toBytes();
      localObject1 = new ByteArrayInputStream(arrayOfByte);
    }
    return (InputStream)localObject1;
  }

  public static int getAdoType(int paramInt)
    throws AdoError
  {
    int i = 13;
    switch (paramInt)
    {
    case -5:
      i = 6;
      break;
    case -2:
      i = 128;
      break;
    case -7:
    case 14:
      i = 11;
      break;
    case 16:
      i = 11;
      break;
    case -8:
    case 1:
      i = 129;
      break;
    case 3:
      i = 10;
      break;
    case 8:
      i = 8;
      break;
    case 6:
      i = 8;
      break;
    case 4:
      i = 4;
      break;
    case -4:
    case 2004:
      i = 205;
      break;
    case -10:
    case -1:
    case 2005:
      i = 201;
      break;
    case 0:
      i = 1;
      break;
    case 2:
      i = 10;
      break;
    case 1111:
      i = 13;
      break;
    case 7:
      i = 7;
      break;
    case 5:
      i = 3;
      break;
    case 9:
    case 91:
      i = 133;
      break;
    case 10:
    case 92:
      i = 134;
      break;
    case 11:
    case 93:
      i = 135;
      break;
    case -6:
      i = 2;
      break;
    case -3:
      i = 204;
      break;
    case -9:
    case 12:
      i = 200;
      break;
    default:
      throw new AdoError("Unknown SQL Type : " + paramInt);
    }
    return i;
  }

  public static int getSqlType(int paramInt)
    throws AdoError
  {
    int i = 1111;
    switch (paramInt)
    {
    case 6:
      i = -5;
      break;
    case 128:
      i = -2;
      break;
    case 11:
      i = -7;
      break;
    case 129:
      i = 1;
      break;
    case 133:
      i = 91;
      break;
    case 10:
      i = 3;
      break;
    case 8:
      i = 8;
      break;
    case 4:
      i = 4;
      break;
    case 205:
      i = -4;
      break;
    case 201:
      i = -1;
      break;
    case 1:
      i = 0;
      break;
    case 13:
      i = 1111;
      break;
    case 7:
      i = 7;
      break;
    case 3:
      i = 5;
      break;
    case 134:
      i = 92;
      break;
    case 135:
      i = 93;
      break;
    case 2:
      i = -6;
      break;
    case 204:
      i = -3;
      break;
    case 15:
    case 200:
      i = 12;
      break;
    default:
      throw new AdoError("Unknown ADO Type : " + paramInt);
    }
    return i;
  }

  public static int getAdoIsolationLevel(int paramInt)
  {
    int i = -1;
    switch (paramInt)
    {
    case 1:
      i = 256;
      break;
    case 2:
      i = 4096;
      break;
    case 4:
      i = 65536;
      break;
    case 8:
      i = 1048576;
      break;
    case 0:
    case 3:
    case 5:
    case 6:
    case 7:
    }
    return i;
  }

  public static int getJDBCIsolationLevel(int paramInt)
  {
    int i = 2;
    switch (paramInt)
    {
    case 256:
      i = 1;
      break;
    case 4096:
      i = 2;
      break;
    case 65536:
      i = 4;
      break;
    case 1048576:
      i = 8;
    }
    return i;
  }

  static void throwIncompatibleDataType(String paramString, int paramInt)
    throws AdoError
  {
    throw new AdoError(null, "Incompatible data types.  The value is of type: " + Variant.typeName(paramInt) + " " + "It cannot be converted to type:  " + paramString, 30009);
  }
}