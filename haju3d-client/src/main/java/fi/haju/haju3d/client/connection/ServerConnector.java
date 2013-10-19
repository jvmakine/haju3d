package fi.haju.haju3d.client.connection;

import com.google.inject.Singleton;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.interaction.WorldEdit;
import fi.haju.haju3d.protocol.world.Chunk;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.List;

@Singleton
public class ServerConnector implements Server {

  private Server remoteServer;

  public ServerConnector() {
  }

  public void setRemoteServer(Server server) {
    this.remoteServer = server;
  }

  public void connect() {
    try {
      Registry registry = LocateRegistry.getRegistry(5250);
      remoteServer = (Server) registry.lookup("haju3d_server");
    } catch (RemoteException e) {
      throw connectionError(e);
    } catch (NotBoundException e) {
      throw connectionError(e);
    }
  }

  @Override
  public void login(Client client) {
    try {
      remoteServer.login(client);
    } catch (RemoteException e) {
      throw connectionError(e);
    }
  }

  @Override
  public void logout(Client client) {
    try {
      remoteServer.logout(client);
    } catch (RemoteException e) {
      throw connectionError(e);
    }
  }

  @Override
  public Chunk getChunk(Vector3i position) {
    try {
      return remoteServer.getChunk(position);
    } catch (RemoteException e) {
      throw connectionError(e);
    }
  }

  @Override
  public List<Chunk> getChunks(Collection<Vector3i> positions) {
    try {
      return remoteServer.getChunks(positions);
    } catch (RemoteException e) {
      throw connectionError(e);
    }
  }

  @Override
  public void registerWorldEdits(List<WorldEdit> edits) {
    try {
      remoteServer.registerWorldEdits(edits);
    } catch (RemoteException e) {
      throw connectionError(e);
    }

  }

  @Override
  public void disconnect(Client client) {
    try {
      remoteServer.disconnect(client);
    } catch (RemoteException e) {
      throw connectionError(e);
    }
  }

  private RuntimeException connectionError(Exception e) {
    return new RuntimeException(e);
  }

}
