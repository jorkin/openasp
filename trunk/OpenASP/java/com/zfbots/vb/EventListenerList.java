package com.zfbots.vb;

import java.util.EventListener;

public class EventListenerList
{
  private static final Object[] NULL_ARRAY = new Object[0];
  protected transient Object[] listenerList = NULL_ARRAY;

  public Object[] getListenerList()
  {
    return this.listenerList;
  }

  public int getListenerCount()
  {
    return this.listenerList.length / 2;
  }

  public int getListenerCount(Class paramClass)
  {
    int i = 0;
    Object[] arrayOfObject = this.listenerList;
    for (int j = 0; j < arrayOfObject.length; j += 2)
    {
      if (paramClass != (Class)arrayOfObject[j])
        continue;
      i++;
    }
    return i;
  }

  public synchronized void add(Class paramClass, EventListener paramEventListener)
  {
    if (paramEventListener == null)
      return;
    if (!paramClass.isInstance(paramEventListener))
      throw new IllegalArgumentException("Listener " + paramEventListener + " is not of type " + paramClass);
    if (this.listenerList == NULL_ARRAY)
    {
      this.listenerList = new Object[] { paramClass, paramEventListener };
    }
    else
    {
      int i = this.listenerList.length;
      Object[] arrayOfObject = new Object[i + 2];
      System.arraycopy(this.listenerList, 0, arrayOfObject, 0, i);
      arrayOfObject[i] = paramClass;
      arrayOfObject[(i + 1)] = paramEventListener;
      this.listenerList = arrayOfObject;
    }
  }

  public synchronized void remove(Class paramClass, EventListener paramEventListener)
  {
    if (paramEventListener == null)
      return;
    if (!paramClass.isInstance(paramEventListener))
      throw new IllegalArgumentException("Listener " + paramEventListener + " is not of type " + paramClass);
    int i = -1;
    for (int j = this.listenerList.length - 2; j >= 0; j -= 2)
    {
      if ((this.listenerList[j] != paramClass) || (this.listenerList[(j + 1)].equals(paramEventListener) != true))
        continue;
      i = j;
      break;
    }
    if (i != -1)
    {
      Object[] arrayOfObject = new Object[this.listenerList.length - 2];
      System.arraycopy(this.listenerList, 0, arrayOfObject, 0, i);
      if (i < arrayOfObject.length)
        System.arraycopy(this.listenerList, i + 2, arrayOfObject, i, arrayOfObject.length - i);
      this.listenerList = (arrayOfObject.length == 0 ? NULL_ARRAY : arrayOfObject);
    }
  }

  public String toString()
  {
    Object[] arrayOfObject = this.listenerList;
    String str = "EventListenerList: ";
    str = str + arrayOfObject.length / 2 + " listeners: ";
    for (int i = 0; i <= arrayOfObject.length - 2; i += 2)
    {
      str = str + " type " + ((Class)arrayOfObject[i]).getName();
      str = str + " listener " + arrayOfObject[(i + 1)];
    }
    return str;
  }
}