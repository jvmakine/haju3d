package fi.haju.haju3d.client;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;

public class ChunkProvider {
  private final Server server;

  private Map<Vector3i, Chunk> chunkCache = new ConcurrentHashMap<Vector3i, Chunk>();
  private Set<Vector3i> requests = Collections.newSetFromMap(new ConcurrentHashMap<Vector3i, Boolean>());
  
  public ChunkProvider(Server server) {
    this.server = server;
  }
  
  public void requestChunk(final Vector3i position, final ChunkProcessor processor) {
    System.out.println("Requested " + position);
    requests.add(position);
    new Thread(new Runnable() {
      @Override
      public void run() {
        processor.chunkLoaded(getChunk(position));
        requests.remove(position);
      }
    }).run();
  }
  
  public boolean hasChunk(Vector3i position) {
    if(requests.contains(position)) return true;
    return chunkCache.containsKey(position);
  }
  
  private Chunk getChunk(Vector3i position) {
    if(!chunkCache.containsKey(position)) {
      try {
        chunkCache.put(position, server.getChunk(position));
      } catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }
    return chunkCache.get(position);
  }
  
}
