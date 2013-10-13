package fi.haju.haju3d.client;

import java.rmi.RemoteException;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fi.haju.haju3d.client.connection.ServerConnector;
import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.server.ServerImpl;
import fi.haju.haju3d.server.world.PerlinNoiseWorldGenerator;

public class LocalClientRunner {
  public static void main(String[] args) throws RemoteException {
    Injector injector = Guice.createInjector(new ClientModule());
    ServerImpl server = new ServerImpl();
    injector.getInstance(ServerConnector.class).setRemoteServer(server);
    PerlinNoiseWorldGenerator wg = new PerlinNoiseWorldGenerator();
    server.setGenerator(wg);
    ChunkRenderer app = injector.getInstance(ChunkRenderer.class);
    Client client = new ClientImpl(app);
    server.login(client);
    app.start();
  }
}
