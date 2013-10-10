package fi.haju.haju3d.protocol;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import fi.haju.haju3d.protocol.interaction.WorldEdit;
import fi.haju.haju3d.protocol.world.Chunk;

public interface Server extends Remote {
  void login(Client client) throws RemoteException;
  void logout(Client client) throws RemoteException;
  
  Chunk getChunk(Vector3i position) throws RemoteException;
  List<Chunk> getChunks(Collection<Vector3i> positions) throws RemoteException;
  
  void registerWorldEdits(List<WorldEdit> edits) throws RemoteException;
  void disconnect(Client client) throws RemoteException;
}
