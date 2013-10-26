package fi.haju.haju3d.server;

import com.google.inject.Singleton;
import fi.haju.haju3d.util.SettingsManager;

import java.io.File;

@Singleton
public class ServerSettings extends SettingsManager {

  private static final String WORLD_NAME_KEY = "worldName";
  private static final String SAVE_PATH_KEY = "savePath";

  private static final String CONFIG_FILE_NAME = "haju3d-server.properties";

  private String worldName;
  private File savePath;

  @Override
  protected void loadSettings() {
    worldName = loadString(WORLD_NAME_KEY, "default");
    savePath = new File(loadString(SAVE_PATH_KEY, "./saves"));
  }

  @Override
  protected String getPropertiesFileName() {
    return CONFIG_FILE_NAME;
  }

  public String getWorldName() {
    return worldName;
  }

  public File getSavePath() {
    return savePath;
  }

}
