package com.zfbots.vb;

import com.zfbots.ado.Recordset;
import com.zfbots.util.Variant;


public class DataChangeEvent
{
  private Recordset rs;
  private Variant value;
  private int changeType;

  public DataChangeEvent(Recordset paramRecordset, int paramInt)
  {
    this.rs = paramRecordset;
    this.changeType = paramInt;
  }

  public Variant getValue()
  {
    return this.value;
  }

  public void setValue(Variant paramVariant)
  {
    this.value = paramVariant;
  }

  public Recordset getRecordset()
  {
    return this.rs;
  }

  public Recordset getSource()
  {
    return this.rs;
  }

  public int getChangeType()
  {
    return this.changeType;
  }

  public String toString()
  {
    return "DataChangeEvent[val:" + this.value + " type: " + this.changeType + " " + this.rs + "]";
  }
}