package com.zfbots.util;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.PrintStream;
import java.util.EventListener;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class UI
{
  public static String copyright = "Copyright 2013 zfbots Inc. All rights reserved.";
  private static String companyName = "zfbots";

  public static void main(String[] paramArrayOfString)
  {
    System.out.println(copyright);
    System.out.println("No startup class available.");
  }

  public static JScrollPane getScrollPane(Component paramComponent)
  {
    Container localContainer = paramComponent.getParent();
    localContainer = localContainer == null ? null : localContainer.getParent();
    if ((localContainer instanceof JScrollPane))
      return (JScrollPane)localContainer;
    return null;
  }

  public static JFrame getJFrame(Component paramComponent)
  {
    while (paramComponent != null)
    {
      if ((paramComponent instanceof JFrame))
        return (JFrame)paramComponent;
      if ((paramComponent instanceof Window))
        return null;
      paramComponent = paramComponent.getParent();
    }
    return null;
  }

  public static Frame getFrame(Component paramComponent)
  {
    while (paramComponent != null)
    {
      if ((paramComponent instanceof Frame))
        return (Frame)paramComponent;
      paramComponent = paramComponent.getParent();
    }
    return null;
  }

  public static Applet getApplet(Component paramComponent)
  {
    while (paramComponent != null)
    {
      if ((paramComponent instanceof Applet))
        return (Applet)paramComponent;
      paramComponent = paramComponent.getParent();
    }
    return null;
  }

  public static Component getParent(Component paramComponent, Class paramClass)
  {
    while (paramComponent != null)
    {
      if (paramClass.isInstance(paramComponent))
        return paramComponent;
      paramComponent = paramComponent.getParent();
    }
    return null;
  }

  public static void forwardKeyPressed(KeyEvent paramKeyEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(KeyListener.class);
    KeyEvent localKeyEvent = null;
    if (arrayOfEventListener.length > 0)
      localKeyEvent = new KeyEvent(paramComponent, paramKeyEvent.getID(), paramKeyEvent.getWhen(), paramKeyEvent.getModifiers(), paramKeyEvent.getKeyCode(), paramKeyEvent.getKeyChar());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((KeyListener)arrayOfEventListener[i]).keyPressed(localKeyEvent);
  }

  public static void forwardKeyReleased(KeyEvent paramKeyEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(KeyListener.class);
    KeyEvent localKeyEvent = null;
    if (arrayOfEventListener.length > 0)
      localKeyEvent = new KeyEvent(paramComponent, paramKeyEvent.getID(), paramKeyEvent.getWhen(), paramKeyEvent.getModifiers(), paramKeyEvent.getKeyCode(), paramKeyEvent.getKeyChar());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((KeyListener)arrayOfEventListener[i]).keyReleased(localKeyEvent);
  }

  public static void forwardKeyTyped(KeyEvent paramKeyEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(KeyListener.class);
    KeyEvent localKeyEvent = null;
    if (arrayOfEventListener.length > 0)
      localKeyEvent = new KeyEvent(paramComponent, paramKeyEvent.getID(), paramKeyEvent.getWhen(), paramKeyEvent.getModifiers(), paramKeyEvent.getKeyCode(), paramKeyEvent.getKeyChar());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((KeyListener)arrayOfEventListener[i]).keyTyped(localKeyEvent);
  }

  public static void forwardFocusGained(FocusEvent paramFocusEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(FocusListener.class);
    FocusEvent localFocusEvent = null;
    if (arrayOfEventListener.length > 0)
      localFocusEvent = new FocusEvent(paramComponent, paramFocusEvent.getID(), paramFocusEvent.isTemporary());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((FocusListener)arrayOfEventListener[i]).focusGained(localFocusEvent);
  }

  public static void forwardFocusLost(FocusEvent paramFocusEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(FocusListener.class);
    FocusEvent localFocusEvent = null;
    if (arrayOfEventListener.length > 0)
      localFocusEvent = new FocusEvent(paramComponent, paramFocusEvent.getID(), paramFocusEvent.isTemporary());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((FocusListener)arrayOfEventListener[i]).focusLost(localFocusEvent);
  }

  public static void forwardMouseClicked(MouseEvent paramMouseEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(MouseListener.class);
    MouseEvent localMouseEvent = null;
    if (arrayOfEventListener.length > 0)
      localMouseEvent = new MouseEvent(paramComponent, paramMouseEvent.getID(), paramMouseEvent.getWhen(), paramMouseEvent.getModifiers(), paramMouseEvent.getX(), paramMouseEvent.getY(), paramMouseEvent.getClickCount(), paramMouseEvent.isPopupTrigger());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((MouseListener)arrayOfEventListener[i]).mouseClicked(localMouseEvent);
  }

  public static void forwardMousePressed(MouseEvent paramMouseEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(MouseListener.class);
    MouseEvent localMouseEvent = null;
    if (arrayOfEventListener.length > 0)
      localMouseEvent = new MouseEvent(paramComponent, paramMouseEvent.getID(), paramMouseEvent.getWhen(), paramMouseEvent.getModifiers(), paramMouseEvent.getX(), paramMouseEvent.getY(), paramMouseEvent.getClickCount(), paramMouseEvent.isPopupTrigger());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((MouseListener)arrayOfEventListener[i]).mousePressed(localMouseEvent);
  }

  public static void forwardMouseReleased(MouseEvent paramMouseEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(MouseListener.class);
    MouseEvent localMouseEvent = null;
    if (arrayOfEventListener.length > 0)
      localMouseEvent = new MouseEvent(paramComponent, paramMouseEvent.getID(), paramMouseEvent.getWhen(), paramMouseEvent.getModifiers(), paramMouseEvent.getX(), paramMouseEvent.getY(), paramMouseEvent.getClickCount(), paramMouseEvent.isPopupTrigger());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((MouseListener)arrayOfEventListener[i]).mouseReleased(localMouseEvent);
  }

  public static void forwardMouseEntered(MouseEvent paramMouseEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(MouseListener.class);
    MouseEvent localMouseEvent = null;
    if (arrayOfEventListener.length > 0)
      localMouseEvent = new MouseEvent(paramComponent, paramMouseEvent.getID(), paramMouseEvent.getWhen(), paramMouseEvent.getModifiers(), paramMouseEvent.getX(), paramMouseEvent.getY(), paramMouseEvent.getClickCount(), paramMouseEvent.isPopupTrigger());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((MouseListener)arrayOfEventListener[i]).mouseEntered(localMouseEvent);
  }

  public static void forwardMouseExited(MouseEvent paramMouseEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(MouseListener.class);
    MouseEvent localMouseEvent = null;
    if (arrayOfEventListener.length > 0)
      localMouseEvent = new MouseEvent(paramComponent, paramMouseEvent.getID(), paramMouseEvent.getWhen(), paramMouseEvent.getModifiers(), paramMouseEvent.getX(), paramMouseEvent.getY(), paramMouseEvent.getClickCount(), paramMouseEvent.isPopupTrigger());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((MouseListener)arrayOfEventListener[i]).mouseExited(localMouseEvent);
  }

  public static void forwardMouseDragged(MouseEvent paramMouseEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(MouseMotionListener.class);
    MouseEvent localMouseEvent = null;
    if (arrayOfEventListener.length > 0)
      localMouseEvent = new MouseEvent(paramComponent, paramMouseEvent.getID(), paramMouseEvent.getWhen(), paramMouseEvent.getModifiers(), paramMouseEvent.getX(), paramMouseEvent.getY(), paramMouseEvent.getClickCount(), paramMouseEvent.isPopupTrigger());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((MouseMotionListener)arrayOfEventListener[i]).mouseDragged(localMouseEvent);
  }

  public static void forwardMouseMoved(MouseEvent paramMouseEvent, Component paramComponent)
  {
    EventListener[] arrayOfEventListener = paramComponent.getListeners(MouseMotionListener.class);
    MouseEvent localMouseEvent = null;
    if (arrayOfEventListener.length > 0)
      localMouseEvent = new MouseEvent(paramComponent, paramMouseEvent.getID(), paramMouseEvent.getWhen(), paramMouseEvent.getModifiers(), paramMouseEvent.getX(), paramMouseEvent.getY(), paramMouseEvent.getClickCount(), paramMouseEvent.isPopupTrigger());
    for (int i = 0; i < arrayOfEventListener.length; i++)
      ((MouseMotionListener)arrayOfEventListener[i]).mouseMoved(localMouseEvent);
  }
}