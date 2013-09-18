package fi.haju.haju3d.client;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;

public class ChunkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProvider.class);
    
  private final Server server;
  private Map<Vector3i, Chunk> chunkCache = new ConcurrentHashMap<Vector3i, Chunk>();
  private Set<List<Vector3i>> requests = Collections.newSetFromMap(new ConcurrentHashMap<List<Vector3i>, Boolean>());
  
  public ChunkProvider(Server server) {
    this.server = server;
  }
  
  public void requestChunks(final List<Vector3i> positions, final ChunkProcessor processor) {
    LOGGER.info("Requested " + positions);
    
    requests.add(positions);
    new Thread(new Runnable() {
      @Override
      public void run() {
        List<Chunk> chunks = new ArrayList<>();
        for (Vector3i pos : positions) {
          chunks.add(getChunk(pos));
        }
        processor.chunksLoaded(chunks);
        requests.remove(positions);
      }
    }).start();
  }
  
  public boolean hasChunk(Vector3i position) {
    if(requests.contains(position)) {
      return true;
    }
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
