package fi.haju.haju3d.server;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.interaction.WorldEdit;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;
import fi.haju.haju3d.server.world.PerlinNoiseWorldGenerator;
import fi.haju.haju3d.server.world.WorldGenerator;

public class ServerImpl implements Server {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerImpl.class);
   
  private WorldGenerator generator;
  private List<Client> loggedInClients = new ArrayList<>();
  private World world = new World();
  private boolean fileMode;
  private File hajuDir = getHajuDir();
  
  public ServerImpl() {
    generator = new PerlinNoiseWorldGenerator();
    generator.setSeed(new Random().nextInt());
  }
  
  public void setFileMode(boolean fileMode) {
    this.fileMode = fileMode;
  }
  
  private void createAndSaveWorld() {
    LOGGER.info("createAndSaveWorld");
    
    int sz = world.getChunkSize();
    int chunks = 10;
    Chunk worldChunk = generator.generateChunk(new Vector3i(), sz * chunks, sz, sz * chunks);
    
    HashSet<Vector3i> validChunks = new HashSet<>();
    for (int x = 0; x < chunks; x++) {
      for (int z = 0; z < chunks; z++) {
        Vector3i position = new Vector3i(x - chunks / 2, 0, z - chunks / 2);
        Chunk chunk = new Chunk(sz, sz, sz, x + z * 100, position);
        for (int xx = 0; xx < sz; xx++) {
          for (int yy = 0; yy < sz; yy++) {
            for (int zz = 0; zz < sz; zz++) {
              chunk.set(xx, yy, zz, worldChunk.get(xx + x * sz, yy, zz + z * sz));
              chunk.setColor(xx, yy, zz, worldChunk.getColor(xx + x * sz, yy, zz + z * sz));
            }
          }
        }
        validChunks.add(position);
        writeObjectToFile(chunkFile(position), chunk);
      }
    }
    
    writeObjectToFile(validChunksFile(), validChunks);
  }
  
  private static void writeObjectToFile(File file, Serializable object) {
    try {
      FileUtils.writeByteArrayToFile(file, SerializationUtils.serialize(object));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @SuppressWarnings("unchecked")
  private static <T> T readObjectFromFile(File file) throws IOException {
    try {
      return (T) SerializationUtils.deserialize(FileUtils.readFileToByteArray(file));
    } catch (SerializationException e) {
      throw new IOException(e);
    }
  }
  
  private File chunkFile(Vector3i position) {
    return new File(hajuDir, "ch#" + position.x + "#" + position.z);
  }

  private File validChunksFile() {
    return new File(hajuDir, "chunks");
  }

  private File getHajuDir() {
    File hajuDir = new File(new File(System.getProperty("user.home")), ".haju3d");
    hajuDir.mkdirs();
    return hajuDir;
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
  
  private synchronized Chunk getOrGenerateChunk(Vector3i position) {
    LOGGER.info("getOrGenerateChunk: " + position);
    
    if (fileMode) {
      int sz = world.getChunkSize();
      if (position.y < 0) {
        return new Chunk(sz, sz, sz, 0, position, Tile.GROUND);
      } else if (position.y > 0) {
        return new Chunk(sz, sz, sz, 0, position, Tile.AIR);
      }
      HashSet<Vector3i> validChunks;
      try {
        validChunks = readObjectFromFile(validChunksFile());
      } catch (IOException | SerializationException e) {
        createAndSaveWorld();
        return getOrGenerateChunk(position);
      }
      if (!validChunks.contains(position)) {
        return new Chunk(sz, sz, sz, 0, position, Tile.AIR);
      }
      try {
        return readObjectFromFile(chunkFile(position));
      } catch (IOException | SerializationException e) {
        createAndSaveWorld();
        return getOrGenerateChunk(position);
      }
    }
    
    if(world.hasChunk(position)) {
      return world.getChunk(position);
    } else {
      int sz = world.getChunkSize();
      Chunk newChunk = generator.generateChunk(position, sz, sz, sz);
      world.setChunk(position, newChunk);
      return newChunk;
    }
  }

  @Override
  public List<Chunk> getChunks(Collection<Vector3i> positions) throws RemoteException {
    List<Chunk> chunks = Lists.newArrayList();
    for(Vector3i pos : positions) {
      chunks.add(getChunk(pos));
    }
    return chunks;
  }

  @Override
  public void registerWorldEdits(final List<WorldEdit> edits) {
    for(final Client client : loggedInClients) {
      asyncCall(new Runnable() {
        @Override
        public void run() {
          try {
            client.registerWorldEdits(edits);
          } catch (RemoteException e) {
            // TODO Log out the client
            LOGGER.error("Error communicating with rhe client", e);
          }          
        }
      });
    }
  }
  
  private void asyncCall(Runnable call) {
    new Thread(call).start();
  }

}

