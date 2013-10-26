package fi.haju.haju3d.server;

import com.google.common.base.Optional;
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
import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class WorldSaver {
  private static final LZ4Factory LZ_4_FACTORY = LZ4Factory.fastestInstance();
  private static final LZ4Compressor LZ_4_COMPRESSOR = LZ_4_FACTORY.fastCompressor();
  private static final LZ4FastDecompressor LZ_4_DECOMPRESSOR = LZ_4_FACTORY.fastDecompressor();

  private static final long MIN_SAVE_INTERVAL = 30000;
  private static final Logger LOGGER = LoggerFactory.getLogger(WorldSaver.class);

  private Set<Vector3i> chunksToSave = Collections.newSetFromMap(new ConcurrentHashMap<Vector3i,Boolean>());
  private Timer timer = new Timer();

  @Inject
  private ServerSettings settings;

  public void saveChunk(final Chunk chunk) {
    if (!chunksToSave.contains(chunk.getPosition())) {
      chunksToSave.add(chunk.getPosition());
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          LOGGER.info("Saving chunk : " + chunk.getPosition());
          writeObjectToFile(chunkFile(chunk.getPosition()), chunk);
          chunksToSave.remove(chunk.getPosition());
        }
      }, MIN_SAVE_INTERVAL);
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
