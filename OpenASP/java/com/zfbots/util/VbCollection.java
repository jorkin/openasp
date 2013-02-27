package com.zfbots.util;

import java.util.Hashtable;
import java.util.Vector;

public class VbCollection
{
  private Vector keys = new Vector();
  private Hashtable values = new Hashtable();
  private int tmpKey = 1;
  public static final int BEFORE = 0;
  public static final int AFTER = 1;

  public Variant getItem(int paramInt)
  {
    if (paramInt < 1)
      throw new ArrayIndexOutOfBoundsException(paramInt);
    Object localObject1 = this.keys.elementAt(paramInt - 1);
    Object localObject2 = this.values.get(localObject1);
    if ((localObject2 == null) && (!this.values.containsKey(localObject1)))
      throw new ArrayIndexOutOfBoundsException(paramInt);
    return new Variant(localObject2);
  }

  public Variant getItem(String paramString)
  {
    String str = paramString.toLowerCase();
    Object localObject = this.values.get(str);
    if ((localObject == null) && (!this.values.containsKey(str)))
      throw new IndexOutOfBoundsException("key doesn't exist: " + paramString);
    return new Variant(localObject);
  }

  public Variant getItem(Variant paramVariant)
  {
    if ((paramVariant.getVarType() != 15) && (paramVariant.isNumeric()))
      return getItem(paramVariant.toInt());
    return getItem(paramVariant.toString());
  }

  public int getCount()
  {
    return this.values.size();
  }

  private Object getValue(Object paramObject)
  {
    if ((paramObject instanceof Variant))
    {
      if (((Variant)paramObject).toObject() == null)
        paramObject = new Variant(paramObject);
      else
        paramObject = ((Variant)paramObject).toObject();
    }
    else if (paramObject == null)
      paramObject = new Variant((Object)null);
    return paramObject;
  }

  public void add(Object paramObject)
    throws RuntimeException
  {
    add(paramObject, String.valueOf(this.tmpKey++));
  }

  public void add(Object paramObject, String paramString)
    throws RuntimeException
  {
    synchronized (this.keys)
    {
      paramString = paramString.toLowerCase();
      if (this.values.containsKey(paramString))
        throw new RuntimeException("The key: " + paramString + " is already associated with an element in this collection.");
      this.keys.addElement(paramString);
      this.values.put(paramString, getValue(paramObject));
    }
  }

  public void add(Object paramObject, String paramString1, String paramString2, int paramInt)
    throws IndexOutOfBoundsException
  {
    add(paramObject, paramString1, getIndex(paramString2), paramInt);
  }

  public void add(Object paramObject, String paramString, int paramInt1, int paramInt2)
    throws IndexOutOfBoundsException, RuntimeException
  {
    if (paramInt1 < 1)
      throw new IndexOutOfBoundsException("Collection: Index " + paramInt1 + " not valid.");
    synchronized (this.keys)
    {
      if (paramString == null)
        paramString = String.valueOf(this.tmpKey++);
      if (paramInt2 == 1)
        paramInt1++;
      paramString = paramString.toLowerCase();
      if (this.values.containsKey(paramString))
        throw new RuntimeException("The key '" + paramString + "' is already associated with an element in this collection.");
      this.keys.insertElementAt(paramString, paramInt1 - 1);
      this.values.put(paramString, getValue(paramObject));
    }
  }

  public void remove(int paramInt)
  {
    synchronized (this.keys)
    {
      Object localObject1 = this.keys.elementAt(paramInt - 1);
      this.keys.removeElementAt(paramInt - 1);
      this.values.remove(localObject1);
    }
  }

  public void remove(String paramString)
  {
    synchronized (this.keys)
    {
      paramString = paramString.toLowerCase();
      this.keys.removeElement(paramString);
      this.values.remove(paramString);
    }
  }

  public void remove(Variant paramVariant)
  {
    if ((paramVariant.getVarType() != 15) && (paramVariant.isNumeric()))
      remove(paramVariant.toInt());
    else
      remove(paramVariant.toString());
  }

  public void clear()
  {
    synchronized (this.keys)
    {
      this.keys.removeAllElements();
      this.values.clear();
    }
  }

  public int getIndex(String paramString)
  {
    paramString = paramString.toLowerCase();
    int i = this.keys.size();
    for (int j = 0; j < i; j++)
      if (paramString.equals(this.keys.elementAt(j)))
        return j + 1;
    return -1;
  }

  public String getKey(int paramInt)
  {
    Object localObject = this.keys.elementAt(paramInt - 1);
    return (localObject instanceof String) ? (String)localObject : "";
  }

  public void setKey(int paramInt, String paramString)
  {
    synchronized (this.keys)
    {
      if ((paramString == null) || (paramString.equals("")))
        return;
      Object localObject1 = this.values.remove(getKey(paramInt));
      paramString = paramString.toLowerCase();
      this.values.put(paramString, localObject1);
      this.keys.setElementAt(paramString, paramInt - 1);
    }
  }

  public void changeKey(String paramString1, String paramString2)
  {
    int i = getIndex(paramString1);
    setKey(i, paramString2);
  }

  public String getNextKey()
  {
    return String.valueOf(this.tmpKey++);
  }

  public boolean containsKey(Object paramObject)
  {
    return this.values.containsKey(paramObject);
  }

  public boolean containsValue(Object paramObject)
  {
    return this.values.containsValue(paramObject);
  }
}