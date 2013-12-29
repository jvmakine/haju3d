package fi.haju.haju3d.server;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;
import fi.haju.haju3d.protocol.interaction.WorldEdit;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.ChunkCoordinateSystem;
import fi.haju.haju3d.protocol.world.World;
import fi.haju.haju3d.server.world.WorldGenerator;
import fi.haju.haju3d.server.world.WorldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.*;

@Singleton
public class ServerImpl implements Server {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerImpl.class);

  @Inject
  private WorldGenerator generator;

  @Inject
  private WorldSaver saver;

  @Inject
  private ServerSettings settings;

  private List<Client> loggedInClients = Collections.synchronizedList(new ArrayList<Client>());
  private ChunkCoordinateSystem chunkCoordinateSystem = ChunkCoordinateSystem.DEFAULT;
  private World world = new World(chunkCoordinateSystem);

  private interface AsyncClientCall {
    void run() throws RemoteException;
  }

  public void start() {
    settings.init();

    Optional<WorldInfo> opt = saver.loadInfoIfOnDisk();
    if (!opt.isPresent()) {
      int seed = new Random().nextInt();
      String name = settings.getWorldName();
      WorldInfo info = new WorldInfo(name, seed);
      opt = Optional.of(info);
      saver.saveWorldInfo(info);
    }
    generator.setSeed(opt.get().getSeed());
  }

  public void shutdown() {
    saver.shutdown();
  }

  @Override
  public synchronized void login(Client client) {
    LOGGER.info("Client " + client + " logged in");
    loggedInClients.add(client);
  }

  @Override
  public synchronized void logout(Client client) {
    loggedInClients.remove(client);
  }

  @Override
  public Chunk getChunk(ChunkPosition position) throws RemoteException {
    return getOrGenerateChunk(position);
  }

  private synchronized Chunk getOrGenerateChunk(ChunkPosition position) {
    if (world.hasChunk(position)) {
      return world.getChunk(position);
    } else {
      Optional<Chunk> opt = saver.loadChunkIfOnDisk(position);
      if (opt.isPresent()) {
        world.setChunk(position, opt.get());
        return opt.get();
      }
      LOGGER.info("generating chunk " + position);
      int size = chunkCoordinateSystem.getChunkSize();
      Chunk newChunk = generator.generateChunk(position, size);
      world.setChunk(position, newChunk);
      saver.saveChunk(newChunk);
      return newChunk;
    }
  }

  @Override
  public List<Chunk> getChunks(Collection<ChunkPosition> positions) throws RemoteException {
    List<Chunk> chunks = Lists.newArrayList();
    for (ChunkPosition pos : positions) {
      chunks.add(getChunk(pos));
    }
    return chunks;
  }

  @Override
  public void registerWorldEdits(final List<WorldEdit> edits) {
    for (WorldEdit edit : edits) {
      Chunk chunk = getOrGenerateChunk(edit.getPosition().getChunkPosition());
      LocalTilePosition p = edit.getPosition().getTileWithinChunk();
      chunk.set(p.x, p.y, p.z, edit.getNewTile());
      saver.saveChunk(chunk);
    }
    for (final Client client : loggedInClients) {
      asyncCall(client, new AsyncClientCall() {
        public void run() throws RemoteException {
          client.registerWorldEdits(edits);
        }
      });
    }
  }

  private void asyncCall(final Client client, final AsyncClientCall call) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          call.run();
        } catch (RemoteException e) {
          LOGGER.warn("Error communicating with client " + client + " disconnecting");
          disconnect(client);
        }
      }
    }).start();
  }

  @Override
  public void disconnect(Client client) {
    LOGGER.info("Disconnecting " + client);
    loggedInClients.remove(client);
  }

}

