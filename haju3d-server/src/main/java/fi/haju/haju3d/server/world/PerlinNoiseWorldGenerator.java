package fi.haju.haju3d.server.world;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.server.world.utils.FloodFiller;
import fi.haju.haju3d.server.world.utils.PerlinNoiseGenerator;
import fi.haju.haju3d.server.world.utils.WorldGenerationUtils;
import fi.haju.haju3d.util.Profiled;

import java.util.Random;

public class PerlinNoiseWorldGenerator implements WorldGenerator {
  
  private int seed;
  private PerlinNoiseGenerator generator;

  @Override
  @Profiled
  public Chunk generateChunk(ChunkPosition position, int size) {
    int realseed = seed ^ (position.x + position.y * 123 + position.z * 12347);
    Chunk chunk = new Chunk(size, realseed, position);
    return makeChunk(chunk, realseed, position);
  }

  @Override
  public void setSeed(int seed) {
    this.seed = seed;
    //TODO : Proper initialization
    this.generator = new PerlinNoiseGenerator(5, 8, new Random(seed));
  }

  private static Chunk filterFloaters(Chunk chunk) {
    Chunk ground = new Chunk(chunk.getSize(), chunk.getSeed(), chunk.getPosition());
    new FloodFiller(ground, chunk).fill();
    return ground;
  }

  private Chunk makeChunk(Chunk chunk, int seed, ChunkPosition position) {
    int size = chunk.getSize();
    float thres = 32;
    boolean onlyAir = true;
    if(generator.getMinValue() + position.y*size > thres) {
      return new Chunk(size, seed, position, Tile.AIR);
    }
    for (int x = 0; x < size; x++) {
      for (int y = 0; y < size; y++) {
        for (int z = 0; z < size; z++) {
          int rx = x + position.x*size;
          int ry = y + position.y*size;
          int rz = z + position.z*size;
          float v = ry;
          v += generator.getValueAt(new Vector3i(rx, ry, rz));
          // TODO Type from noise
          Tile tile = v < thres ? Tile.GROUND : Tile.AIR;
          if(tile != Tile.AIR) {
            onlyAir = false;
          }
          chunk.set(x, y, z, tile);
          // TODO Color from noise
          float col = 1.0f;
          chunk.setColor(x, y, z, col);
        }
      }
    }
    if(onlyAir) {
      return new Chunk(size, seed, position, Tile.AIR);
    }
    Random r = new Random(seed);
    // add trees
    generateTrees(chunk, r);
    return filterFloaters(chunk);
  }

  private void generateTrees(Chunk chunk, Random r) {
    int size = chunk.getSize();
    for (int i = 0; i < 4; ++i) {
      int x = r.nextInt(size - 3);
      int z = r.nextInt(size - 3);
      int y = WorldGenerationUtils.findGround(chunk, size, x, z);
      if (y >= 1 && y < size - 20 && chunk.get(x, y - 1, z) == Tile.GROUND) {
        WorldGenerationUtils.makeTreeAt(chunk, r, new LocalTilePosition(x, z, y));
      }
    }
  }

}
