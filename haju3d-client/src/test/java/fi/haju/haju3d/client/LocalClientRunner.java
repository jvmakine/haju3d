package fi.haju.haju3d.client;

import java.rmi.RemoteException;

import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.server.ServerImpl;
import fi.haju.haju3d.server.world.PerlinNoiseWorldGenerator;

public class LocalClientRunner {
  public static void main(String[] args) throws RemoteException {
    ServerImpl server = new ServerImpl();
    PerlinNoiseWorldGenerator wg = new PerlinNoiseWorldGenerator();
    server.setGenerator(wg);
    ChunkRenderer app = new ChunkRenderer(new ChunkProvider(server), server);
    Client client = new ClientImpl(app);
    server.login(client);
    app.start();
  }
}
