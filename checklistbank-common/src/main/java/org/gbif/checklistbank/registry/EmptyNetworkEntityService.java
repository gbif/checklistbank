package org.gbif.checklistbank.registry;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.common.collect.Maps;

/**
 * A simple read only implementation of a NetworkEntityService backed by a CSV file.
 */
public class EmptyNetworkEntityService<T extends NetworkEntity> implements NetworkEntityService<T> {

  @Override
  public UUID create(@NotNull T entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(@NotNull UUID key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T get(@NotNull UUID key) {
    return null;
  }

  @Override
  public Map<UUID, String> getTitles(Collection<UUID> keys) {
    return Maps.newHashMap();
  }

  @Override
  public PagingResponse<T> list(@Nullable Pageable page) {
    return new PagingResponse<T>();
  }

  @Override
  public PagingResponse<T> search(String query, @Nullable Pageable page) {
    return new PagingResponse<T>();
  }

  @Override
  public PagingResponse<T> listByIdentifier(IdentifierType type, String identifier, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PagingResponse<T> listByIdentifier(String identifier, @Nullable Pageable page) {
    return new PagingResponse<T>();
  }

  @Override
  public void update(@NotNull T entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addComment(@NotNull UUID targetEntityKey, @NotNull Comment comment) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteComment(@NotNull UUID targetEntityKey, int commentKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Comment> listComments(@NotNull UUID targetEntityKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addContact(@NotNull UUID targetEntityKey, @NotNull Contact contact) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteContact(@NotNull UUID targetEntityKey, int contactKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Contact> listContacts(@NotNull UUID targetEntityKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateContact(@NotNull UUID targetEntityKey, @NotNull Contact contact) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addEndpoint(@NotNull UUID targetEntityKey, @NotNull Endpoint endpoint) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteEndpoint(@NotNull UUID targetEntityKey, int endpointKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Endpoint> listEndpoints(@NotNull UUID targetEntityKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addIdentifier(@NotNull UUID targetEntityKey, @NotNull Identifier identifier) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteIdentifier(@NotNull UUID targetEntityKey, int identifierKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID targetEntityKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addMachineTag(@NotNull UUID targetEntityKey, @NotNull MachineTag machineTag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addMachineTag(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name, @NotNull String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteMachineTag(@NotNull UUID targetEntityKey, int machineTagKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MachineTag> listMachineTags(@NotNull UUID targetEntityKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addTag(@NotNull UUID targetEntityKey, @NotNull String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int addTag(@NotNull UUID targetEntityKey, @NotNull Tag tag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteTag(@NotNull UUID taggedEntityKey, int tagKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Tag> listTags(@NotNull UUID taggedEntityKey, @Nullable String owner) {
    throw new UnsupportedOperationException();
  }
}
