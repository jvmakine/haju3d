package fi.haju.haju3d.protocol;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import fi.haju.haju3d.protocol.interaction.WorldEdit;

public interface Client extends Remote {
  void registerWorldEdits(List<WorldEdit> edits) throws RemoteException;
}
