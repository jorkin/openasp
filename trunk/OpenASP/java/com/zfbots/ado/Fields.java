package com.zfbots.ado;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

import com.zfbots.util.Variant;


public class Fields
{
  Vector fields = null;

  public Fields()
  {
    this(5);
  }

  public Fields(int paramInt)
  {
    this.fields = new Vector(paramInt);
  }

  public int getCount()
  {
    return this.fields.size();
  }

  void add(Field paramField)
  {
    this.fields.addElement(paramField);
  }

  public Field getField(Variant paramVariant)
    throws AdoError
  {
    Field localField = null;
    if (paramVariant == null)
      return null;
    if (paramVariant.getVarType() == 15)
      localField = getField(paramVariant.toString());
    else if (paramVariant.isNumeric())
      localField = getField(paramVariant.toInt());
    else
      throw new AdoError("Unknown/Unhandled object type for this operation: ", 90002);
    return localField;
  }

  public Field getField(Object paramObject)
    throws AdoError
  {
    Field localField = null;
    if ((paramObject instanceof String))
    {
      localField = getField((String)paramObject);
    }
    else if ((paramObject instanceof Variant))
    {
      localField = getField((Variant)paramObject);
    }
    else if ((paramObject instanceof Integer))
    {
      Integer localInteger = (Integer)paramObject;
      int i = localInteger.intValue();
      localField = getField(i);
    }
    else
    {
      throw new AdoError("Unknown/Unhandled object type for this operation: ", 90002);
    }
    return localField;
  }

  public Field getField(int paramInt)
    throws AdoError
  {
    if ((paramInt < 0) || (paramInt >= this.fields.size()))
      throw new AdoError("Field array index out of bounds, array size: " + this.fields.size(), 30010);
    return (Field)this.fields.elementAt(paramInt);
  }

  public Field getField(String paramString)
    throws AdoError
  {
    return getField(paramString, null);
  }

  public Field getField(String paramString1, String paramString2)
    throws AdoError
  {
    int i = getFieldIndex(paramString1, paramString2);
    return getField(i);
  }

  public int getFieldIndex(String paramString1, String paramString2)
    throws AdoError
  {
    int i = this.fields.size();
    for (int j = 0; j < i; j++)
    {
      Field localField = (Field)this.fields.elementAt(j);
      if ((paramString1.equalsIgnoreCase(localField.getName())) && ((paramString2 == null) || (paramString2.equalsIgnoreCase(localField.getTableName()))))
        return j;
    }
    throw new AdoError("Field not found: " + paramString1, 30011);
  }

  public void printFields(PrintStream paramPrintStream)
    throws AdoError, IOException
  {
    if (paramPrintStream != null)
      for (int i = 0; i < getCount(); i++)
      {
        Field localField = (Field)this.fields.elementAt(i);
        int j = localField.getSqlType();
        switch (j)
        {
        case 4:
          paramPrintStream.println(localField.getValue().toInt());
          break;
        case -4:
        case -1:
          InputStream localInputStream = (InputStream)localField.getValue().toObject();
          byte[] arrayOfByte = new byte[1024];
          int k = 0;
          while (true)
          {
            k = localInputStream.read(arrayOfByte);
            if (k <= 0)
              break;
            if (j == -1)
            {
              printcharbuf(paramPrintStream, arrayOfByte);
              continue;
            }
            printbuf(paramPrintStream, arrayOfByte);
          }
          paramPrintStream.println("");
          break;
        default:
          paramPrintStream.println(localField.getValue().toString());
        }
      }
  }

  void printbuf(PrintStream paramPrintStream, byte[] paramArrayOfByte)
  {
    int i = paramArrayOfByte.length;
    for (int j = 0; j < i; j++)
      paramPrintStream.print(paramArrayOfByte[j]);
  }

  void printcharbuf(PrintStream paramPrintStream, byte[] paramArrayOfByte)
  {
    int i = paramArrayOfByte.length;
    for (int j = 0; j < i; j++)
      paramPrintStream.print((char)paramArrayOfByte[j]);
  }

  void copyValues(Fields paramFields)
    throws AdoError
  {
    for (int i = 0; i < getCount(); i++)
    {
      Field localField = (Field)this.fields.elementAt(i);
      localField.copyValue(paramFields.getField(i));
    }
  }
}