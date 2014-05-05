package fi.haju.haju3d.server.world;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.server.world.utils.FloodFiller;
import fi.haju.haju3d.server.world.utils.PerlinNoiseGenerator;
import fi.haju.haju3d.util.Profiled;

public class PerlinNoiseWorldGenerator implements WorldGenerator {
  
  private static final int TERRAIN_FEATURE_SIZE = 7;
  private static final int TERRAIN_SMOOTHNESS = 3;
  private static final int TERRAIN_THRESHOLD = 64;
  
  private int seed;
  private PerlinNoiseGenerator generator;
  private PerlinNoiseGenerator typeGenerator;

  @Override
  @Profiled
  public Chunk generateChunk(ChunkPosition position, int sizeLog2) {
    int realseed = seed ^ (position.x + position.y * 123 + position.z * 12347);
    return makeChunk(sizeLog2, realseed, position);
  }

  @Override
  public void setSeed(int seed) {
    this.seed = seed;
    this.generator = new PerlinNoiseGenerator(TERRAIN_FEATURE_SIZE, TERRAIN_SMOOTHNESS, seed);
    this.typeGenerator = new PerlinNoiseGenerator(2, TERRAIN_SMOOTHNESS, seed);
  }

  private static Chunk filterFloaters(Chunk chunk) {
    Chunk ground = new Chunk(chunk.getSize(), chunk.getSeed(), chunk.getPosition());
    new FloodFiller(ground, chunk).fill();
    return ground;
  }

  private Chunk makeChunk(int sizeLog2, int seed, ChunkPosition position) {
    boolean onlyAir = true;
    int size = 1 << sizeLog2;
    Vector3i wp = position.mult(size);
    float minVal = generator.getMinValue(wp, size); 
    if(minVal + position.y*(size + 1) > TERRAIN_THRESHOLD) {
      return new Chunk(size, seed, position, Tile.AIR);
    }
    float maxVal = generator.getMaxValue(wp, size);
    if(maxVal + position.y*(size) < TERRAIN_THRESHOLD) {
      return new Chunk(size, seed, position, Tile.ROCK);
    }
    Chunk chunk = new Chunk(size, seed, position);
    int sx = (position.x << sizeLog2);
    int sy = (position.y << sizeLog2);
    int sz = (position.z << sizeLog2);
    for (int x = 0; x < size; x++) {
      int rx = x + sx;
      for (int y = 0; y < size; y++) {
        int ry = y + sy;
        for (int z = 0; z < size; z++) {
          int rz = z + sz;
          float v = ry + generator.getValueAt(rx, ry, rz);
          float tv = typeGenerator.getValueAt(rx, ry, rz);
          Tile tile = v < TERRAIN_THRESHOLD ? getGround(tv) : Tile.AIR;
          if(tile != Tile.AIR) {
            onlyAir = false;
          }
          chunk.set(x, y, z, tile);
          // TODO Color from noise
        }
      }
    }
    if(onlyAir) {
      return new Chunk(size, seed, position, Tile.AIR);
    }
    chunk = filterFloaters(chunk);
    return chunk;
  }
  
  private final static Tile getGround(float v) {
    return v < 0 ? Tile.GROUND : Tile.ROCK;
  }

}
