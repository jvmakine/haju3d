package fi.haju.haju3d.protocol;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.interaction.WorldEdit;
import fi.haju.haju3d.protocol.world.Chunk;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

public interface Server extends Remote {
  void login(Client client) throws RemoteException;

  void logout(Client client) throws RemoteException;

  Chunk getChunk(ChunkPosition position) throws RemoteException;

  List<Chunk> getChunks(Collection<ChunkPosition> positions) throws RemoteException;

  void registerWorldEdits(List<WorldEdit> edits) throws RemoteException;

  void disconnect(Client client) throws RemoteException;
}
