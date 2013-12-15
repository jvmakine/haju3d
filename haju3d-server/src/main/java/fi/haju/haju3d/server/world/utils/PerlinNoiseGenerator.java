package fi.haju.haju3d.server.world.utils;

import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;
import com.jme3.math.Vector3f;

import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.FloatArray3d;
import fi.haju.haju3d.util.noise.InterpolationUtil;

public final class PerlinNoiseGenerator {
  private static final float LEVEL_AMPLITUDE_MULTIPLIER = 5.0f;
  private final int numberOfLevels;
  private final int baseMapSize;
  private final Random random;
  private final Map<Integer, NoiseLevel> levels = Maps.newHashMap(); 
  
  private final class NoiseLevel {
    public final int size;
    public final float amplitude;
    public final Map<Vector3i, FloatArray3d> data = Maps.newHashMap();
    
    public NoiseLevel(int size, float amplitude) {
      this.size = size;
      this.amplitude = amplitude;
    }
    
    public float getValueAt(Vector3f pos) {
      Vector3i posi = new Vector3i((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
      return InterpolationUtil.interpolateLinear3d(pos.x - posi.x, pos.y - posi.y, pos.z - posi.z,
          getValueAt(posi),
          getValueAt(posi.add(1,0,0)),
          getValueAt(posi.add(0,1,0)),
          getValueAt(posi.add(1,1,0)),
          getValueAt(posi.add(0,0,1)),
          getValueAt(posi.add(1,0,1)),
          getValueAt(posi.add(0,1,1)),
          getValueAt(posi.add(1,1,1))
      );
    }
    
    private float getValueAt(Vector3i pos) {
      Vector3i mapPos = new Vector3i((int)Math.floor(pos.x/(float)size), (int)Math.floor(pos.y/(float)size), (int)Math.floor(pos.z/(float)size));
      makeIfDoesNotExist(mapPos);
      return amplitude * data.get(mapPos).get(pos.x - mapPos.x*size, pos.y - mapPos.y*size, pos.z - mapPos.z*size);
    }
    
    private void makeIfDoesNotExist(Vector3i pos) {
      if(data.containsKey(pos)) return;
      data.put(pos, new FloatArray3d(size, size, size, new FloatArray3d.Initializer() {        
        @Override
        public float getValue(int x, int y, int z) {
          return random.nextFloat() * 2.0f - 1.0f;
        }
      }));
    }
    
  }
  
  public PerlinNoiseGenerator(int levels, int baseMapSize, Random random) {
    this.numberOfLevels = levels;
    this.baseMapSize = baseMapSize;
    this.random = random;
  }
  
  private Vector3f div(Vector3i v, float divisor) {
    return new Vector3f(v.x / divisor, v.y/divisor, v.z/divisor);
  }
    
  public float getValueAt(Vector3i pos) {
    float value = 0.0f;
    int size = baseMapSize;
    for(int level = 1; level <= numberOfLevels; ++level) {
      NoiseLevel noise = levels.get(level);
      if(noise == null) {
        noise = new NoiseLevel(2, level*LEVEL_AMPLITUDE_MULTIPLIER);
        levels.put(level, noise);
      }
      value += noise.getValueAt(div(pos, size));
      size *= 2;
    }
    return value;
  }
  
  public float getMaxValue() {
    return (float)Math.pow(LEVEL_AMPLITUDE_MULTIPLIER, numberOfLevels);
  }
  
  public float getMinValue() {
    return getMaxValue() * -1;
  }
  
}
