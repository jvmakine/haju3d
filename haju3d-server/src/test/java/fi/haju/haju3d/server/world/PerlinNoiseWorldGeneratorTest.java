package fi.haju.haju3d.server.world;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.world.Chunk;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertTrue;

public class PerlinNoiseWorldGeneratorTest {
  private static final LZ4Factory LZ_4_FACTORY = LZ4Factory.fastestInstance();
  private static final LZ4Compressor LZ_4_COMPRESSOR = LZ_4_FACTORY.fastCompressor();
  private static final LZ4Compressor LZ_4_COMPRESSOR2 = LZ_4_FACTORY.highCompressor();

  @Test
  public void testCompressionRatios() {
    Chunk chunk = new PerlinNoiseWorldGenerator().generateChunk(new ChunkPosition(0, 0, 0), 64);
    byte[] data = chunk.getTiles().getData();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int len = 0;
    byte prev = -1;
    for (byte b : data) {
      if (b != prev) {
        baos.write(len > 255 ? 255 : len); //didn't bother to handle longer runs yet
        baos.write(b);
        len = 0;
      }
      len++;
      prev = b;
    }

    System.out.println("ORIG=" + data.length);
    System.out.println("LZ4=" + LZ_4_COMPRESSOR.compress(data).length);
    System.out.println("LZ4SLOW=" + LZ_4_COMPRESSOR2.compress(data).length);
    System.out.println("RLE+LZ4=" + LZ_4_COMPRESSOR.compress(baos.toByteArray()).length);

    //1 chunk compressed is about 12KB and (32 m)^3
    //area of 640*640*160 m^3 takes 24 MB

    // verify at least 10x compression
    assertTrue(LZ_4_COMPRESSOR.compress(baos.toByteArray()).length * 10 < data.length);
  }
}
