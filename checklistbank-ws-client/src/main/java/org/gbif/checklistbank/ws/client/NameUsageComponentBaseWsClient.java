package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.NameUsageComponentService;
import org.gbif.checklistbank.ws.util.Constants;
import org.gbif.ws.client.BaseWsGetClient;

import javax.annotation.Nullable;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Base client with generic return type GenericType<PagingResponse<T>> representing the model object
 * and WebResource attributes.
 * The client must extend this class by defining the type T expected in its PagingResponse.
 *
 * @param <T> the (interpreted) model class
 */
abstract class NameUsageComponentBaseWsClient<T> extends BaseWsGetClient<T, Integer>
  implements NameUsageComponentService<T> {

  // used to tell Jersey client api to return an instance of List<ChecklistUsage>
  protected final GenericType<PagingResponse<T>> tPage;
  private final String mainPath;
  private final String subResourcePath;

  /**
   * @param resourceClass   the interpreted name usage component class
   * @param tPage           the generic type of a paging response for the interpreted class
   * @param resource        the web resource for the checklistbank ws resource
   * @param subResourcePath the path to the subresource of name usage
   * @param mainPath        the path to the standalone path of the component
   */
  NameUsageComponentBaseWsClient(Class<T> resourceClass, GenericType<PagingResponse<T>> tPage, WebResource resource,
    String subResourcePath, String mainPath) {
    super(resourceClass, resource, null);
    this.tPage = tPage;
    this.subResourcePath = subResourcePath;
    this.mainPath = mainPath;
  }

  /**
   * Returns a component.
   * The object returned is the interpreted version of it.
   *
   * @return A component
   */
  @Override
  public T get(int key) {
    return get(mainPath, String.valueOf(key));
  }

  /**
   * Returns all components for a name usage.
   * This calls the URL {@code /species/<k1>/XYZ}.
   *
   * @param usageKey the ChecklistUsage the Description are related to
   * @param page     paging parameters or null for first page with default size.
   *
   * @return Wrapper that contains a potentially empty list, but never null.
   */
  @Override
  public PagingResponse<T> listByUsage(int usageKey, @Nullable Pageable page) {
    return get(tPage, page, Constants.SPECIES_PATH, String.valueOf(usageKey), subResourcePath);
  }

}
