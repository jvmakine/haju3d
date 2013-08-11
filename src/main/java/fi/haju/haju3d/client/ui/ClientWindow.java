package fi.haju.haju3d.client.ui;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.glu.GLU;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;

public class ClientWindow implements Runnable, WindowListener, GLEventListener {

  private static final String TITLE = "Haju3D";
  
  private GLWindow window;
  private Screen screen;
  private Display display;
  private boolean running = false; 
  
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
    window.addGLEventListener(this);
    window.setDefaultCloseOperation(WindowClosingMode.DISPOSE_ON_CLOSE);
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
    running = false;
  }

  public void windowGainedFocus(WindowEvent event) {
  }

  public void windowLostFocus(WindowEvent event) {
  }

  public void windowMoved(WindowEvent event) {
  }

  public void windowRepaint(WindowUpdateEvent event) {
  }

  public void windowResized(WindowEvent event) {
  }

  public void display(GLAutoDrawable glAutoDrawable) {
    int width = window.getWidth();
    int height = window.getHeight();
    GL2 gl2 = (GL2)glAutoDrawable.getGL();
    gl2.glClear( GL.GL_COLOR_BUFFER_BIT );

    // draw a triangle filling the window
    gl2.glLoadIdentity();
    gl2.glBegin( GL.GL_TRIANGLES );
    gl2.glColor3f( 1, 0, 0 );
    gl2.glVertex2f( 0, 0 );
    gl2.glColor3f( 0, 1, 0 );
    gl2.glVertex2f( width, 0 );
    gl2.glColor3f( 0, 0, 1 );
    gl2.glVertex2f( width / 2, height );
    gl2.glEnd();    
  }

  public void dispose(GLAutoDrawable glAutoDrawable) {
    running = false;
  }

  public void init(GLAutoDrawable glAutoDrawable) {
    int width = window.getWidth();
    int height = window.getHeight();
    GL2 gl2 = (GL2)glAutoDrawable.getGL();
    gl2.glMatrixMode( GL2.GL_PROJECTION );
    gl2.glLoadIdentity();
    GLU glu = new GLU();
    glu.gluOrtho2D( 0.0f, width, 0.0f, height );
    gl2.glMatrixMode( GL2.GL_MODELVIEW );
    gl2.glLoadIdentity();
    gl2.glViewport( 0, 0, width, height );
  }

  public void reshape(GLAutoDrawable glAutoDrawable, int arg1, int arg2, int arg3, int arg4) {
    init(glAutoDrawable);
  }
  
  
}
