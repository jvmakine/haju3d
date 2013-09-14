package fi.haju.haju3d.server;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.server.world.PerlinNoiseWorldGenerator;
import fi.haju.haju3d.server.world.WorldGenerator;

public class ServerImpl implements Server {
   
  private WorldGenerator generator;
  
  List<Client> loggedInClients = Lists.newArrayList(); 
  
  public ServerImpl() {
    generator = new PerlinNoiseWorldGenerator();
    generator.setSeed(new Random().nextInt());  
  }

  @Override
  public synchronized void login(Client client) {
    loggedInClients.add(client);
  }

  @Override
  public synchronized void logout(Client client) {
    loggedInClients.remove(client);
  }
  
  @Override
  public Chunk getChunk(Vector3i position) throws RemoteException {
    return generator.generateChunk(position);
  }
  

}
