package fi.haju.haju3d.server;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.server.world.PerlinNoiseWorldGenerator;
import fi.haju.haju3d.server.world.WorldGenerator;


public class ServerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Server.class).to(ServerImpl.class).in(Scopes.SINGLETON);
    bind(WorldGenerator.class).to(PerlinNoiseWorldGenerator.class).in(Scopes.SINGLETON);
  }
}
