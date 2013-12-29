package fi.haju.haju3d.server;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;

import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.server.world.PerlinNoiseWorldGenerator;
import fi.haju.haju3d.server.world.WorldGenerator;
import fi.haju.haju3d.util.ProfileInterceptor;
import fi.haju.haju3d.util.Profiled;


public class ServerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Server.class).to(ServerImpl.class).in(Scopes.SINGLETON);
    bind(WorldGenerator.class).to(PerlinNoiseWorldGenerator.class).in(Scopes.SINGLETON);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Profiled.class), new ProfileInterceptor());
  }
}
