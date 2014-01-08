package org.gbif.checklistbank.index;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.MetadataType;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Mocked dataset service implementing only the get by uuid method and always
 * returning a checklist dataset for any UUID.
 */
public class DatasetServiceMock implements DatasetService {

  @Override
  public Dataset get(UUID key) {
    Dataset ds = new Dataset();
    ds.setKey(key);
    ds.setType(DatasetType.CHECKLIST);
    ds.setTitle("Mock dataset " + key);
    return ds;
  }

  @Override
  public UUID create(@NotNull Dataset dataset) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void delete(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> list(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> search(String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> listByIdentifier(IdentifierType identifierType, String s,
    @Nullable Pageable pageable) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> listByIdentifier(String s, @Nullable Pageable pageable) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void update(@NotNull Dataset dataset) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> listConstituents(UUID uuid, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> listByCountry(@NotNull Country country, @Nullable DatasetType datasetType,
    @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> listByType(DatasetType datasetType, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<Metadata> listMetadata(UUID uuid, @Nullable MetadataType type) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Metadata getMetadata(int i) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteMetadata(int i) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Metadata insertMetadata(UUID uuid, InputStream inputStream) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public InputStream getMetadataDocument(UUID uuid) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public InputStream getMetadataDocument(int i) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> listDeleted(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> listDuplicates(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> listDatasetsWithNoEndpoint(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public int addComment(@NotNull UUID uuid, @NotNull Comment comment) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteComment(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<Comment> listComments(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public int addContact(@NotNull UUID uuid, @NotNull Contact contact) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteContact(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<Contact> listContacts(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void updateContact(@NotNull UUID uuid, @NotNull Contact contact) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public int addEndpoint(@NotNull UUID uuid, @NotNull Endpoint endpoint) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteEndpoint(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<Endpoint> listEndpoints(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull MachineTag machineTag) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public int addMachineTag(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name,
    @NotNull String value) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteMachineTag(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<MachineTag> listMachineTags(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String s) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public int addTag(@NotNull UUID targetEntityKey, @NotNull Tag tag) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<Network> listNetworks(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> listConstituents(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
