package fi.haju.haju3d.server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.util.noise.PerlinNoiseUtil;

public class ServerImpl implements Server {
  
  private static final int WIDTH = 120;
  private static final int HEIGHT = 120;
  private static final int DEPTH = 120;
  
  private Chunk chunk = makeChunk();

  List<Client> loggedInClients = Lists.newArrayList(); 
  
  @Override
  public synchronized void login(Client client) {
    loggedInClients.add(client);
  }

  @Override
  public synchronized void logout(Client client) {
    loggedInClients.remove(client);
  }
  
  @Override
  public Chunk getChunk() throws RemoteException {
    return chunk;
  }
  
  private static class FloodFill {
    private List<Vector3i> front = new ArrayList<>();
    private Set<Vector3i> visited = new HashSet<>();
    private Chunk ground;
    private Chunk orig;

    public FloodFill(Chunk ground, Chunk orig) {
      this.ground = ground;
      this.orig = orig;
    }

    public void fill() {
      test(new Vector3i(0, 0, 0));
      while (!front.isEmpty()) {
        Vector3i v = front.remove(front.size() - 1);
        test(v.add(1, 0, 0));
        test(v.add(-1, 0, 0));
        test(v.add(0, 1, 0));
        test(v.add(0, -1, 0));
        test(v.add(0, 0, 1));
        test(v.add(0, 0, -1));
      }
    }

    private void test(Vector3i n) {
      if (visited.contains(n)) {
        return;
      }
      if (orig.get(n.x, n.y, n.z) == Tile.AIR) {
        return;
      }
      ground.set(n.x, n.y, n.z, Tile.GROUND);
      visited.add(n);
      front.add(n);
    }
  }
  
  private static Chunk filterFloaters(Chunk grid) {
    Chunk ground = new Chunk(grid.getWidth(), grid.getHeight(), grid.getDepth());
    new FloodFill(ground, grid).fill();
    return ground;
  }
  
  private Chunk makeChunk() {
    Chunk grid = new Chunk(WIDTH, HEIGHT, DEPTH);
    int w = grid.getWidth();
    int h = grid.getHeight();
    int d = grid.getDepth();

    float[] noise = PerlinNoiseUtil.make3dPerlinNoise(12, w, h, d);
    float thres = h / 3;
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < d; z++) {
          float v = Math.abs(y);
          if (y < h / 5) {
            v += PerlinNoiseUtil.interpolateLinear(y / (h / 5), -10, 0);
          }
          v += noise[x + y * w + z * w * h] * 3;
          grid.set(x, y, z, v < thres ? Tile.GROUND : Tile.AIR);
        }
      }
    }
    return filterFloaters(grid);
  }

}
