package fi.haju.haju3d.client.ui.render;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

public class Ortho2DViewportRenderer implements GLEventListener {

  public void display(GLAutoDrawable glAutoDrawable) {
  }

  public void dispose(GLAutoDrawable glAutoDrawable) {
  }

  public void init(GLAutoDrawable glAutoDrawable) {
    int width = glAutoDrawable.getWidth();
    int height = glAutoDrawable.getHeight();
    GL2 gl2 = (GL2)glAutoDrawable.getGL();
    gl2.glMatrixMode( GL2.GL_PROJECTION );
    gl2.glLoadIdentity();
    GLU glu = new GLU();
    glu.gluOrtho2D( 0.0f, width, 0.0f, height );
    gl2.glMatrixMode( GL2.GL_MODELVIEW );
    gl2.glLoadIdentity();
    gl2.glViewport( 0, 0, width, height );
  }

  public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
    init(glAutoDrawable);
  }

}
