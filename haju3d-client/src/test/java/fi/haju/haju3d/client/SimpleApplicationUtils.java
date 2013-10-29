package fi.haju.haju3d.client;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

public class SimpleApplicationUtils {
  public static AppSettings configureSimpleApplication(SimpleApplication app) {
    AppSettings settings = new AppSettings(true);
    settings.setResolution(1024, 768);
    settings.setVSync(true);
    settings.setAudioRenderer(null);
    settings.setFullscreen(false);
    app.setSettings(settings);
    app.setShowSettings(false);
    app.setDisplayStatView(false);
    return settings;
  }
}
