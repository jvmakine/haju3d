package fi.haju.haju3d.client;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import fi.haju.haju3d.client.ui.MainFrame;

/**
 * Class to start the client
 */
public class ClientRunner {

  public static void main(String[] args) {
    final MainFrame app = new MainFrame();
    SwingUtilities.invokeLater (
        new Runnable() {
          public void run() {
            // Removes the normal title bar. Most games do this.
            app.setUndecorated(true);
            app.setVisible(true);
            // Goes into fullscreen-exclusive mode
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            gd.setFullScreenWindow(app);
          }
        }
    );
 
  }

}
