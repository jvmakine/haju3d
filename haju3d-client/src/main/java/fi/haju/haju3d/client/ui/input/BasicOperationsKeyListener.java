package fi.haju.haju3d.client.ui.input;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;

import fi.haju.haju3d.client.ui.ClientWindow;

public class BasicOperationsKeyListener implements KeyListener {

  private ClientWindow clientWindow;
  
  public void keyPressed(KeyEvent event) {
    switch(event.getKeyCode()) {
      case KeyEvent.VK_F : 
        clientWindow.switchFullscreen();
        break;
      case KeyEvent.VK_ESCAPE :
        clientWindow.close();
        break;
    }
  }

  public void keyReleased(KeyEvent event) {
  }

  public void setClientWindow(ClientWindow clientWindow) {
    this.clientWindow = clientWindow;
  }

}
