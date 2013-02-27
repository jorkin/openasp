package com.zfbots.vb;

import java.util.EventListener;

import com.zfbots.util.Variant;


public abstract interface DataBound extends EventListener
{
  public abstract void dataChanged(DataChangeEvent paramDataChangeEvent);

  public abstract Variant getDataValue();

  public abstract String getDataField();

  public abstract void setDataField(String paramString);

  public abstract DataSource getDataSource();

  public abstract void setDataSource(DataSource paramDataSource);
}