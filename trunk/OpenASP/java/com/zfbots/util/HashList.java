package com.zfbots.util;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

public class HashList extends AbstractList
  implements List
{
  private Vector keys = new Vector();
  private Hashtable values = new Hashtable();
  private int tmpKey = 1;
  public static final int BEFORE = 0;
  public static final int AFTER = 1;

  public int size()
  {
    return this.values.size();
  }

  public Object get(int paramInt)
  {
    if (paramInt < 0)
      return null;
    Object localObject = this.keys.elementAt(paramInt);
    return this.values.get(localObject);
  }

  public Object get(Object paramObject)
  {
    if (paramObject == null)
      return null;
    return this.values.get(paramObject);
  }

  public Object set(int paramInt, Object paramObject)
  {
    Object localObject = this.keys.elementAt(paramInt);
    return this.values.put(localObject, paramObject);
  }

  public Object set(Object paramObject1, Object paramObject2)
  {
    return put(paramObject1, paramObject2);
  }

  public boolean add(Object paramObject)
  {
    add(paramObject, null);
    return true;
  }

  public void add(int paramInt, Object paramObject)
  {
    add(paramObject, null, paramInt, 0);
  }

  public void add(Object paramObject1, Object paramObject2)
  {
    if (paramObject2 == null)
      paramObject2 = String.valueOf(this.tmpKey++);
    this.keys.addElement(paramObject2);
    this.values.put(paramObject2, paramObject1);
  }

  public void add(Object paramObject1, Object paramObject2, Object paramObject3, int paramInt)
  {
    add(paramObject1, paramObject2, getIndex(paramObject3), paramInt);
  }

  public void add(Object paramObject1, Object paramObject2, int paramInt)
  {
    add(paramObject1, paramObject2, paramInt, 0);
  }

  public void add(Object paramObject1, Object paramObject2, int paramInt1, int paramInt2)
  {
    if (paramInt1 < 0)
      throw new IndexOutOfBoundsException("Collection: Index " + paramInt1 + " not valid.");
    if (paramObject2 == null)
      paramObject2 = String.valueOf(this.tmpKey++);
    if (paramInt2 == 1)
      paramInt1++;
    this.keys.insertElementAt(paramObject2, paramInt1);
    this.values.put(paramObject2, paramObject1);
  }

  public Object remove(int paramInt)
  {
    Object localObject = this.keys.elementAt(paramInt);
    this.keys.removeElementAt(paramInt);
    return this.values.remove(localObject);
  }

  public Object removeKey(Object paramObject)
  {
    this.keys.removeElement(paramObject);
    return this.values.remove(paramObject);
  }

  public boolean remove(Object paramObject)
  {
    int i = indexOf(paramObject);
    if (i >= 0)
    {
      Object localObject = getKey(i);
      this.keys.removeElement(localObject);
      this.values.remove(localObject);
      return true;
    }
    return false;
  }

  public void clear()
  {
    this.keys.removeAllElements();
    this.values.clear();
  }

  public int getIndex(Object paramObject)
  {
    int i = this.keys.size();
    for (int j = 0; j < i; j++)
      if (paramObject.equals(this.keys.elementAt(j)))
        return j;
    return -1;
  }

  public Object getKey(int paramInt)
  {
    return this.keys.elementAt(paramInt);
  }

  public void setKey(int paramInt, Object paramObject)
  {
    if ((paramObject == null) || (paramObject.equals("")))
      return;
    Object localObject = this.values.remove(getKey(paramInt));
    this.values.put(paramObject, localObject);
    this.keys.setElementAt(paramObject, paramInt);
  }

  public void changeKey(Object paramObject1, Object paramObject2)
  {
    int i = getIndex(paramObject1);
    setKey(i, paramObject2);
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

  public Object put(Object paramObject1, Object paramObject2)
  {
    if (!this.values.containsKey(paramObject1))
      this.keys.addElement(paramObject1);
    return this.values.put(paramObject1, paramObject2);
  }

  public void putAll(Map paramMap)
  {
    Iterator localIterator = paramMap.entrySet().iterator();
    while (localIterator.hasNext())
    {
      Map.Entry localEntry = (Map.Entry)localIterator.next();
      put(localEntry.getKey(), localEntry.getValue());
    }
  }

  public Set keySet()
  {
    return this.values.keySet();
  }

  public Collection values()
  {
    return this.values.values();
  }

  public Set entrySet()
  {
    return this.values.entrySet();
  }
}