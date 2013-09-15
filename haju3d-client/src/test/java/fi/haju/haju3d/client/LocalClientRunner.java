package fi.haju.haju3d.client;

import java.rmi.RemoteException;

import com.jme3.system.AppSettings;

import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.server.ServerImpl;

public class LocalClientRunner {
  public static void main(String[] args) throws RemoteException {
    Client client = new ClientImpl();
    Server server = new ServerImpl();
    server.login(client);
    
    ChunkRenderer app = new ChunkRenderer(server.getChunk(new Vector3i(0, 0, 0)));
    app.setUseVertexColor(false);
    AppSettings settings = new AppSettings(true);
    settings.setVSync(true);
    settings.setAudioRenderer(null);
    settings.setFullscreen(false);
    app.setSettings(settings);
    app.setShowSettings(false);
    app.start();
  }
}
