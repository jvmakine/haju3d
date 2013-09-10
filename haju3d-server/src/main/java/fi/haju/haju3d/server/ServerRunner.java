package fi.haju.haju3d.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import fi.haju.haju3d.protocol.Server;

public class ServerRunner {

  public static void main(String[] args) {
    try {
      Registry registry = LocateRegistry.createRegistry(5250);
      ServerImpl server = new ServerImpl();
      Server stub = (Server)UnicastRemoteObject.exportObject(server, 5250);
      registry.rebind("haju3d_server", stub);
      System.out.println("Haju3d bound");
      while(true) {
        Thread.sleep(100);
        server.messageToClients();
      }
    } catch (RemoteException | InterruptedException e) {
      System.err.println("Haju3d exception:");
      e.printStackTrace();
    }
  }

}
