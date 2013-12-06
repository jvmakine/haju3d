package fi.haju.haju3d.client;


import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

import fi.haju.haju3d.client.util.ProfileInterceptor;
import fi.haju.haju3d.client.util.Profiled;

public class ClientModule extends AbstractModule {

  @Override
  protected void configure() {
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Profiled.class), new ProfileInterceptor());
  }

}
