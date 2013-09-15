package fi.haju.haju3d.client;

import java.rmi.RemoteException;

import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.server.ServerImpl;

public class LocalClientRunner {
  public static void main(String[] args) throws RemoteException {
    Client client = new ClientImpl();
    Server server = new ServerImpl();
    server.login(client);
    
    ChunkRenderer app = new ChunkRenderer(new ChunkProvider(server));;
    app.start();
  }
}
