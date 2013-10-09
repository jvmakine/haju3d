package fi.haju.haju3d.client;

import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;

/**
 * Class to start the client
 */
public class ClientRunner {

  public static void main(String[] args) throws Exception {  
    
    Registry registry = LocateRegistry.getRegistry(5250);
    Server server = (Server)registry.lookup("haju3d_server");
        
    ChunkRenderer app = new ChunkRenderer(new ChunkProvider(server), server);
    
    final Client client = new ClientImpl(app);
    Client stub = (Client)UnicastRemoteObject.exportObject(client, 5251);
    server.login(stub);
    
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
