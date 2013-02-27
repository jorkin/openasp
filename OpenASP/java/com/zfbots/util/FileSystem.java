package com.zfbots.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.Vector;

public class FileSystem
{
  private static Vector files;
  private static Integer allocatedFile = new Integer(0);

  public static int getFreeFile()
  {
    int i = files.size();
    for (int j = 0; j < i; j++)
    {
      if (files.elementAt(j) != null)
        continue;
      files.setElementAt(allocatedFile, j);
      return j;
    }
    files.addElement(allocatedFile);
    return files.size() - 1;
  }

  public static InputFile in(int paramInt)
  {
    return (InputFile)files.elementAt(paramInt);
  }

  public static OutputFile out(int paramInt)
  {
    return (OutputFile)files.elementAt(paramInt);
  }

  public static InputFile openInput(String paramString, int paramInt)
    throws IOException
  {
    InputFile localInputFile = new InputFile(paramString);
    if (paramInt >= files.size())
      files.setSize(paramInt + 1);
    files.setElementAt(localInputFile, paramInt);
    return localInputFile;
  }

  public static OutputFile openOutput(String paramString, int paramInt)
    throws IOException
  {
    OutputFile localOutputFile = new OutputFile(paramString);
    if (paramInt >= files.size())
      files.setSize(paramInt + 1);
    files.setElementAt(localOutputFile, paramInt);
    return localOutputFile;
  }

  public static AppendFile openAppend(String paramString, int paramInt)
    throws IOException
  {
    AppendFile localAppendFile = new AppendFile(paramString);
    if (paramInt >= files.size())
      files.setSize(paramInt + 1);
    files.setElementAt(localAppendFile, paramInt);
    return localAppendFile;
  }

  public static void close()
    throws IOException
  {
    int i = files.size();
    for (int j = 0; j < i; j++)
      close(j);
  }

  public static void close(int paramInt)
    throws IOException
  {
    Object localObject = files.elementAt(paramInt);
    if (localObject == null)
      throw new FileNotFoundException("File Number not found:" + paramInt);
    if ((localObject instanceof InputFile))
      ((InputFile)localObject).close();
    else if ((localObject instanceof OutputFile))
      ((OutputFile)localObject).close();
    files.setElementAt(null, paramInt);
  }

  public static boolean isEOF(int paramInt)
    throws IOException
  {
    Object localObject = files.elementAt(paramInt);
    if (localObject == null)
      throw new FileNotFoundException("File Number not found:" + paramInt);
    if ((localObject instanceof InputFile))
    {
      InputFile localInputFile = (InputFile)localObject;
      if (localInputFile.getFilePointer() >= localInputFile.length())
        return true;
    }
    return false;
  }

  public static Date getFileDateTime(String paramString)
    throws IOException
  {
    File localFile = new File(paramString);
    if (localFile.exists())
      return new Date(localFile.lastModified());
    throw new FileNotFoundException("File not found:" + paramString);
  }

  public static int getFileLen(String paramString)
    throws IOException
  {
    File localFile = new File(paramString);
    if (localFile.exists())
      return (int)localFile.length();
    throw new FileNotFoundException("File not found:" + paramString);
  }

  public static void kill(String paramString)
    throws IOException
  {
    File localFile = new File(paramString);
    if ((localFile.exists()) && (!localFile.isDirectory()))
      localFile.delete();
    else
      throw new FileNotFoundException("File not found:" + paramString);
  }

  public static int getFileLoc(int paramInt)
    throws IOException
  {
    Object localObject = files.elementAt(paramInt);
    if ((localObject instanceof InputFile))
      return (int)((InputFile)localObject).getFilePointer();
    throw new IOException("Loc not implemented for output files");
  }

  public static int getFileLen(int paramInt)
    throws IOException
  {
    Object localObject = files.elementAt(paramInt);
    if (localObject == null)
      throw new FileNotFoundException("File Number not found:" + paramInt);
    if ((localObject instanceof InputFile))
      return (int)((InputFile)localObject).length();
    if ((localObject instanceof OutputFile))
      return ((OutputFile)localObject).length();
    return 0;
  }

  public static File mkDir(String paramString)
    throws IOException
  {
    File localFile = new File(paramString);
    if (!localFile.mkdir())
      throw new IOException("Path/File access error:" + paramString);
    return localFile;
  }

  public static void rmDir(String paramString)
    throws IOException
  {
    File localFile = new File(paramString);
    if (localFile.isDirectory())
      localFile.delete();
    else
      throw new FileNotFoundException("Directory not found:" + paramString);
  }

  public static int seek(int paramInt)
    throws IOException
  {
    Object localObject = files.elementAt(paramInt);
    if ((localObject instanceof InputFile))
      return (int)((InputFile)localObject).getFilePointer() + 1;
    throw new FileNotFoundException("File Number not found:" + paramInt);
  }

  public static void seek(int paramInt1, int paramInt2)
    throws IOException
  {
    Object localObject = files.elementAt(paramInt1);
    if ((localObject instanceof InputFile))
    {
      ((InputFile)localObject).seek(paramInt2 - 1);
      return;
    }
    throw new FileNotFoundException("File Number not found:" + paramInt1);
  }

  public static String getDriveName(String paramString)
  {
    if (paramString != null)
    {
      int i = paramString.indexOf(":");
      if (i >= 0)
        return paramString.substring(0, i + 1);
    }
    return "";
  }

  public static String getBaseName(String paramString)
  {
    if (paramString != null)
    {
      String str = new File(paramString).getName();
      int i = str.indexOf(".");
      if (i >= 0)
        return str.substring(0, i);
    }
    return "";
  }

  public static String getExtensionName(String paramString)
  {
    if (paramString != null)
    {
      String str = new File(paramString).getName();
      int i = str.indexOf(".");
      if (i >= 0)
        return str.substring(i + 1, str.length());
    }
    return "";
  }

  public static int getAttr(File paramFile)
    throws IOException
  {
    if (paramFile == null)
      throw new FileNotFoundException("File not found");
    if (!paramFile.exists())
      throw new FileNotFoundException("File not found:" + paramFile.getPath());
    int i = 0;
    if (paramFile.isDirectory())
      i += 16;
    if (paramFile.isHidden())
      i += 2;
    if (!paramFile.canWrite())
      i++;
    return i;
  }

  public static int getAttr(String paramString)
    throws IOException
  {
    return getAttr(new File(paramString));
  }

  public static String readString(RandomAccessFile paramRandomAccessFile, int paramInt)
    throws IOException
  {
    byte[] arrayOfByte = new byte[paramInt];
    paramRandomAccessFile.read(arrayOfByte, 0, paramInt);
    return new String(arrayOfByte);
  }

  public static RandomAccessFile openFile(String paramString1, String paramString2)
    throws IOException
  {
    return new RandomAccessFile(paramString1, paramString2);
  }

  public static RandomAccessFile openFile(String paramString, int paramInt)
    throws IOException
  {
    String str;
    if (paramInt == 1)
      str = "r";
    else
      str = "rw";
    RandomAccessFile localRandomAccessFile = openFile(paramString, str);
    if (paramInt == 8)
      localRandomAccessFile.seek(localRandomAccessFile.length());
    return localRandomAccessFile;
  }

  static
  {
    files = new Vector();
    files.addElement(allocatedFile);
  }
}