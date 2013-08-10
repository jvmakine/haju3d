package fi.haju.haju3d.client.ui;

import java.awt.BorderLayout;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

public class MainFrame extends JFrame {

  private static final long serialVersionUID = 1L;

  public MainFrame() {
    // Title
    super("JOGL Application");
 
    // Kill the process when the JFrame is closed
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    GLProfile glprofile = GLProfile.getDefault();
    GLCapabilities glcapabilities = new GLCapabilities( glprofile );
    final GLCanvas glcanvas = new GLCanvas( glcapabilities );

    glcanvas.addGLEventListener( new GLEventListener() {

      public void display(GLAutoDrawable glAutoDrawable) {
        int width = glAutoDrawable.getWidth();
        int height = glAutoDrawable.getHeight();
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
      }

      public void init(GLAutoDrawable glAutoDrawable) {
      }

      public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        GL2 gl2 = (GL2)glAutoDrawable.getGL();
        gl2.glMatrixMode( GL2.GL_PROJECTION );
        gl2.glLoadIdentity();

        GLU glu = new GLU();
        glu.gluOrtho2D( 0.0f, width, 0.0f, height );

        gl2.glMatrixMode( GL2.GL_MODELVIEW );
        gl2.glLoadIdentity();

        gl2.glViewport( 0, 0, width, height );
      }
     
        
    });
    getContentPane().add( glcanvas, BorderLayout.CENTER );
    setSize( 640, 480 );
    setVisible( true );
  }
  
}
