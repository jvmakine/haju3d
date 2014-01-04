package fi.haju.haju3d.client;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.LogManager;

public final class SimpleApplicationUtils {
  private SimpleApplicationUtils() {
  }

  public static AppSettings configureSimpleApplication(SimpleApplication app) {
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.install();

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

  public static void addLights(SimpleApplication app) {
    Node rootNode = app.getRootNode();
    DirectionalLight light = new DirectionalLight();
    light.setDirection(new Vector3f(-1, -2, -0.5f).normalizeLocal());
    light.setColor(new ColorRGBA(1f, 1f, 1f, 1f).mult(.8f));
    rootNode.addLight(light);

    DirectionalLight light2 = new DirectionalLight();
    light2.setDirection(new Vector3f(1, -2, 3).normalizeLocal());
    light2.setColor(new ColorRGBA(0.4f, 0.4f, 1f, 1f).mult(1.0f));
    rootNode.addLight(light2);

    DirectionalLight light3 = new DirectionalLight();
    light3.setDirection(new Vector3f(-4, 2, 2).normalizeLocal());
    light3.setColor(new ColorRGBA(0.6f, 0.2f, 0.3f, 1f).mult(1.0f));
    rootNode.addLight(light3);

    AmbientLight ambient = new AmbientLight();
    ambient.setColor(ColorRGBA.Blue.mult(0.4f));
    rootNode.addLight(ambient);

    DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(app.getAssetManager(), 2048, 4);
    dlsr.setLight(light);
    dlsr.setShadowIntensity(0.4f);
    dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
    app.getViewPort().addProcessor(dlsr);
  }

  public static void addCartoonEdges(SimpleApplication app) {
    CartoonEdgeFilter rimLightFilter = new CartoonEdgeFilter();
    rimLightFilter.setEdgeColor(ColorRGBA.Black);

    rimLightFilter.setEdgeIntensity(0.5f);
    rimLightFilter.setEdgeWidth(1.0f);

    rimLightFilter.setNormalSensitivity(0.0f);
    rimLightFilter.setNormalThreshold(0.0f);

    rimLightFilter.setDepthSensitivity(20.0f);
    rimLightFilter.setDepthThreshold(0.0f);
    FilterPostProcessor fpp = new FilterPostProcessor(app.getAssetManager());
    fpp.addFilter(rimLightFilter);
    app.getViewPort().addProcessor(fpp);
  }

  public static void setupCrosshair(SimpleApplication app, AppSettings settings) {
    BitmapFont guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
    BitmapText crossHair = new BitmapText(guiFont, false);
    crossHair.setSize(guiFont.getCharSet().getRenderedSize() * 2);
    crossHair.setText("+");
    crossHair.setLocalTranslation(
        settings.getWidth() / 2 - crossHair.getLineWidth() / 2,
        settings.getHeight() / 2 + crossHair.getLineHeight() / 2, 0);
    app.getGuiNode().attachChild(crossHair);
  }

  public static Material makeColorMaterial(AssetManager assetManager, ColorRGBA color) {
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    return mat;
  }

  public static Material makeLineMaterial(AssetManager assetManager, ColorRGBA color) {
    Material matVC = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    matVC.setColor("Color", color);
    return matVC;
  }
}
