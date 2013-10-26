package fi.haju.haju3d.client;

import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.interaction.WorldEdit;

import java.util.List;

public class ClientImpl implements Client {

  private final ChunkRenderer app;

  public ClientImpl(ChunkRenderer app) {
    this.app = app;
  }

  @Override
  public void registerWorldEdits(List<WorldEdit> edits) {
    app.getWorldManager().registerWorldEdits(edits);
  }

}
