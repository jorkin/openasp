package com.zfbots.ado;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.zfbots.util.Variant;


public class Field
{
  private boolean firstCallToAppend = false;
  private Variant data;
  private Variant oldData;
  private String name;
  private String sqlTypeName;
  private boolean hasChanged = false;
  private int precision = 0;
  private int scale = 0;
  private int type = 0;
  private int sqlType = 0;
  private int actualSize = -1;
  private int definedSize = 0;
  private Recordset ownerRS;
  private boolean searchable;
  private boolean autoInc;
  private boolean currency;
  private boolean readOnly;
  private boolean updatable;
  private int nullable;
  private int attributes;
  private String tableName;

  private Field(Recordset paramRecordset)
  {
    this.ownerRS = paramRecordset;
    this.data = new Variant();
    this.oldData = new Variant();
    this.data.setNull();
    this.oldData.setNull();
  }

  public Field(Recordset paramRecordset, ResultSetMetaData paramResultSetMetaData, int paramInt)
    throws AdoError
  {
    this(paramRecordset);
    initialize(paramResultSetMetaData, paramInt);
  }

  public Field(Recordset paramRecordset, Field paramField)
    throws AdoError
  {
    this(paramRecordset);
    initialize(paramField);
  }

  public boolean equals(Object paramObject)
  {
    if (this == paramObject)
      return true;
    if ((paramObject instanceof Field))
    {
      Variant localVariant = ((Field)paramObject).data;
      if (this.data == localVariant)
        return true;
      if ((this.data != null) && (this.data.equals(localVariant)))
        return true;
    }
    return false;
  }

  public int getAttributes()
  {
    return this.attributes;
  }

  public int getActualSize()
  {
    if (this.data == null)
      return -1;
    try
    {
      Object localObject = this.data.toObject();
      if ((localObject instanceof String))
        return ((String)localObject).length();
      if ((localObject instanceof byte[]))
        return ((byte[])localObject).length;
      if ((localObject instanceof InputStream))
        return ((InputStream)localObject).available();
      if ((localObject instanceof Reader))
        return this.data.toString().length();
    }
    catch (Exception localException)
    {
    }
    return this.definedSize;
  }

  public String getName()
  {
    return this.name;
  }

  public int getDefinedSize()
    throws AdoError
  {
    return this.definedSize;
  }

  public int getPrecision()
    throws AdoError
  {
    if (this.precision <= 0)
      throw new AdoError("error getting column information for: " + getName(), 30007);
    return this.precision;
  }

  public int getScale()
  {
    return this.scale;
  }

  public int getType()
  {
    return this.type;
  }

  public int getSqlType()
  {
    return this.sqlType;
  }

  public boolean isNumericType()
  {
	boolean i = true;
    switch (this.sqlType)
    {
    case -6:
    case -5:
    case 2:
    case 4:
    case 5:
    case 6:
    case 7:
    case 8:
      i = false;
    case -4:
    case -3:
    case -2:
    case -1:
    case 0:
    case 1:
    case 3:
    }
    return i;
  }

  public String getSqlTypeName()
  {
    return this.sqlTypeName;
  }

  public synchronized Variant getValue()
    throws AdoError
  {
    return this.data;
  }

  public synchronized void setValue(Variant paramVariant)
    throws AdoError
  {
    if (isReadOnly())
    {
      Connection localConnection = this.ownerRS.getActiveConnection();
      if ((localConnection == null) || ((localConnection.getWorkArounds() & 0x4) != 4))
        throw new AdoError("Cannot update a read only column: " + getName(), 30012);
    }
    if (isAutoInc())
      throw new AdoError("Cannot update an auto increment column: " + getName(), 30013);
    saveOldValue();
    if (paramVariant == null)
      this.data = new Variant().setNull();
    else
      this.data = paramVariant;
    this.hasChanged = true;
  }

  public synchronized void setValue(Field paramField)
    throws AdoError
  {
    if (paramField == null)
      setValue((Variant)null);
    else
      setValue(paramField.getValue());
  }

  public boolean isSearchable()
  {
    return this.searchable;
  }

  public boolean isAutoInc()
  {
    return this.autoInc;
  }

  public boolean isReadOnly()
  {
    return this.readOnly;
  }

  public boolean isCurrency()
  {
    return this.currency;
  }

  public int getNullable()
  {
    return this.nullable;
  }

  public boolean isNull()
  {
	  boolean i = (this.data.getVarType() == 1) || (this.data.toObject() == null) ? true : false;
    return i;
  }

  public String getTableName()
  {
    return this.tableName;
  }

  public synchronized void appendChunk(Variant paramVariant)
    throws AdoError, IOException
  {
    if (!isValid())
      throwInvalidError();
    if (paramVariant == null)
    {
      this.data = new Variant();
      this.data.setNull();
      saveOldValue();
      this.hasChanged = true;
    }
    else
    {
      InputStream localInputStream = null;
      if (this.hasChanged)
      {
        localInputStream = AdoUtil.getStream(this.data);
        if (localInputStream == null)
          return;
      }
      else
      {
        saveOldValue();
        this.data = new Variant();
        this.data.setNull();
        this.hasChanged = true;
      }
      AdoUtil.appendToData(this.data, paramVariant, localInputStream);
    }
  }

  public synchronized Variant getChunk(int paramInt1, int paramInt2)
    throws AdoError, IOException
  {
    if (!isValid())
      throwInvalidError();
    Variant localVariant = new Variant();
    localVariant.setNull();
    InputStream localInputStream = AdoUtil.getStream(this.data);
    if (localInputStream != null)
      try
      {
        if (paramInt1 > 0)
          localInputStream.skip(paramInt1);
        int i = localInputStream.available();
        int j = paramInt2 < i ? paramInt2 : i;
        if (j > 0)
        {
          byte[] arrayOfByte = new byte[j];
          int k = localInputStream.read(arrayOfByte);
          localVariant.set(arrayOfByte);
        }
      }
      catch (IOException localIOException)
      {
        throw new AdoError(localIOException, "Error retrieving data.", 30003);
      }
    return localVariant;
  }

  public synchronized Variant getChunk(int paramInt)
    throws AdoError, IOException
  {
    return getChunk(0, paramInt);
  }

  synchronized void setInitialValue(Variant paramVariant)
  {
    if (paramVariant == null)
      this.data.setNull();
    else
      this.data = paramVariant;
    this.oldData = new Variant();
    this.oldData.setNull();
  }

  boolean isDirty()
  {
    return this.hasChanged;
  }

  synchronized void copyValue(Field paramField)
  {
    this.data = paramField.data;
  }

  synchronized void clearValue()
    throws AdoError
  {
    Variant localVariant = new Variant();
    localVariant.setNull();
    setValue(localVariant);
  }

  synchronized Variant getOldValue()
  {
    return this.oldData;
  }

  synchronized void clearDirtyFlag()
    throws AdoError
  {
    this.oldData = new Variant();
    this.oldData.setNull();
    this.hasChanged = false;
  }

  synchronized void restoreOldValue()
    throws AdoError
  {
    this.data = this.oldData;
    this.oldData = new Variant();
    this.oldData.setNull();
    this.hasChanged = false;
  }

  synchronized void saveOldValue()
    throws AdoError
  {
    if ((!this.hasChanged) || (this.oldData.isNull()))
      this.oldData = this.data;
  }

  void setTableName(String paramString)
  {
    this.tableName = paramString;
  }

  private boolean isValid()
  {
    boolean bool = false;
    if (this.ownerRS != null)
      bool = this.ownerRS.isCursorOnValidRow();
    return bool;
  }

  private void throwInvalidError()
    throws AdoError
  {
    throw new AdoError("The cursor must be positioned on a valid record to access the field value.", 30006);
  }

  private void initialize(ResultSetMetaData paramResultSetMetaData, int paramInt)
    throws AdoError
  {
    try
    {
      this.name = paramResultSetMetaData.getColumnName(paramInt);
      this.sqlTypeName = paramResultSetMetaData.getColumnTypeName(paramInt);
      this.sqlType = paramResultSetMetaData.getColumnType(paramInt);
      this.scale = paramResultSetMetaData.getScale(paramInt);
      try
      {
        this.searchable = paramResultSetMetaData.isSearchable(paramInt);
      }
      catch (SQLException localSQLException1)
      {
        if ((this.sqlType == -1) || (this.sqlType == -4))
          this.searchable = false;
        else
          this.searchable = true;
      }
      this.readOnly = paramResultSetMetaData.isReadOnly(paramInt);
      this.updatable = paramResultSetMetaData.isWritable(paramInt);
      this.nullable = paramResultSetMetaData.isNullable(paramInt);
      this.autoInc = paramResultSetMetaData.isAutoIncrement(paramInt);
      this.currency = paramResultSetMetaData.isCurrency(paramInt);
      this.definedSize = paramResultSetMetaData.getColumnDisplaySize(paramInt);
      this.precision = -1;
      try
      {
        this.precision = paramResultSetMetaData.getPrecision(paramInt);
      }
      catch (Exception localException)
      {
      }
      if (this.precision <= 0)
        switch (this.sqlType)
        {
        case 1:
        case 12:
          this.precision = this.definedSize;
          break;
        case 91:
        case 92:
        case 93:
          Connection localConnection = this.ownerRS.getActiveConnection();
          TypeInfo localTypeInfo = localConnection.getTypeInfo(this.sqlType);
          if (localTypeInfo == null)
            break;
          this.precision = localTypeInfo.precision;
        }
      this.attributes = 0;
      if (this.updatable)
        this.attributes |= 4;
      if (this.nullable == 1)
        this.attributes |= 32;
      if ((this.sqlType == -1) || (this.sqlType == -4))
        this.attributes |= 128;
      this.type = AdoUtil.getAdoType(this.sqlType);
      try
      {
        this.tableName = paramResultSetMetaData.getTableName(paramInt);
      }
      catch (SQLException localSQLException2)
      {
      }
    }
    catch (SQLException localSQLException3)
    {
      throw new AdoError(localSQLException3, "error getting column information for: " + getName(), 30007);
    }
  }

  private void initialize(Field paramField)
    throws AdoError
  {
    this.name = paramField.name;
    this.sqlTypeName = paramField.sqlTypeName;
    this.sqlType = paramField.sqlType;
    this.precision = paramField.precision;
    this.scale = paramField.scale;
    this.searchable = paramField.searchable;
    this.readOnly = paramField.readOnly;
    this.updatable = paramField.updatable;
    this.nullable = paramField.nullable;
    this.autoInc = paramField.autoInc;
    this.currency = paramField.currency;
    this.definedSize = paramField.definedSize;
    this.actualSize = paramField.actualSize;
    this.attributes = paramField.attributes;
    this.type = AdoUtil.getAdoType(this.sqlType);
    this.tableName = paramField.tableName;
  }

  private void throwValueIsNull()
    throws AdoError
  {
    throw new AdoError("Field value is null.", 30008);
  }

  public String toString()
  {
    return "Field[name=" + this.name + ",value=" + this.data + (this.hasChanged ? ",dirty" : "") + "]";
  }
}