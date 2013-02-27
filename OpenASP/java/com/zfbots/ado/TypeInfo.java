package com.zfbots.ado;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

class TypeInfo
{
  String typeName = null;
  short dataType;
  int precision = 0;
  String literalPrefix;
  String literalSuffix;
  String createParams;
  short nullable;
  boolean caseSensitive;
  short searchable;
  boolean unsignedAttribute;
  boolean fixedPrecScale;
  boolean autoIncrement;
  String localTypeName;
  short minimumScale;
  short maximumScale;
  int sqlDataType;
  int sqlDateTimeSub;
  int numPrecRadix;

  TypeInfo(ResultSet paramResultSet)
    throws Exception
  {
    try
    {
      this.typeName = paramResultSet.getString(1);
    }
    catch (Exception localException1)
    {
      processError(localException1, "typeName");
    }
    try
    {
      this.dataType = paramResultSet.getShort(2);
    }
    catch (Exception localException2)
    {
      processError(localException2, "dataType");
    }
    try
    {
      this.precision = paramResultSet.getInt(3);
    }
    catch (Exception localException3)
    {
      processError(localException3, "precision");
    }
    try
    {
      this.literalPrefix = paramResultSet.getString(4);
    }
    catch (Exception localException4)
    {
      processError(localException4, "literalPrefix");
    }
    try
    {
      this.literalSuffix = paramResultSet.getString(5);
    }
    catch (Exception localException5)
    {
      processError(localException5, "literalSuffix");
    }
    try
    {
      this.createParams = paramResultSet.getString(6);
    }
    catch (Exception localException6)
    {
      processError(localException6, "createParams");
    }
    try
    {
      this.nullable = paramResultSet.getShort(7);
    }
    catch (Exception localException7)
    {
      processError(localException7, "nullable");
    }
    try
    {
      this.caseSensitive = paramResultSet.getBoolean(8);
    }
    catch (Exception localException8)
    {
      processError(localException8, "caseSensitive");
    }
    try
    {
      this.searchable = paramResultSet.getShort(9);
    }
    catch (Exception localException9)
    {
      processError(localException9, "searchable");
    }
    try
    {
      this.unsignedAttribute = paramResultSet.getBoolean(10);
    }
    catch (Exception localException10)
    {
      processError(localException10, "unsignedAttribute");
    }
    try
    {
      this.fixedPrecScale = paramResultSet.getBoolean(11);
    }
    catch (Exception localException11)
    {
      processError(localException11, "fixedPrecScale");
    }
    try
    {
      this.autoIncrement = paramResultSet.getBoolean(12);
    }
    catch (Exception localException12)
    {
      processError(localException12, "autoIncrement");
    }
    try
    {
      this.localTypeName = paramResultSet.getString(13);
    }
    catch (Exception localException13)
    {
      processError(localException13, "localTypeName");
    }
    try
    {
      this.minimumScale = paramResultSet.getShort(14);
    }
    catch (Exception localException14)
    {
      processError(localException14, "minimumScale");
    }
    try
    {
      this.maximumScale = paramResultSet.getShort(15);
    }
    catch (Exception localException15)
    {
      processError(localException15, "maximumScale");
    }
    if (paramResultSet.getMetaData().getColumnCount() == 18)
    {
      try
      {
        this.sqlDataType = paramResultSet.getInt(16);
      }
      catch (Exception localException16)
      {
        processError(localException16, "sqlDataType");
      }
      try
      {
        this.sqlDateTimeSub = paramResultSet.getInt(17);
      }
      catch (Exception localException17)
      {
        processError(localException17, "sqlDateTimeSub");
      }
      try
      {
        this.numPrecRadix = paramResultSet.getInt(18);
      }
      catch (Exception localException18)
      {
        processError(localException18, "numPrecRadix");
      }
    }
  }

  void processError(Exception paramException, String paramString)
    throws Exception
  {
    System.out.print("TypeInfo(");
    if (this.typeName != null)
      System.out.print(this.typeName + ",");
    System.out.println(paramString + ") unavailable: " + paramException);
    throw paramException;
  }
}