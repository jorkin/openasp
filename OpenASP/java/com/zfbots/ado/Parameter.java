package com.zfbots.ado;

import java.io.InputStream;
import java.util.Properties;

import com.zfbots.util.Variant;


public class Parameter
{
  private boolean changed;
  private Variant data;
  private String name;
  private int precision;
  private int scale;
  private int size;
  private int type;
  private int sqlType;
  private int direction;
  private Properties props;

  public Parameter()
  {
    this.name = "";
    this.data = new Variant();
    this.data.setNull();
    this.direction = 1;
    this.type = 4;
    this.sqlType = 4;
    this.props = new Properties();
  }

  public Parameter(String paramString, int paramInt1, int paramInt2, int paramInt3, Variant paramVariant)
  {
    this.name = paramString;
    this.data = paramVariant;
    if (paramVariant == null)
    {
      this.data = new Variant();
      this.data.setNull();
    }
    this.direction = paramInt2;
    this.size = paramInt3;
    this.type = paramInt1;
    try
    {
      this.sqlType = AdoUtil.getSqlType(paramInt1);
    }
    catch (Exception localException)
    {
    }
    this.props = new Properties();
  }

  public int getDirection()
  {
    return this.direction;
  }

  public void setDirection(int paramInt)
  {
    this.direction = paramInt;
  }

  public String getName()
  {
    return this.name;
  }

  public void setName(String paramString)
  {
    this.name = paramString;
  }

  public int getPrecision()
  {
    return this.precision;
  }

  public void setPrecision(int paramInt)
    throws AdoError
  {
    if (paramInt < 0)
      throw new AdoError("The precision for the parameter should be >= 0", 40001);
    this.precision = paramInt;
  }

  public Properties getProperties()
  {
    return this.props;
  }

  public int getScale()
  {
    return this.scale;
  }

  public void setScale(int paramInt)
    throws AdoError
  {
    if (paramInt < 0)
      throw new AdoError("The scale for the parameter should be >= 0", 40002);
    this.scale = paramInt;
  }

  public int getSize()
  {
    return this.size;
  }

  public void setSize(int paramInt)
    throws AdoError
  {
    if (paramInt < 0)
      throw new AdoError("The size for the parameter should be >= 0", 40003);
    this.size = paramInt;
  }

  public int getType()
  {
    return this.type;
  }

  public void setType(int paramInt)
    throws AdoError
  {
    this.type = paramInt;
    this.sqlType = AdoUtil.getSqlType(paramInt);
  }

  public int getSqlType()
  {
    return this.sqlType;
  }

  public void setSqlType(int paramInt)
    throws AdoError
  {
    this.sqlType = paramInt;
    this.type = AdoUtil.getAdoType(paramInt);
  }

  public Variant getValue()
  {
    return this.data;
  }

  public void setValue(Variant paramVariant)
  {
    this.data = paramVariant;
    this.changed = true;
  }

  public void appendChunk(Variant paramVariant)
    throws AdoError
  {
    if (paramVariant == null)
    {
      this.data.setNull();
      this.changed = true;
    }
    else
    {
      InputStream localInputStream = null;
      if (this.changed)
      {
        localInputStream = (InputStream)this.data.toObject();
        if (localInputStream == null)
          throw new AdoError("Error retrieving old data from variant.", 40004);
      }
      else
      {
        this.data.setNull();
        this.changed = true;
      }
      AdoUtil.appendToData(this.data, paramVariant, localInputStream);
    }
  }

  public String toString()
  {
    return "Parameter[name=" + this.name + ",data=" + this.data + ",direction=" + this.direction + ",type=" + this.type + "]";
  }
}