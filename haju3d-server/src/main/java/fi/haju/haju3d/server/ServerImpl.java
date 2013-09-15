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
  
  List<Client> loggedInClients = Lists.newArrayList(); 
  Map<Vector3i, Chunk> chunks = Maps.newHashMap();
  
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
    Chunk center = getOrGenerateChunk(position);
    Chunk xp1 = getOrGenerateChunk(position.add(1, 0, 0));
    Chunk xm1 = getOrGenerateChunk(position.add(-1, 0, 0));
    Chunk zp1 = getOrGenerateChunk(position.add(0, 0, 1));
    Chunk zm1 = getOrGenerateChunk(position.add(0, 0, -1));
    center.setXMinusFrom(xm1);
    center.setXPlusFrom(xp1);
    center.setZMinusFrom(zm1);
    center.setZPlusFrom(zp1);
    return center;
  }
  
  private Chunk getOrGenerateChunk(Vector3i position) {
    if(chunks.keySet().contains(position)) {
      return chunks.get(position);
    } else {
      Chunk newChunk = generator.generateChunk(position);
      chunks.put(position, newChunk);
      return newChunk;
    }
  }

}

