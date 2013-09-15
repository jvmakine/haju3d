package fi.haju.haju3d.client;

import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.jme3.system.AppSettings;

import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;

/**
 * Class to start the client
 */
public class ClientRunner {

  public static void main(String[] args) throws Exception {
    final Client client = new ClientImpl();
    Client stub = (Client)UnicastRemoteObject.exportObject(client, 5251);
    Registry registry = LocateRegistry.getRegistry(5250);
    Server server = (Server)registry.lookup("haju3d_server");
    server.login(stub);

    ChunkRenderer app = new ChunkRenderer(server.getChunk(new Vector3i(0, 0, 0)));
    AppSettings settings = new AppSettings(true);
    settings.setVSync(true);
    settings.setAudioRenderer(null);
    settings.setFullscreen(true);
    app.setSettings(settings);
    app.setShowSettings(false);
    app.setCloseEventHandler(new CloseEventHandler() {
      @Override
      public void onClose() {
        try {
          UnicastRemoteObject.unexportObject(client, false);
        } catch (NoSuchObjectException e) {
          e.printStackTrace();
        }
      }
    });
    app.start();
  }

}
