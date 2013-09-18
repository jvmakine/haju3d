package fi.haju.haju3d.server;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.server.world.PerlinNoiseWorldGenerator;
import fi.haju.haju3d.server.world.WorldGenerator;

public class ServerImpl implements Server {
   
  private WorldGenerator generator;
  private List<Client> loggedInClients = Lists.newArrayList();
  private Map<Vector3i, Chunk> chunks = Maps.newHashMap();
  
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
    return getOrGenerateChunk(position);
  }
  
  private Chunk getOrGenerateChunk(Vector3i position) {
    if(chunks.containsKey(position)) {
      return chunks.get(position);
    } else {
      Chunk newChunk = generator.generateChunk(position);
      chunks.put(position, newChunk);
      return newChunk;
    }
  }

}

