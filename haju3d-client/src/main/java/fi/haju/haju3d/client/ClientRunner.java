package fi.haju.haju3d.client;

import com.google.inject.Guice;
import com.google.inject.Injector;
import fi.haju.haju3d.client.connection.ServerConnector;
import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;

import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Class to start the client
 */
public class ClientRunner {

  public static void main(String[] args) throws Exception {

    Injector injector = Guice.createInjector(new ClientModule());
    final ServerConnector server = injector.getInstance(ServerConnector.class);
    server.connect();
    ChunkRenderer app = injector.getInstance(ChunkRenderer.class);

    final Client client = new ClientImpl(app);
    Client stub = (Client) UnicastRemoteObject.exportObject(client, 5251);
    server.login(stub);

    app.setCloseEventHandler(new CloseEventHandler() {
      @Override
      public void onClose() {
        try {
          server.disconnect(client);
          UnicastRemoteObject.unexportObject(client, false);
        } catch (NoSuchObjectException e) {
          //TODO : Proper error handling
          e.printStackTrace();
        }
      }
    });
    app.start();
  }

}
