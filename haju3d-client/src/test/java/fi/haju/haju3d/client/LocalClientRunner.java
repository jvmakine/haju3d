package fi.haju.haju3d.client;

import java.rmi.RemoteException;

import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.server.ServerImpl;
import fi.haju.haju3d.server.world.PerlinNoiseWorldGenerator;

public class LocalClientRunner {
  public static void main(String[] args) throws RemoteException {
    Client client = new ClientImpl();
    ServerImpl server = new ServerImpl();
    PerlinNoiseWorldGenerator wg = new PerlinNoiseWorldGenerator();
    wg.setFastMode(true);
    server.setGenerator(wg);
//    server.setFileMode(true);
    server.login(client);
    
    ChunkRenderer app = new ChunkRenderer(new ChunkProvider(server));
    app.start();
  }
}
