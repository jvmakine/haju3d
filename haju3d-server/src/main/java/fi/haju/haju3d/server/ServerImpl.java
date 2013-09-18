package fi.haju.haju3d.server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.World;
import fi.haju.haju3d.server.world.PerlinNoiseWorldGenerator;
import fi.haju.haju3d.server.world.WorldGenerator;

public class ServerImpl implements Server {
   
  private WorldGenerator generator;
  private List<Client> loggedInClients = new ArrayList<>();
  private World world = new World();
  
  public ServerImpl() {
    generator = new PerlinNoiseWorldGenerator();
    generator.setSeed(new Random().nextInt());
  }
  
  public void setGenerator(WorldGenerator generator) {
    this.generator = generator;
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
    return getOrGenerateChunk(position);
  }
  
  private Chunk getOrGenerateChunk(Vector3i position) {
    if(world.hasChunk(position)) {
      return world.getChunk(position);
    } else {
      int sz = world.getChunkSize();
      Chunk newChunk = generator.generateChunk(position, sz, sz, sz);
      world.setChunk(position, newChunk);
      return newChunk;
    }
  }

}

