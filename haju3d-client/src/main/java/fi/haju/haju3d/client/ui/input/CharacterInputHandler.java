package fi.haju.haju3d.client.ui.input;

import java.util.HashSet;
import java.util.Set;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import fi.haju.haju3d.client.Character;
import fi.haju.haju3d.client.TilePosition;
import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.client.ui.WorldManager;

public class CharacterInputHandler {
  
  public static final float MOUSE_X_SPEED = 3.0f;
  public static final float MOUSE_Y_SPEED = MOUSE_X_SPEED;
  
  private final Character character;
  private final WorldManager worldManager;
  private final ChunkRenderer renderer;
  private final Set<String> activeInputs = new HashSet<>();
  
  public CharacterInputHandler(Character character, WorldManager worldManager, ChunkRenderer renderer) {
    this.character = character;
    this.worldManager = worldManager;
    this.renderer = renderer;
  }

  public void register(final InputManager inputManager) {
    // moving
    inputManager.addMapping(InputActions.STRAFE_LEFT, new KeyTrigger(KeyInput.KEY_A));
    inputManager.addMapping(InputActions.STRAFE_RIGHT, new KeyTrigger(KeyInput.KEY_D));
    inputManager.addMapping(InputActions.WALK_FORWARD, new KeyTrigger(KeyInput.KEY_W));
    inputManager.addMapping(InputActions.WALK_BACKWARD, new KeyTrigger(KeyInput.KEY_S));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          activeInputs.add(name);
        } else {
          activeInputs.remove(name);
        }
      }
    }, InputActions.STRAFE_LEFT, InputActions.STRAFE_RIGHT, InputActions.WALK_FORWARD, InputActions.WALK_BACKWARD);

    // looking
    inputManager.addMapping(InputActions.LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true), new KeyTrigger(KeyInput.KEY_LEFT));
    inputManager.addMapping(InputActions.LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false), new KeyTrigger(KeyInput.KEY_RIGHT));
    inputManager.addMapping(InputActions.LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false), new KeyTrigger(KeyInput.KEY_UP));
    inputManager.addMapping(InputActions.LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true), new KeyTrigger(KeyInput.KEY_DOWN));

    inputManager.addListener(new AnalogListener() {
      @Override
      public void onAnalog(String name, float value, float tpf) {
        if (name.equals(InputActions.LOOK_LEFT)) {
          character.setLookAzimuth(character.getLookAzimuth() + value * MOUSE_X_SPEED);
        } else if (name.equals(InputActions.LOOK_RIGHT)) {
          character.setLookAzimuth(character.getLookAzimuth() - value * MOUSE_X_SPEED);
        } else if (name.equals(InputActions.LOOK_UP)) {
          character.setLookElevation(character.getLookElevation() - value * MOUSE_Y_SPEED);
        } else if (name.equals(InputActions.LOOK_DOWN)) {
          character.setLookElevation(character.getLookElevation() + value * MOUSE_Y_SPEED);
        }
      }
    }, InputActions.LOOK_LEFT, InputActions.LOOK_RIGHT, InputActions.LOOK_UP, InputActions.LOOK_DOWN);

    // jumping
    inputManager.addMapping(InputActions.JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed && canJump(character.getNode())) {
          character.setVelocity(character.getVelocity().add(new Vector3f(0.0f, 0.5f, 0.0f)));
        }
      }
    }, InputActions.JUMP);

    // toggle flycam
    inputManager.addMapping(InputActions.TOGGLE_FLYCAM, new KeyTrigger(KeyInput.KEY_RETURN));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          renderer.toggleFlyCam();
        }
      }
    }, InputActions.TOGGLE_FLYCAM);

    // toggle fullscreen
    inputManager.addMapping(InputActions.CHANGE_FULL_SCREEN, new KeyTrigger(KeyInput.KEY_F));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean keyPressed, float tpf) {
        renderer.toggleFullScreen();
      }
    }, InputActions.CHANGE_FULL_SCREEN);
    
    // Dig
    inputManager.addMapping(InputActions.BUILD, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean keyPressed, float tpf) {
        if(keyPressed) {
          TilePosition tile = renderer.getSelectedTile();
          if(tile != null) {
            System.out.println("Dig: " + tile);
          }
        }
      }
    }, InputActions.BUILD);
  }
  
  private boolean canJump(Node characterNode) {
    Vector3f pos = characterNode.getLocalTranslation();
    return worldManager.getTerrainCollisionPoint(pos, pos.add(new Vector3f(0, -2.0f, 0)), 0.0f) != null;
  }

  public Set<String> getActiveInputs() {
    return activeInputs;
  }
  
}
