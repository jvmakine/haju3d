package fi.haju.haju3d.client;

import com.google.common.collect.Lists;

import fi.haju.haju3d.client.ui.ClientWindow;
import fi.haju.haju3d.client.ui.render.Ortho2DViewportRenderer;
import fi.haju.haju3d.client.ui.render.TriangleRenderer;

/**
 * Class to start the client
 */
public class ClientRunner {

  public static void main(String[] args) {
    ClientWindow window = new ClientWindow();
    window.setGlListeners(Lists.newArrayList(
        new Ortho2DViewportRenderer(),
        new TriangleRenderer()
    ));
    window.run();
  }

}
