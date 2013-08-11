package fi.haju.haju3d.client.ui;

import java.util.List;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.google.common.collect.Lists;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;

public class ClientWindow implements Runnable, WindowListener {

  private static final String TITLE = "Haju3D";
  
  private GLWindow window;
  private Screen screen;
  private Display display;
  private boolean running = false;
  private List<GLEventListener> glListeners = Lists.newArrayList();
  
  private void setup() {
    GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
    caps.setRedBits(8);
    caps.setGreenBits(8);
    caps.setBlueBits(8);

    display = NewtFactory.createDisplay(null);
    screen = NewtFactory.createScreen(display, 0);
    
    window = GLWindow.create(screen, caps);
    window.setTitle(TITLE);
    window.setUndecorated(false);
    window.setSize(800, 600);
    window.addWindowListener(this);
    window.setDefaultCloseOperation(WindowClosingMode.DISPOSE_ON_CLOSE);
    for (GLEventListener listener : glListeners) {
      window.addGLEventListener(listener);
    }
  }

  public void run() {
    setup();
    running = true;
    window.setVisible(true);
    while(running) {
      display.dispatchMessages();
    }
    window.destroy();
  }

  public void windowDestroyNotify(WindowEvent event) {
    running = false;
  }

  public void windowDestroyed(WindowEvent event) {
  }

  public void windowGainedFocus(WindowEvent event) {
  }

  public void windowLostFocus(WindowEvent areventg0) {
  }

  public void windowMoved(WindowEvent event) {
  }

  public void windowRepaint(WindowUpdateEvent event) {
  }

  public void windowResized(WindowEvent event) {
  }

  public void setGlListeners(List<GLEventListener> glListeners) {
    this.glListeners = glListeners;
  }
  
}
