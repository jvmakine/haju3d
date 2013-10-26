package fi.haju.haju3d.server;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Singleton
public class WorldSaver {

  private static final long MIN_SAVE_INTERVAL = 60000;
  private static final Logger LOGGER = LoggerFactory.getLogger(WorldSaver.class);

  private Queue<Chunk> chunksToSave = new ConcurrentLinkedQueue<Chunk>();
  private Map<Chunk, Long> lastSaveTimes = Maps.newHashMap();

  private final Thread saverThread = new Thread(new Runnable() {
    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(100);
          while (!chunksToSave.isEmpty()) {
            Chunk toSave = null;
            synchronized (chunksToSave) {
              toSave = chunksToSave.poll();
            }
            if (toSave != null) {
              LOGGER.info("Saving chunk : " + toSave.getPosition());
              writeObjectToFile(chunkFile(toSave.getPosition()), toSave);
            }
          }
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  });

  @Inject
  private ServerSettings settings;

  public void init() {
    saverThread.run();
  }

  public void saveChunkIfNecessary(Chunk chunk) {
    synchronized (chunksToSave) {
      if ((!lastSaveTimes.containsKey(chunk) || lastSaveTimes.get(chunk) < System.currentTimeMillis() - MIN_SAVE_INTERVAL)
          && !chunksToSave.contains(chunk)) {
        lastSaveTimes.put(chunk, System.currentTimeMillis());
        chunksToSave.add(chunk);
      }
    }
  }

  public Optional<Chunk> loadChunkIfOnDisk(Vector3i pos) {
    File file = chunkFile(pos);
    if (!file.exists()) return Optional.absent();
    LOGGER.info("loading from disk : " + pos);
    return Optional.of((Chunk) readObjectFromFile(file));
  }

  private static void writeObjectToFile(File file, Serializable object) {
    try {
      FileUtils.writeByteArrayToFile(file, SerializationUtils.serialize(object));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T readObjectFromFile(File file) {
    try {
      return (T) SerializationUtils.deserialize(FileUtils.readFileToByteArray(file));
    } catch (SerializationException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static File getDirAt(String path) {
    File dir = new File(path);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return dir;
  }

  private File chunkFile(Vector3i position) {
    return new File(getDirAt(settings.getSavePath() + "/" + settings.getWorldName()), "ch#" + position.x + "#" + position.y + "#" + position.z);
  }

}
