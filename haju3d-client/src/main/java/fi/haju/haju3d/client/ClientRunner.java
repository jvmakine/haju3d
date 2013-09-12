package fi.haju.haju3d.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.jme3.system.AppSettings;

import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;

/**
 * Class to start the client
 */
public class ClientRunner {

  public static void main(String[] args) {
    
    Client client = new ClientImpl();
    
    try {
      Client stub = (Client)UnicastRemoteObject.exportObject(client, 5251);
      Registry registry = LocateRegistry.getRegistry(5250);
      Server server = (Server)registry.lookup("haju3d_server");
      server.login(stub);
      
      ChunkRenderer app = new ChunkRenderer(server.getChunk());
      AppSettings settings = new AppSettings(true);
      settings.setVSync(true);
      settings.setAudioRenderer(null);
      settings.setFullscreen(true);
      app.setSettings(settings);
      app.setShowSettings(false);
      app.start();
    } catch (RemoteException | NotBoundException e) {
      throw new RuntimeException(e);
    }
    
  }

}
