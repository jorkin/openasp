package com.zfbots.ado;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import com.zfbots.util.Variant;


class AdoCursor
  implements Runnable
{
  private Recordset ownerRecset;
  private Fields fields;
  private Vector records;
  private ResultSet rs;
  private int maxRecords;
  private boolean allRecordsFetched;
  boolean abortQuery;
  private int recToFetch;
  private int numToFetch;

  AdoCursor(Recordset paramRecordset, Fields paramFields)
  {
    this.ownerRecset = paramRecordset;
    this.fields = paramFields;
    this.records = new Vector(50);
    this.recToFetch = 2147483647;
    this.numToFetch = 2;
    this.allRecordsFetched = false;
    setAbortQuery(false);
  }

  Variant getValueAt(int paramInt1, int paramInt2, boolean paramBoolean)
    throws AdoError
  {
    int i = this.records.size();
    if (paramInt1 >= i)
      return new Variant().setNull();
    if (paramBoolean)
    {
      paramInt1++;
      int j = 0;
      for (int k = 0; k < i; k++)
      {
        Record localRecord = getRow(k);
        if (localRecord.isFilteredOut())
          continue;
        j++;
        if (j == paramInt1)
          return localRecord.get(paramInt2);
      }
      return new Variant();
    }
    return getRow(paramInt1).get(paramInt2);
  }

  void setFields(Fields paramFields)
  {
    this.fields = paramFields;
  }

  void setResultSet(ResultSet paramResultSet)
  {
    this.rs = paramResultSet;
  }

  void setMaxRecords(int paramInt)
  {
    this.maxRecords = paramInt;
  }

  void setAllRecsFetched(boolean paramBoolean)
  {
    println("Cursor.setAllRecsFetched " + paramBoolean);
    this.allRecordsFetched = paramBoolean;
  }

  boolean allRecsFetched()
  {
    return this.allRecordsFetched;
  }

  int getRecordsFetched()
  {
    int i = 0;
    synchronized (this.records)
    {
      i = this.records.size();
    }
    return i;
  }

  boolean moveCursor()
    throws AdoError
  {
	  boolean i = true;
    try
    {
      if (((this.maxRecords > 0) && (getRecordsFetched() == this.maxRecords)) || (this.rs == null))
        i = true;
      else
        i = !this.rs.next() ? false : true;
    }
    catch (SQLException localSQLException)
    {
      i = false;
      throw new AdoError(localSQLException, "error moving to next record.", 69001);
    }
    return i;
  }

  void addEmptyRow(Fields paramFields)
    throws AdoError
  {
    Record localRecord = createRecord(paramFields);
    addRecord(localRecord);
  }

  int addToCache(Fields paramFields)
    throws AdoError
  {
    Fields localFields = this.fields;
    if (paramFields != null)
      localFields = paramFields;
    Record localRecord = createRecord(localFields);
    updateCache(localRecord, paramFields);
    addRecord(localRecord);
    return this.records.size();
  }

  void updateCache(int paramInt, Fields paramFields)
    throws AdoError
  {
    Record localRecord = getRow(paramInt);
    updateCache(localRecord, paramFields);
  }

  void deleteRecord(int paramInt)
    throws AdoError
  {
    Record localRecord = getRow(paramInt);
    localRecord.setDeleted(true);
    localRecord.setFilteredOut(true);
  }

  void removeFromCache(int paramInt)
    throws AdoError
  {
    this.records.removeElementAt(paramInt);
  }

  boolean isDeletedRecord(int paramInt)
  {
    try
    {
      Record localRecord = getRow(paramInt);
      return localRecord.isDeleted();
    }
    catch (Exception localException)
    {
    }
    return false;
  }

  boolean isFilteredOut(int paramInt)
    throws AdoError
  {
    return getRow(paramInt).isFilteredOut();
  }

  void setFilteredOut(int paramInt, boolean paramBoolean)
    throws AdoError
  {
    getRow(paramInt).setFilteredOut(paramBoolean);
  }

  void setAllFilteredOut(boolean paramBoolean)
    throws AdoError
  {
    int i = getRecordsFetched();
    for (int j = 0; j < i; j++)
      getRow(j).setFilteredOut(paramBoolean);
  }

  private void updateCache(Record paramRecord, Fields paramFields)
    throws AdoError
  {
    Fields localFields = this.fields;
    if (paramFields != null)
      localFields = paramFields;
    int i = localFields.getCount();
    for (int j = 0; j < i; j++)
    {
      Field localField = localFields.getField(j);
      int k = localField.getSqlType();
      Variant localVariant = paramRecord.get(j);
      if (paramFields != null)
        AdoUtil.getColValue(k, localField, localVariant);
      else
        AdoUtil.getColValue(this.rs, k, j + 1, localVariant);
    }
  }

  void updateCacheForField(int paramInt1, Field paramField, int paramInt2)
    throws AdoError
  {
    Record localRecord = getRow(paramInt1);
    Variant localVariant = localRecord.get(paramInt2);
    localVariant.set(paramField.getValue());
  }

  private void addRecord(Record paramRecord)
  {
    synchronized (this.records)
    {
      this.records.add(paramRecord);
    }
  }

  private Record getRow(int paramInt)
    throws AdoError
  {
    Record localRecord = null;
    synchronized (this.records)
    {
      localRecord = (Record)this.records.elementAt(paramInt);
    }
    if (localRecord == null)
      throw new AdoError(null, "Internal error fetching row. row not found, rowNum: " + paramInt, 69002);
    return localRecord;
  }

  void reset()
  {
    this.records.removeAllElements();
    this.recToFetch = 2147483647;
    this.allRecordsFetched = false;
  }

  private final void println(String paramString)
  {
    if (this.ownerRecset != null)
      this.ownerRecset.println(paramString);
  }

  void fillRecordBuffer(int paramInt, boolean paramBoolean)
    throws AdoError
  {
    int i = this.fields.getCount();
    Record localRecord = null;
    Field localField;
    if ((paramBoolean) && (paramInt >= this.records.size()))
    {
      for (int j = 0; j < i; j++)
      {
        localField = this.fields.getField(j);
        localField.setInitialValue(new Variant().setNull());
      }
      return;
    }
    if (paramBoolean)
      localRecord = getRow(paramInt);
    for (int j = 0; j < i; j++)
    {
      localField = this.fields.getField(j);
      int k = localField.getSqlType();
      Variant localVariant;
      if (paramBoolean)
      {
        localVariant = localRecord.get(j);
        localField.setInitialValue(localVariant);
      }
      else
      {
        localVariant = new Variant();
        localVariant.setNull();
        AdoUtil.getColValue(this.rs, k, j + 1, localVariant);
        localField.setInitialValue(localVariant);
      }
    }
  }

  private Record createRecord(Fields paramFields)
    throws AdoError
  {
    int i = paramFields.getCount();
    Record localRecord = new Record(i);
    for (int j = 0; j < i; j++)
    {
      Field localField = paramFields.getField(j);
      Variant localVariant = new Variant();
      localVariant.setNull();
      localRecord.add(localVariant);
    }
    return localRecord;
  }

  synchronized boolean getRecord(int paramInt)
  {
	boolean i = true;
    if (paramInt > 0)
      if (paramInt > getRecordsFetched())
      {
        this.recToFetch = paramInt;
        while ((this.recToFetch == paramInt) && (!this.allRecordsFetched))
          try
          {
            if (this.ownerRecset.isDisconnected)
              break;
            println("waiting for record: " + this.recToFetch);
            wait();
          }
          catch (InterruptedException localInterruptedException)
          {
          }
        i = paramInt <= getRecordsFetched() ? false : true;
      }
      else
      {
        i = false;
      }
    return i;
  }

  private synchronized void getData()
    throws AdoError
  {
    try
    {
      for (int i = 0; (i < this.numToFetch) && (!this.allRecordsFetched) && (!this.abortQuery); i++)
        if (!moveCursor())
        {
          addToCache(null);
          if ((this.recToFetch == 2147483647) || (this.recToFetch != getRecordsFetched()))
            continue;
          println("notify that we fetched record: " + this.recToFetch);
          this.recToFetch = 2147483647;
          notify();
        }
        else
        {
          this.allRecordsFetched = true;
          println("notify that all recs have been fetched.");
          this.recToFetch = 2147483647;
          notify();
          this.rs = null;
          this.ownerRecset.closeCursor();
          this.ownerRecset.setRecordCount(getRecordsFetched());
        }
    }
    catch (Exception localException)
    {
      println("caught exception in getData; setting AllRecordsFetched to true.");
      println(localException.toString());
      this.allRecordsFetched = true;
      notify();
      throw new AdoError(localException, "Caught Exception getting data.", 69003);
    }
  }

  public void run()
  {
    try
    {
      while ((!this.allRecordsFetched) && (!this.abortQuery))
        getData();
      setAbortQuery(false);
    }
    catch (AdoError localAdoError)
    {
      this.ownerRecset.setBackgroundFetchError(localAdoError);
    }
  }

  void setAbortQuery(boolean paramBoolean)
  {
    this.abortQuery = paramBoolean;
  }

  public String toString()
  {
    String str = "AdoCursor[";
    str = str + "maxRecords=" + this.maxRecords + ",allRecordsFetched=" + this.allRecordsFetched + ",recToFetch=" + this.recToFetch + ",numToFetch=" + this.numToFetch + "]";
    return str;
  }

  private static class Record
  {
    private byte flags = 0;
    private static byte IS_FILTERED_OUT = 1;
    private static byte IS_DELETED = 2;
    Vector values = null;

    Record(int paramInt)
    {
      this.values = new Vector(paramInt);
    }

    boolean isDeleted()
    {
      return (this.flags & IS_DELETED) > 0;
    }

    void setDeleted(boolean paramBoolean)
    {
      this.flags = (byte)(this.flags & (IS_DELETED ^ 0xFFFFFFFF));
      if (paramBoolean)
        this.flags = (byte)(this.flags | IS_DELETED);
    }

    boolean isFilteredOut()
    {
      return (this.flags & IS_FILTERED_OUT) > 0;
    }

    void setFilteredOut(boolean paramBoolean)
    {
      this.flags = (byte)(this.flags & (IS_FILTERED_OUT ^ 0xFFFFFFFF));
      if (paramBoolean)
        this.flags = (byte)(this.flags | IS_FILTERED_OUT);
    }

    Variant get(int paramInt)
    {
      return (Variant)this.values.elementAt(paramInt);
    }

    void add(Variant paramVariant)
    {
      this.values.addElement(paramVariant);
    }
  }
}
