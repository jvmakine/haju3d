package fi.haju.haju3d.server;

import com.google.inject.Singleton;

import fi.haju.haju3d.util.SettingsManager;

@Singleton
public class ServerSettings extends SettingsManager {

  private static final String WORLD_NAME_KEY = "worldName";
  
  private static final String CONFIG_FILE_NAME = "haju3d-server.properties";

  private String worldName;
  
  @Override
  protected void loadSettings() {
    worldName = loadString(WORLD_NAME_KEY, "default");
  }
  
  @Override
  protected String getPropertiesFileName() {
    return CONFIG_FILE_NAME;
  }

  public String getWorldName() {
    return worldName;
  }

}
