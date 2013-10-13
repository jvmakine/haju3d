package fi.haju.haju3d.client;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import fi.haju.haju3d.client.connection.ServerConnector;
import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.client.ui.WorldManager;
import fi.haju.haju3d.client.ui.input.CharacterInputHandler;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;

public class ClientModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ServerConnector.class).in(Scopes.SINGLETON);
    bind(ChunkRenderer.class).in(Scopes.SINGLETON);
    bind(WorldManager.class).in(Scopes.SINGLETON);
    bind(ChunkSpatialBuilder.class).in(Scopes.SINGLETON);
    bind(CharacterInputHandler.class).in(Scopes.SINGLETON);
  }

}
