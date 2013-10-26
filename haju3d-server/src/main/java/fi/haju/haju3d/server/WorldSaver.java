package fi.haju.haju3d.server;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.server.world.WorldInfo;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Singleton
public class WorldSaver {
  private static final LZ4Factory LZ_4_FACTORY = LZ4Factory.fastestInstance();
  private static final LZ4Compressor LZ_4_COMPRESSOR = LZ_4_FACTORY.fastCompressor();
  private static final LZ4FastDecompressor LZ_4_DECOMPRESSOR = LZ_4_FACTORY.fastDecompressor();

  private static final long MIN_SAVE_INTERVAL = 60000;
  private static final Logger LOGGER = LoggerFactory.getLogger(WorldSaver.class);

  private Queue<Chunk> chunksToSave = new ConcurrentLinkedQueue<Chunk>();
  private Map<Chunk, Long> lastSaveTimes = Maps.newHashMap();

  private volatile boolean running = false;

  private void saveLoop() {
    while (running) {
      try {
        Thread.sleep(100);
        while (!chunksToSave.isEmpty()) {
          Chunk toSave;
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

  @Inject
  private ServerSettings settings;

  public void start() {
    if (running) {
      return;
    }
    running = true;
    Thread saverThread = new Thread(new Runnable() {
      @Override
      public void run() {
        saveLoop();
      }
    }, "WorldSaver");
    saverThread.start();
  }

  public void stop() {
    running = false;
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
  
  public void saveWorldInfo(WorldInfo info) {
    writeObjectToFile(infoFile(), info);
  }

  public Optional<Chunk> loadChunkIfOnDisk(Vector3i pos) {
    File file = chunkFile(pos);
    if (!file.exists()) return Optional.absent();
    LOGGER.info("loading from disk : " + pos);
    try {
      return Optional.of((Chunk) readObjectFromFile(file));
    } catch (RuntimeException e) {
      return Optional.absent();
    }
  }
  
  public Optional<WorldInfo> loadInfoIfOnDisk() {
    File file = infoFile();
    if (!file.exists()) return Optional.absent();
    try {
      return Optional.of((WorldInfo)readObjectFromFile(file));
    } catch (RuntimeException e) {
      return Optional.absent();
    }
  }

  private static void writeObjectToFile(File file, Serializable object) {
    try {
      FileUtils.writeByteArrayToFile(file, compress(SerializationUtils.serialize(object)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T readObjectFromFile(File file) {
    try {
      return (T) SerializationUtils.deserialize(decompress(FileUtils.readFileToByteArray(file)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] compress(byte[] bytes) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.write(ByteBuffer.allocate(4).putInt(bytes.length).array(), baos);
    IOUtils.write(LZ_4_COMPRESSOR.compress(bytes), baos);
    return baos.toByteArray();
  }

  private static byte[] decompress(byte[] bytes) throws IOException {
    int length = ByteBuffer.wrap(bytes).getInt();
    return LZ_4_DECOMPRESSOR.decompress(bytes, 4, length);
  }

  private File infoFile() {
    File chunkDir = new File(settings.getSavePath(), settings.getWorldName());
    chunkDir.mkdirs();
    return new File(chunkDir, "info");
  }
  
  private File chunkFile(Vector3i position) {
    File chunkDir = new File(settings.getSavePath(), settings.getWorldName());
    chunkDir.mkdirs();
    return new File(chunkDir, "ch#" + position.x + "#" + position.y + "#" + position.z);
  }
}
