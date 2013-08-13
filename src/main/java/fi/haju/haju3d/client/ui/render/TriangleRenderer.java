package fi.haju.haju3d.client.ui.render;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

public class TriangleRenderer implements GLEventListener {

  public void display(GLAutoDrawable glAutoDrawable) {
    drawTriangle(glAutoDrawable.getWidth(), glAutoDrawable.getHeight(), (GL2)glAutoDrawable.getGL());    
  }

  /**
   * draw a triangle filling the window
   */
  private void drawTriangle(int width, int height, GL2 gl2) {
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
  }
  
  

}
