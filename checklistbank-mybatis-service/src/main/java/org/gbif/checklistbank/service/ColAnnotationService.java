package org.gbif.checklistbank.service;

import org.gbif.checklistbank.service.mybatis.model.ColAnnotation;

/**
 * Service that deals with nub usage annotations from the Catalogue of Life GSD pipeline project (i4Life).
 * See:
 * <ul>
 *   <li><a href="http://dev.gbif.org/issues/browse/CLB-248">http://dev.gbif.org/issues/browse/CLB-248</a></li>
 *   <li><a href="http://www.catalogueoflife.org/piping_devel/webservice/gbp/GBIF/">i4Life piping services</a></li>
 * </ul>
 */
public interface ColAnnotationService {

  /**
   * Creates an annotation, overwriting any existing annotation with the same taxonKey.
   */
  void insertAnnotation(ColAnnotation annotation);
}
