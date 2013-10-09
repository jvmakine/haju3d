package fi.haju.haju3d.client;

import java.util.List;

import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.client.ui.ChunkSpatial;
import fi.haju.haju3d.client.ui.WorldManager;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.interaction.WorldEdit;
import fi.haju.haju3d.protocol.world.TilePosition;

public class ClientImpl implements Client {

  private final ChunkRenderer app;
  
  public ClientImpl(ChunkRenderer app) {
    this.app = app;
  }

  @Override
  public void registerWorldEdits(List<WorldEdit> edits) {
    for(WorldEdit edit : edits) {
      TilePosition tile = edit.getPosition();
      WorldManager worldManager = app.getWorldManager();
      ChunkSpatial chunkSpatial = worldManager.getChunkSpatial(tile.getChunkPosition());
      int x = tile.getTileWithinChunk().x;
      int y = tile.getTileWithinChunk().y;
      int z = tile.getTileWithinChunk().z;
      // TODO: Handle multiple edits to same chunk simultaneously
      chunkSpatial.chunk.set(x, y, z, edit.getNewTile());
      worldManager.rebuildChunkSpatial(chunkSpatial);
      // Update also the bordering chunks if necessary
      if(x < ChunkSpatialBuilder.SMOOTH_BUFFER) {
        worldManager.rebuildChunkSpatial(worldManager.getChunkSpatial(tile.getChunkPosition().add(-1, 0, 0)));
      }
      if(x >= worldManager.getChunkSize() - ChunkSpatialBuilder.SMOOTH_BUFFER) {
        worldManager.rebuildChunkSpatial(worldManager.getChunkSpatial(tile.getChunkPosition().add(1, 0, 0)));
      }
      if(y < ChunkSpatialBuilder.SMOOTH_BUFFER) {
        worldManager.rebuildChunkSpatial(worldManager.getChunkSpatial(tile.getChunkPosition().add(0, -1, 0)));
      }
      if(y >= worldManager.getChunkSize() - ChunkSpatialBuilder.SMOOTH_BUFFER) {
        worldManager.rebuildChunkSpatial(worldManager.getChunkSpatial(tile.getChunkPosition().add(0, 1, 0)));
      }
      if(z < ChunkSpatialBuilder.SMOOTH_BUFFER) {
        worldManager.rebuildChunkSpatial(worldManager.getChunkSpatial(tile.getChunkPosition().add(0, 0, -1)));
      }
      if(z >= worldManager.getChunkSize() - ChunkSpatialBuilder.SMOOTH_BUFFER) {
        worldManager.rebuildChunkSpatial(worldManager.getChunkSpatial(tile.getChunkPosition().add(0, 0, 1)));
      }
    }
  }

}
