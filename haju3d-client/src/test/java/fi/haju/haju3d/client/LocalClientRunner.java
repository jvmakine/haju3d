package fi.haju.haju3d.client;

import com.google.inject.Guice;
import com.google.inject.Injector;
import fi.haju.haju3d.client.connection.ServerConnector;
import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.server.ServerImpl;
import fi.haju.haju3d.server.ServerModule;

import java.rmi.RemoteException;

public class LocalClientRunner {
  public static void main(String[] args) throws RemoteException {
    Injector clientInjector = Guice.createInjector(new ClientModule());
    Injector serverInjector = Guice.createInjector(new ServerModule());

    final ServerImpl server = serverInjector.getInstance(ServerImpl.class);
    clientInjector.getInstance(ServerConnector.class).setRemoteServer(server);

    server.start();

    ChunkRenderer app = clientInjector.getInstance(ChunkRenderer.class);

    app.setCloseEventHandler(new CloseEventHandler() {
      @Override
      public void onClose() {
        server.shutdown();
      }
    });

    Client client = new ClientImpl(app);
    server.login(client);
    app.start();
  }
}
