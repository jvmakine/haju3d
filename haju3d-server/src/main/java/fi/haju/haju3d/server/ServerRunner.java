package fi.haju.haju3d.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.server.world.WorldGenerator;

public class ServerRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerRunner.class);
  private static final String SERVER_NAME = "haju3d_server";
  private static final int PORT = 5250;
  
  public static void main(String[] args) {
    try {
      Injector injector = Guice.createInjector(new ServerModule());
      injector.getInstance(WorldGenerator.class).setSeed(new Random().nextInt());
      ServerImpl server = injector.getInstance(ServerImpl.class);
      Registry registry = LocateRegistry.createRegistry(PORT);
      Server stub = (Server)UnicastRemoteObject.exportObject(server, PORT);
      registry.rebind(SERVER_NAME, stub);
      LOGGER.info("Started Haju3D server at " + PORT);
      while(true) {
        Thread.sleep(100);
      }
    } catch (RemoteException | InterruptedException e) {
      LOGGER.error("Error running Haju3D server", e);
    }
  }

}
