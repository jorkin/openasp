package com.zfbots.util;

import java.io.File;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class XmlDom
{
  private Document doc;
  private String lastLoadedURL;
  private boolean validating = false;
  private boolean preservingWhiteSpace = false;
  private ParseError parseError = new ParseError();

  public Document getDocument()
    throws Exception
  {
    if (this.doc == null)
    {
      DocumentBuilderFactory localDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
      localDocumentBuilderFactory.setValidating(false);
      DocumentBuilder localDocumentBuilder = localDocumentBuilderFactory.newDocumentBuilder();
      this.doc = localDocumentBuilder.newDocument();
    }
    return this.doc;
  }

  public String getUrl()
  {
    return this.lastLoadedURL;
  }

  public boolean isPreservingWhiteSpace()
  {
    return this.preservingWhiteSpace;
  }

  public void setPreservingWhiteSpace(boolean paramBoolean)
  {
    this.preservingWhiteSpace = paramBoolean;
  }

  public boolean isValidating()
  {
    return this.validating;
  }

  public void setValidating(boolean paramBoolean)
  {
    this.validating = paramBoolean;
  }

  public boolean load(String paramString)
    throws Exception
  {
    if (paramString == null)
      throw new Exception("fileName == null");
    this.lastLoadedURL = paramString;
    return load(new File(paramString));
  }

  public boolean load(File paramFile)
  {
    try
    {
      DocumentBuilderFactory localDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
      localDocumentBuilderFactory.setValidating(this.validating);
      localDocumentBuilderFactory.setIgnoringElementContentWhitespace(true);
      DocumentBuilder localDocumentBuilder = localDocumentBuilderFactory.newDocumentBuilder();
      this.doc = localDocumentBuilder.parse(paramFile);
      if (!this.preservingWhiteSpace)
        removeWhiteSpaceNodes(this.doc);
    }
    catch (SAXParseException localSAXParseException)
    {
      this.parseError.setException(localSAXParseException);
      return false;
    }
    catch (Exception localException)
    {
      this.parseError.setException(localException);
      return false;
    }
    this.parseError.clear();
    return true;
  }

  public boolean loadXML(String paramString)
  {
    try
    {
      if (paramString == null)
        throw new Exception("no data to parse");
      this.lastLoadedURL = null;
      DocumentBuilderFactory localDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
      localDocumentBuilderFactory.setValidating(false);
      DocumentBuilder localDocumentBuilder = localDocumentBuilderFactory.newDocumentBuilder();
      this.doc = localDocumentBuilder.parse(new InputSource(new StringReader(paramString)));
      if (!this.preservingWhiteSpace)
        removeWhiteSpaceNodes(this.doc);
    }
    catch (SAXParseException localSAXParseException)
    {
      this.parseError.setException(localSAXParseException);
      return false;
    }
    catch (Exception localException)
    {
      this.parseError.setException(localException);
      return false;
    }
    this.parseError.clear();
    return true;
  }

  public ParseError getParseError()
  {
    return this.parseError;
  }

  public void save(String paramString)
    throws Exception
  {
    throw new Exception("save() is not yet supported.");
  }

  private static boolean isTextValueType(Node paramNode)
  {
    if (paramNode == null)
      return false;
    switch (paramNode.getNodeType())
    {
    case 2:
    case 3:
    case 4:
    case 7:
    case 8:
      return true;
    case 5:
    case 6:
    }
    return false;
  }

  public static String getText(Node paramNode)
  {
    if (paramNode != null)
    {
      if (isTextValueType(paramNode))
        return paramNode.getNodeValue();
      Node localNode;
      for (localNode = paramNode.getFirstChild(); (localNode != null) && (!(localNode instanceof Text)); localNode = localNode.getNextSibling());
      if (localNode != null)
        return localNode.getNodeValue();
    }
    return "";
  }

  public static void setText(Node paramNode, String paramString)
  {
    if (paramNode != null)
    {
      if (isTextValueType(paramNode))
      {
        paramNode.setNodeValue(paramString);
        return;
      }
      Node localNode = paramNode.getFirstChild();
      if ((localNode == null) && (paramNode.getNodeType() == 1))
      {
        paramNode.appendChild(paramNode.getOwnerDocument().createTextNode(paramString));
        return;
      }
      while ((localNode != null) && (!(localNode instanceof Text)))
        localNode = localNode.getNextSibling();
      if (localNode != null)
        localNode.setNodeValue(paramString);
    }
  }

  public static void removeWhiteSpaceNodes(Node paramNode)
  {
    if (paramNode == null)
      return;
    NodeList localNodeList = paramNode.getChildNodes();
    if (localNodeList == null)
      return;
    for (int i = localNodeList.getLength() - 1; i >= 0; i--)
    {
      Node localNode = localNodeList.item(i);
      if ((localNode.getNodeType() == 3) && ((localNode.getNodeValue() == null) || (localNode.getNodeValue().trim().length() == 0)))
        paramNode.removeChild(localNode);
      else
        removeWhiteSpaceNodes(localNode);
    }
  }

  public static class ParseError
  {
    private Exception ex = null;
    private int errorCode = 0;
    private int line = 0;
    private int colpos = 0;

    void clear()
    {
      this.errorCode = 0;
      this.ex = null;
      this.line = 0;
      this.colpos = 0;
    }

    void setException(Exception paramException)
    {
      clear();
      this.ex = paramException;
    }

    void setException(SAXParseException paramSAXParseException)
    {
      setException(paramSAXParseException);
      this.line = paramSAXParseException.getLineNumber();
      this.colpos = paramSAXParseException.getColumnNumber();
      this.errorCode = 1;
    }

    public String getMessage()
    {
      return this.ex == null ? "" : this.ex.getMessage();
    }

    public int getErrorCode()
    {
      return this.errorCode;
    }

    public int getLineNumber()
    {
      return this.line;
    }

    public int getColumnNumber()
    {
      return this.colpos;
    }
  }
}