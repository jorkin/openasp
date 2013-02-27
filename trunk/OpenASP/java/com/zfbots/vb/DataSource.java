package com.zfbots.vb;

import com.zfbots.ado.Recordset;

public abstract interface DataSource
{
  public abstract void addDataBound(DataBound paramDataBound);

  public abstract void removeDataBound(DataBound paramDataBound);

  public abstract Recordset getRecordset();
}