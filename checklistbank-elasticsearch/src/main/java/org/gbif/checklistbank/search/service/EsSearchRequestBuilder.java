/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.search.service;

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchConstants;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.common.search.EsFieldMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.GeoShapeRelation;
import co.elastic.clients.elasticsearch._types.ScoreSort;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoShapeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;

import static org.gbif.api.model.common.search.SearchRequest.QueryField;
import static org.gbif.api.util.SearchTypeValidator.isDateRange;
import static org.gbif.api.util.SearchTypeValidator.isNumericRange;
import static org.gbif.common.search.es.indexing.EsQueryUtils.LOWER_BOUND_RANGE_PARSER;
import static org.gbif.common.search.es.indexing.EsQueryUtils.RANGE_SEPARATOR;
import static org.gbif.common.search.es.indexing.EsQueryUtils.RANGE_WILDCARD;
import static org.gbif.common.search.es.indexing.EsQueryUtils.UPPER_BOUND_RANGE_PARSER;
import static org.gbif.common.search.es.indexing.EsQueryUtils.extractFacetLimit;
import static org.gbif.common.search.es.indexing.EsQueryUtils.extractFacetOffset;

/**
 * This class is a copy org.gbif.common.search.EsSearchRequestBuilder, copied to avoid gbif-api collisions.
 */
public class EsSearchRequestBuilder<P extends SearchParameter> {

  public static final String PRE_HL_TAG = "<em class=\"gbifHl\">";
  public static final String POST_HL_TAG = "</em>";
  private static final int MAX_SIZE_TERMS_AGGS = 1200000;
  private static final IntUnaryOperator DEFAULT_SHARD_SIZE = size -> (size * 2) + 50000;

  private EsFieldMapper<P> esFieldMapper;

  private final Highlight highlight;

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  public EsSearchRequestBuilder(EsFieldMapper<P> esFieldMapper) {
    this.esFieldMapper = esFieldMapper;
    this.highlight = highlight();
  }

  private Highlight highlight() {
    return new Highlight.Builder()
            .preTags(PRE_HL_TAG)
            .postTags(POST_HL_TAG)
            .encoder(HighlighterEncoder.Html)
            .type("unified")
            .requireFieldMatch(false)
            .numberOfFragments(0)
            .fields(
                esFieldMapper.highlightingFields().stream()
                    .map(field -> NamedValue.of(field, HighlightField.of(h -> h)))
                    .collect(Collectors.toList()))
            .build();
  }
  public SearchRequest buildSearchRequest(
    org.gbif.api.model.common.search.SearchRequest<P> searchRequest, String index) {

    SearchRequest.Builder esRequest = new SearchRequest.Builder();
    esRequest.index(index);
    esRequest.trackTotalHits(h -> h.enabled(true));
    esRequest.source(s -> s.filter(f -> f.excludes(esFieldMapper.excludeFields())
                                         .includes(esFieldMapper.getMappedFields())));

    // size and offset
    esRequest.size(searchRequest.getLimit());
    esRequest.from((int) searchRequest.getOffset());

    // sort
    if (Strings.isNullOrEmpty(searchRequest.getQ())) {
      esRequest.sort(esFieldMapper.sorts());
    } else {
      esRequest.sort(s -> s.score(new ScoreSort.Builder().build()));
      if (searchRequest.isHighlight()) {
        esRequest.highlight(highlight);
      }
    }

    // add query
    if (SearchConstants.QUERY_WILDCARD.equals(searchRequest.getQ())) { // Is a search all
      esRequest.query(q -> q.matchAll(new MatchAllQuery.Builder().build()));
    } else {
      buildQuery(searchRequest.getParameters(), searchRequest.getQ(), searchRequest.getQFields())
        .ifPresent(q -> esRequest.query(new Query.Builder().bool(q).build()));
    }

    return esRequest.build();
  }

  public SearchRequest buildFacetedSearchRequest(
      FacetedSearchRequest<P> searchRequest, boolean facetsEnabled, String index) {

    SearchRequest.Builder esRequest = new SearchRequest.Builder();
    esRequest.index(index);
    esRequest.trackTotalHits(v -> v.enabled(true));

    esRequest.source(s -> s.filter(f -> f.excludes(esFieldMapper.excludeFields())
      .includes(esFieldMapper.getMappedFields())));

    // size and offset
    esRequest.size(searchRequest.getLimit());
    esRequest.from((int) searchRequest.getOffset());

    // sort
    if (Strings.isNullOrEmpty(searchRequest.getQ())) {
      esRequest.sort(esFieldMapper.sorts());
    } else {
      esRequest.query( q ->  q.matchAll(new MatchAllQuery.Builder().build()));
      if (searchRequest.isHighlight()) {
        esRequest.highlight(highlight);
      }
    }

    // group params
    GroupedParams<P> groupedParams = groupParameters(searchRequest);

    // add query
    if (SearchConstants.QUERY_WILDCARD.equals(searchRequest.getQ())) { // Is a search all
      esRequest.query( q ->  q.matchAll(new MatchAllQuery.Builder().build()));
    } else {
      buildQuery(groupedParams.queryParams, searchRequest.getQ(), searchRequest.getQFields())
          .ifPresent(b -> esRequest.query(new Query.Builder().bool(b).build()));
    }

    // add aggs
    buildAggregations(searchRequest, groupedParams.postFilterParams, facetsEnabled)
        .ifPresent(esRequest::aggregations);

    // post-filter
    buildPostFilter(groupedParams.postFilterParams)
      .ifPresent(pf -> esRequest.postFilter(new Query.Builder()
                                              .bool(pf.build())
                                              .build()));

    return esRequest.build();
  }

  public Optional<BoolQuery> buildQueryNode(FacetedSearchRequest<P> searchRequest) {
    return buildQuery(searchRequest.getParameters(), searchRequest.getQ(), searchRequest.getQFields());
  }

  public SearchRequest buildAutocompleteQuery(
      org.gbif.api.model.common.search.SearchRequest<P> searchRequest, P parameter, String index) {
    Optional<BoolQuery> filterQuery = buildQuery(searchRequest.getParameters(), null, null);

    BoolQuery.Builder query = QueryBuilders.bool();

    if (!Strings.isNullOrEmpty(searchRequest.getQ())) {
      query.should(q -> q
                     .match(m -> m
                     .field(esFieldMapper.getAutocompleteField(parameter))
                     .query(searchRequest.getQ())
                     .operator(Operator.And)));
      if (searchRequest.getQ().length() > 2) {
        query.should(sb -> sb.spanFirst(sf -> sf.match(m -> m.spanMulti(spm -> spm
                                 .match( q -> q.prefix(p ->  p.field(esFieldMapper.get(parameter))
                                 .value(searchRequest.getQ().toLowerCase())))))
                          .end(3)
                          .boost(100f)));
      }
    } else {
      query.must(m -> m.matchAll(QueryBuilders.matchAll().build()));
    }
    filterQuery.ifPresent(fq -> query.must(m -> m.bool(fq)));

    SearchRequest.Builder request = new SearchRequest.Builder();
    request.index(index);

    request.size(searchRequest.getLimit());
    request.from(Math.max(0, (int) searchRequest.getOffset()));
    request.query(qb -> qb.bool(query.build()));

    // add source field
    request.source(s -> s.filter(f -> f.excludes(esFieldMapper.excludeFields())
      .includes(esFieldMapper.includeSuggestFields(parameter))));

    return request.build();
  }

  public SearchRequest buildSuggestQuery(String prefix, P parameter, Integer limit, String index) {
    SearchRequest.Builder request = new SearchRequest.Builder();
    request.index(index);

    String esField = esFieldMapper.get(parameter);

    // create suggest query
    request.suggest(s -> s
      .suggesters(esField, fs -> fs
          .prefix(prefix)
          .completion(cs -> cs
              .size(limit != null ? limit : SearchConstants.DEFAULT_SUGGEST_LIMIT)
              .skipDuplicates(true))));

    // add source field
    request.source(s -> s.filter(f -> f.excludes(esFieldMapper.excludeFields())
      .includes(esFieldMapper.includeSuggestFields(parameter))));

    return request.build();
  }

  private Optional<BoolQuery> buildQuery(Map<P, Set<String>> params, String qParam, Set<QueryField> queryFields) {
    // create bool node
    BoolQuery.Builder bool = QueryBuilders.bool();

    // adding full text search parameter
    if (!Strings.isNullOrEmpty(qParam)) {
      if (queryFields == null || queryFields.isEmpty()) {
        bool.must(esFieldMapper.fullTextQuery(qParam));
      } else {
        bool.must(esFieldMapper.fullTextQuery(qParam, queryFields));
      }
    }

    if (params != null && !params.isEmpty()) {
      // adding term queries to bool
      bool.filter(
          params.entrySet().stream()
          .filter(e -> Objects.nonNull(esFieldMapper.get(e.getKey())))
          .map(e -> buildTermQuery(e.getValue(), e.getKey(), esFieldMapper.get(e.getKey())))
          .filter(l -> !l.isEmpty())
          .flatMap(List::stream).collect(Collectors.toList())
        );
    }

    BoolQuery query  = bool.build();
    return query.must().isEmpty() && query.filter().isEmpty() ? Optional.empty() : Optional.of(query);
  }

  @VisibleForTesting
  GroupedParams groupParameters(FacetedSearchRequest<P> searchRequest) {
    GroupedParams groupedParams = new GroupedParams<P>();

    if (!searchRequest.isMultiSelectFacets()
        || searchRequest.getFacets() == null
        || searchRequest.getFacets().isEmpty()) {
      groupedParams.queryParams = searchRequest.getParameters();
      return groupedParams;
    }

    groupedParams.queryParams = new HashMap<>();
    groupedParams.postFilterParams = new HashMap<>();

    searchRequest
        .getParameters()
        .forEach(
            (k, v) -> {
              if (searchRequest.getFacets().contains(k)) {
                groupedParams.postFilterParams.put(k, v);
              } else {
                groupedParams.queryParams.put(k, v);
              }
            });

    return groupedParams;
  }

  private Optional<BoolQuery.Builder> buildPostFilter(Map<P, Set<String>> postFilterParams) {
    if (postFilterParams == null || postFilterParams.isEmpty()) {
      return Optional.empty();
    }

    BoolQuery.Builder bool = QueryBuilders.bool();


    bool.filter(
            postFilterParams.entrySet().stream()
                .flatMap(
                    e ->
                      buildTermQuery(e.getValue(), e.getKey(), esFieldMapper.get(e.getKey()))
                            .stream())
              .collect(Collectors.toList()));


    return Optional.of(bool);
  }

  private Optional<Map<String,Aggregation>> buildAggregations(
      FacetedSearchRequest<P> searchRequest,
      Map<P, Set<String>> postFilterParams,
      boolean facetsEnabled) {
    if (!facetsEnabled
        || searchRequest.getFacets() == null
        || searchRequest.getFacets().isEmpty()) {
      return Optional.empty();
    }

    if (searchRequest.isMultiSelectFacets()
        && postFilterParams != null
        && !postFilterParams.isEmpty()) {
      return Optional.of(buildFacetsMultiselect(searchRequest, postFilterParams));
    }

    return Optional.of(buildFacets(searchRequest));
  }

  private Map<String,Aggregation> buildFacetsMultiselect(
      FacetedSearchRequest<P> searchRequest, Map<P, Set<String>> postFilterParams) {

    if (searchRequest.getFacets().size() == 1) {
      // same case as normal facets
      return buildFacets(searchRequest);
    }

    Map<String,Aggregation> facets = new HashMap<>();
    searchRequest.getFacets().stream()
        .filter(p -> esFieldMapper.get(p) != null)
        .forEach(
            facetParam -> {

              // build filter aggs
              List<Query> filter =
                      postFilterParams.entrySet().stream()
                          .filter(entry -> entry.getKey() != facetParam)
                          .flatMap(
                              e ->
                                  buildTermQuery(
                                      e.getValue(), e.getKey(), esFieldMapper.get(e.getKey()))
                                      .stream())
                          .collect(Collectors.toList());

              // add filter to the aggs
              String esField = esFieldMapper.get(facetParam);


              Query filterAggs = AggregationBuilders.filter().bool(b -> b.filter(filter)).build();


              // build terms aggs and add it to the filter aggs
              TermsAggregation termsAggs =
                  buildTermsAggs(
                      esField,
                      extractFacetOffset(searchRequest, facetParam),
                      extractFacetLimit(searchRequest, facetParam),
                      searchRequest.getFacetMinCount());

              facets.put(esField, Aggregation.of(ab -> ab.filter(filterAggs)
                .aggregations("filtered_" + esField,
                                                  Aggregation.of(ta -> ta.terms(termsAggs)))));

            });

    return facets;
  }

  private Map<String,Aggregation> buildFacets(FacetedSearchRequest<P> searchRequest) {
    Map<String,Aggregation> facets = new HashMap<>();
    searchRequest.getFacets().stream()
      .filter(p -> esFieldMapper.get(p) != null)
      .forEach(
          facetParam -> {
            String esField = esFieldMapper.get(facetParam);
            TermsAggregation ta = buildTermsAggs(esField,
                                                 extractFacetOffset(searchRequest, facetParam),
                                                 extractFacetLimit(searchRequest, facetParam),
                                                 searchRequest.getFacetMinCount());
            facets.put(esField, Aggregation.of(ab -> ab.terms(ta)));
      });
     return facets;
  }

  private TermsAggregation buildTermsAggs(String esField, int facetOffset, int facetLimit, Integer minCount) {
    // build aggs for the field
    TermsAggregation.Builder builder = AggregationBuilders.terms().field(esField);


    // min count
    Optional.ofNullable(minCount).ifPresent(builder::minDocCount);

    // aggs size
    int size = calculateAggsSize(esField, facetOffset, facetLimit);
    builder.size(size);

    // aggs shard size
    builder.shardSize(
        Optional.ofNullable(esFieldMapper.getCardinality(esField))
            .orElse(DEFAULT_SHARD_SIZE.applyAsInt(size)));

    return builder.build();
  }

  private int calculateAggsSize(String esField, int facetOffset, int facetLimit) {
    int maxCardinality =
        Optional.ofNullable(esFieldMapper.getCardinality(esField)).orElse(Integer.MAX_VALUE);

    // the limit is bounded by the max cardinality of the field
    int limit = Math.min(facetOffset + facetLimit, maxCardinality);

    // we set a maximum limit for performance reasons
    if (limit > MAX_SIZE_TERMS_AGGS) {
      throw new IllegalArgumentException(
          "Facets paging is only supported up to " + MAX_SIZE_TERMS_AGGS + " elements");
    }
    return limit;
  }

  private List<Query> buildTermQuery(Collection<String> values, P param, String esField) {

    Query.Builder builder = new Query.Builder();
    if (esFieldMapper.isSpatialParameter(param)) {
      return values.stream()
              .map(v -> builder.geoShape(buildGeoShapeQuery(v, esField).build()).build())
              .collect(Collectors.toList());
    }

    List<Query> queries = new ArrayList<>();
    // collect queries for each value
    List<FieldValue> parsedValues = new ArrayList<>();
    for (String value : values) {
      if (isNumericRange(value) || isDateRange(value) && value.contains(RANGE_SEPARATOR)) {
        queries.add(new Query.Builder().range(buildRangeQuery(esField, value)).build());
        continue;
      }
      parsedValues.add(esFieldMapper.parseParamValue(value, param));
    }

    if (parsedValues.size() == 1) {
      // single term
      queries.add(new Query.Builder().term(t -> t.field(esField).value(parsedValues.get(0))).build());
    } else if (parsedValues.size() > 1) {
      // multi term query
      queries.add(new Query.Builder()
                    .terms(t -> t.field(esField)
                                 .terms(new TermsQueryField.Builder().value(parsedValues).build()))
                    .build());
    }
    return queries;
  }

  private RangeQuery buildRangeQuery(String esField, String value) {
    if (esFieldMapper.isDateField(esField)) {
      String[] values = value.split(RANGE_SEPARATOR);
      return RangeQuery.of(
          r ->
              r.date(
                  d -> {
                    d.field(esField);

                    LocalDateTime lowerBound = LOWER_BOUND_RANGE_PARSER.apply(values[0]);
                    if (lowerBound != null) {
                      d.gte(lowerBound.toString());
                    }

                    LocalDateTime upperBound = UPPER_BOUND_RANGE_PARSER.apply(values[1]);
                    if (upperBound != null) {
                      d.lte(upperBound.toString());
                    }

                    return d;
                  }));
    }

    String[] values = value.split(RANGE_SEPARATOR);
    return RangeQuery.of(
        r ->
            r.untyped(
                u -> {
                  u.field(esField);
                  if (!RANGE_WILDCARD.equals(values[0])) {
                    u.gte(JsonData.of(new BigDecimal(values[0])));
                  }
                  if (!RANGE_WILDCARD.equals(values[1])) {
                    u.lte(JsonData.of(new BigDecimal(values[1])));
                  }
                  return u;
                }));
  }

  private static LinearRing[] holes(Polygon polygon) {
    List<LinearRing> holes = new ArrayList<>(polygon.getNumInteriorRing());
    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      holes.add(GEOMETRY_FACTORY.createLinearRing(normalizePolygonCoordinates(polygon.getInteriorRingN(i).getCoordinates())));
    }
    return holes.toArray(new LinearRing[]{});
  }

  private static LinearRing shell(Polygon polygon) {
    return GEOMETRY_FACTORY.createLinearRing(normalizePolygonCoordinates(polygon.getExteriorRing().getCoordinates()));
  }

  private static Geometry readGeometry(String wkt) {
    try {
      Geometry geometry = new WKTReader().read(wkt);
      if (!isSupported(geometry)) {
        throw new IllegalArgumentException(geometry.getGeometryType() + " shape is not supported");
      }
      return geometry;
    } catch (ParseException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  private static boolean isSupported(Geometry geometry) {
    return geometry instanceof LinearRing || geometry instanceof Point || geometry instanceof Polygon || geometry instanceof MultiPolygon;
  }

  private static Geometry normalize(Geometry geometry) {
    if (geometry instanceof Polygon) {
      Polygon polygon = (Polygon) geometry;
      return GEOMETRY_FACTORY.createPolygon(shell(polygon), holes(polygon));
    } else if (geometry instanceof MultiPolygon) {
      List<Polygon> polygons = new ArrayList<>();
      for (int i = 0; i < geometry.getNumGeometries(); i++) {
        Polygon polygon = (Polygon) geometry.getGeometryN(i);
        polygons.add(GEOMETRY_FACTORY.createPolygon(shell(polygon), holes(polygon)));
      }
      return GEOMETRY_FACTORY.createMultiPolygon(polygons.toArray(new Polygon[]{}));
    }
    return geometry;
  }

  public static GeoShapeQuery.Builder buildGeoShapeQuery(String wkt, String fieldName) {

    Geometry geometry = normalize(readGeometry(wkt));

    WKTWriter wktWriter = new WKTWriter();

    return QueryBuilders.geoShape()
            .field(fieldName)
            .shape(b -> b.shape(JsonData.of(wktWriter.write(geometry)))
                         .relation(GeoShapeRelation.Within));
  }

  /** Eliminates consecutive duplicates. The order is preserved. */
  @VisibleForTesting
  static Coordinate[] normalizePolygonCoordinates(Coordinate[] coordinates) {
    List<Coordinate> normalizedCoordinates = new ArrayList<>();

    // we always have to keep the fist and last coordinates
    int i = 0;
    normalizedCoordinates.add(i++, coordinates[0]);

    for (int j = 1; j < coordinates.length; j++) {
      if (!coordinates[j - 1].equals(coordinates[j])) {
        normalizedCoordinates.add(i++, coordinates[j]);
      }
    }

    return normalizedCoordinates.toArray(new Coordinate[0]);
  }

  @VisibleForTesting
  static class GroupedParams<P extends SearchParameter> {
    Map<P, Set<String>> postFilterParams;
    Map<P, Set<String>> queryParams;
  }
}
