package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.utils.VerbatimNameUsageMapper;
import org.gbif.checklistbank.utils.VerbatimNameUsageMapperKryo;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.type.JavaType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple mapper for neo4j node or relationships to POJOs or maps.
 * When a new POJO class is first encountered the fields are read and cached.
 * All immediate fields of the POJO are persisted as individual neo4j properties.
 * Any nested classes and collections will be serialized to SMILE byte arrays.
 */
public class NeoMapper {

  private final static Logger LOG = LoggerFactory.getLogger(NeoMapper.class);
  private final static Map<Class, List<FieldData>> FIELDS = Maps.newHashMap();
  private static final String PROP_VERBATIM_DATA = "verbatim";
  private final static Map<Rank, DwcTerm> classificationTerms = Maps.newTreeMap();
  private static NeoMapper instance;
  private final ObjectMapper objMapper;
  private VerbatimNameUsageMapper verbatimMapper = new VerbatimNameUsageMapperKryo();
  private final TypeFactory tf = TypeFactory.defaultInstance();
  private final JavaType issueType;
  enum FieldType {PRIMITIVE, NATIVE, ENUM, OTHER}

  static {
    for (DwcTerm ht : DwcTerm.HIGHER_RANKS) {
      classificationTerms.put(Rank.valueOf(ht.simpleName().toUpperCase()), ht);
    }
  }
  static class FieldData {

    public final Field field;
    public final String property;
    public final FieldType type;
    public final Class clazz;
    public final JavaType otherType;

    FieldData(Field field, String property, FieldType type, Class clazz, @Nullable JavaType otherType) {
      Preconditions.checkNotNull(field);
      Preconditions.checkNotNull(property);
      Preconditions.checkNotNull(type);
      Preconditions.checkNotNull(clazz);
      if (type == FieldType.OTHER) {
        Preconditions.checkNotNull(otherType);
      }
      this.field = field;
      this.property = property;
      this.type = type;
      this.clazz = clazz;
      this.otherType = otherType;
    }
  }

  private NeoMapper() {
    SmileFactory f = new SmileFactory();
    objMapper = new ObjectMapper(f);
    objMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      issueType = tf.constructType(NameUsage.class.getDeclaredField("issues").getGenericType());
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException("issues field not existing on NameUsage");
    }
  }

  public static synchronized NeoMapper instance() {
    if (instance == null) {
      instance = new NeoMapper();
    }
    return instance;
  }

  public void setProperty(Node n, Term property, Object value) {
    if (value == null) {
      n.removeProperty(propertyName(property));
    } else {
      n.setProperty(propertyName(property), value);
    }
  }

  /**
   * Store object properties in container.
   *
   * @param deleteNullProperties if true removed properties from the Node which are null in the bean
   */
  public void store(Node n, Object obj, boolean deleteNullProperties) {
    for (Map.Entry<String, Object> entry : propertyMap(obj, deleteNullProperties).entrySet()) {
      if (entry.getValue() != null) {
        n.setProperty(entry.getKey(), entry.getValue());
      } else if(deleteNullProperties) {
        n.removeProperty(entry.getKey());
      }
    }
  }

  public void store(Node n, NameUsageContainer usage) {
    store(n, usage, true);
  }

  public Map<String, Object> propertyMap(String coreID, NameUsage u, VerbatimNameUsage v) {
    Map<String, Object> props = propertyMap(u, false);
    // in addition to the NameUsage properties also keep a few more id terms needed for resolution in the normalizer
    props.put(DcTerm.identifier.simpleName(), coreID);
    props.put(PROP_VERBATIM_DATA, verbatimMapper.write(v));
    return props;
  }

  /**
   * Store object properties in a map.
   *
   * @param keepNullProperties if true keep properties with null values in the map
   */
  public Map<String, Object> propertyMap(Object obj, boolean keepNullProperties) {
    Map<String, Object> props = Maps.newHashMap();

    Class cl = obj.getClass();
    if (!FIELDS.containsKey(cl)) {
      initModelMap(cl);
    }
    try {
      for (FieldData f : FIELDS.get(cl)) {
        if (f.type == FieldType.PRIMITIVE) {
          if (f.clazz.equals(long.class)) {
            props.put(f.property, f.field.getLong(obj));
          } else if (f.clazz.equals(int.class)) {
            props.put(f.property, f.field.getInt(obj));
          } else if (f.clazz.equals(boolean.class)) {
            props.put(f.property, f.field.getBoolean(obj));
          } else if (f.clazz.equals(double.class)) {
            props.put(f.property, f.field.getDouble(obj));
          } else if (f.clazz.equals(float.class)) {
            props.put(f.property, f.field.getFloat(obj));
          } else if (f.clazz.equals(char.class)) {
            props.put(f.property, f.field.getChar(obj));
          } else if (f.clazz.equals(byte.class)) {
            props.put(f.property, f.field.getByte(obj));
          }

        } else {

          Object val = f.field.get(obj);
          if (val != null) {
            switch (f.type) {
              case NATIVE:
                props.put(f.property, val);
                break;
              case ENUM:
                props.put(f.property, ((Enum) val).ordinal());
                break;
              case OTHER:
                props.put(f.property, objMapper.writeValueAsBytes(val));
                break;
            }
          } else if (keepNullProperties) {
            props.put(f.property, null);
          }
        }
      }
    } catch (IllegalAccessException e) {
      LOG.error("Failed to read bean", e);
    } catch (IOException e) {
      LOG.error("Failed to convert bean properties to SMILE", e);
    }
    return props;
  }

  public static Rank readRank(Node n) {
    return readEnum(n, TaxonProperties.RANK, Rank.class, null);
  }

  public static Origin readOrigin(Node n) {
    return readEnum(n, TaxonProperties.ORIGIN, Origin.class, null);
  }

  public static String readScientificName(Node n) {
    return (String) n.getProperty(TaxonProperties.SCIENTIFIC_NAME, null);
  }

  public static String readCanonicalName(Node n) {
    return (String) n.getProperty(TaxonProperties.CANONICAL_NAME, null);
  }

  public static <T> T readEnum(Node n, String property, Class<T> vocab, T defaultValue) {
    Object val = n.getProperty(property, null);
    if (val != null) {
      int idx = (Integer) val;
      return (T) vocab.getEnumConstants()[idx];
    }
    return defaultValue;
  }

  public void storeEnum(Node n, String property, Enum value) {
    n.setProperty(property, value.ordinal());
  }

  public void storeEnum(Node n, Term property, Enum value) {
    n.setProperty(propertyName(property), value.ordinal());
  }
  /**
   * Reads object properties from container and sets the object fields.
   */
  public <T> T read(Node n, T obj) {
    Class cl = obj.getClass();
    if (!FIELDS.containsKey(cl)) {
      initModelMap(cl);
    }
    try {
      for (FieldData f : FIELDS.get(cl)) {

        Object val = n.getProperty(f.property, null);
        if (val == null) {
          switch (f.type) {
            case PRIMITIVE:
              // keep default
              break;
            case NATIVE:
            case ENUM:
            case OTHER:
              if (List.class.isAssignableFrom(f.clazz)) {
                f.field.set(obj, Lists.newArrayList());
              } else if (Set.class.isAssignableFrom(f.clazz)) {
                f.field.set(obj, Sets.newHashSet());
              } else {
                f.field.set(obj, null);
              }
              break;
          }

        } else {
          switch (f.type) {
            case PRIMITIVE:
            case NATIVE:
              f.field.set(obj, val);
              break;
            case ENUM:
              Object[] values = f.clazz.getEnumConstants();
              f.field.set(obj, values[(int) val]);
              break;
            case OTHER:
              f.field.set(obj, objMapper.readValue((byte[]) val, f.otherType));
              break;
          }
        }
      }
    } catch (IllegalAccessException e) {
      LOG.error("Failed to read bean", e);

    } catch (IOException e) {
      LOG.error("Failed to read bean property", e);
    }
    return obj;
  }

  /**
   * Reads a node into a name usage instance with keys being the node ids long values.
   */
  public NameUsageContainer read(Node n) {
    if (n != null) {
      NameUsageContainer u = read(n, new NameUsageContainer());
      // map node id to key, its not fixed across tests but stable within one
      u.setKey((int) n.getId());

      try {
        IdName in = getRelatedTaxonKey(n, RelType.BASIONYM_OF, Direction.INCOMING);
        if (in != null) {
          u.setBasionymKey(in.id);
          u.setBasionym(in.name);
        }
      } catch (RuntimeException e) {
        LOG.error("Unable to read basionym relation for {} with node {}", u.getScientificName(), n.getId());
        addIssue(n, NameUsageIssue.RELATIONSHIP_MISSING);
        addRemark(n, "Multiple original name relations");
      }

      try {
        // pro parte synonym relations must have been flattened already...
        IdName in = getRelatedTaxonKey(n, RelType.SYNONYM_OF, Direction.OUTGOING);
        if (in != null) {
          u.setAcceptedKey(in.id);
          u.setAccepted(in.name);
          // update synonym flag based on relations
          u.setSynonym(true);
        }
      } catch (RuntimeException e) {
        LOG.error("Unable to read accepted name relation for {} with node {}", u.getScientificName(), n.getId());
        addIssue(n, NameUsageIssue.RELATIONSHIP_MISSING);
        addRemark(n, "Multiple accepted name relations");
      }

      try {
        IdName in = getRelatedTaxonKey(n, RelType.PARENT_OF, Direction.INCOMING);
        if (in != null) {
          u.setParentKey(in.id);
          u.setParent(in.name);
        }
      } catch (RuntimeException e) {
        LOG.error("Unable to read parent relation for {} with node {}", u.getScientificName(), n.getId());
        addIssue(n, NameUsageIssue.RELATIONSHIP_MISSING);
        addRemark(n, "Multiple parent relations");
      }

      return u;
    }
    return null;
  }

  public VerbatimNameUsage readVerbatim(Node n) {
    if (n != null) {
      return verbatimMapper.read((byte[]) n.getProperty(PROP_VERBATIM_DATA, null));
    }
    return null;
  }

  private IdName getRelatedTaxonKey(Node n, RelType type, Direction dir) {
    Relationship rel = n.getSingleRelationship(type, dir);
    if (rel != null) {
      IdName in = new IdName();
      Node n2 = rel.getOtherNode(n);
      in.id = (int)n2.getId();
      in.name = (String) n2.getProperty(TaxonProperties.CANONICAL_NAME, n2.getProperty(TaxonProperties.SCIENTIFIC_NAME, null));
      return in;
    }
    return null;
  }

  /**
   * Returns the field type for any class
   *
   * @param fCl the class to inspect
   *
   * @return the field type or null if NeoMapper cannot handle this type
   */
  private FieldType fieldTypeOf(Class fCl) {
    if (fCl.isPrimitive()) {
      return FieldType.PRIMITIVE;
    } else if (fCl.isEnum()) {
      return FieldType.ENUM;
    } else if (String.class.isAssignableFrom(fCl)) {
      return FieldType.NATIVE;
    } else if (Number.class.isAssignableFrom(fCl)) {
      return FieldType.NATIVE;
    } else if (Boolean.class.isAssignableFrom(fCl)) {
      return FieldType.NATIVE;
    } else if (Character.class.isAssignableFrom(fCl)) {
      return FieldType.NATIVE;
    } else {
      return FieldType.OTHER;
    }
  }

  private void initModelMap(Class cl) {
    List<FieldData> fields = Lists.newArrayList();
    FIELDS.put(cl, fields);

    for (Field f : getAllFields(cl)) {
      try {
        f.setAccessible(true);
      } catch (Exception e) {
        // Ignored. This is only a final precaution, nothing we can do.
      }

      Class fCl = f.getType();
      FieldType type = fieldTypeOf(fCl);
      JavaType otherType = null;

      if (type == null) {
        LOG.warn("Ignore field {} with unsupported type {}", f.getName(), fCl);
        continue;

      } else if (type == FieldType.OTHER) {

        // use generic type as class for collections
        otherType = tf.constructType(f.getGenericType());
      }

      // allow JsonProperty annotation to override the default field name
      JsonProperty anno = f.getAnnotation(JsonProperty.class);
      String property;
      if (anno == null) {
        property = f.getName();
      } else {
        property = anno.value();
      }

      fields.add(new FieldData(f, property, type, fCl, otherType));
      LOG.debug("Map field {} with type {} {} to neo property {}", f.getName(), type, otherType == null ? fCl : otherType, property);
    }
  }

  private List<Field> getAllFields(Class<?> cl) {
    List<Field> result = Lists.newArrayList();
    while (cl != null && cl != Object.class) {
      for (Field field : cl.getDeclaredFields()) {
        if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
          result.add(field);
        }
      }
      cl = cl.getSuperclass();
    }
    return result;
  }

  public static String propertyName(Term t) {
    return "v_" + t.simpleName();
  }

  public static boolean hasProperty(Node n, Term t) {
    return n.hasProperty(propertyName(t));
  }

  public static String value(Node n, Term t) {
    return (String) n.getProperty(propertyName(t), null);
  }

  public static List<String> listValue(Node n, Term t) {
    return Lists.newArrayList( (String[]) n.getProperty(propertyName(t), new String[0]));
  }

  public static LinneanClassification readVerbatimClassification(Node n) {
    LinneanClassification lc = new NameUsage();
    for (Map.Entry<Rank, DwcTerm> ct : classificationTerms.entrySet()) {
      ClassificationUtils.setHigherRank(lc, ct.getKey(), (String) n.getProperty(ct.getValue().simpleName(), null));
    }
    return lc;
  }

  /**
   * @return list of ranked name instances from lowest rank to highest using the verbatim denormed classification.
   */
  public static List<RankedName> listVerbatimClassification(Node n, @Nullable Rank minRank) {
    List<RankedName> cl = Lists.newArrayList();
    for (Map.Entry<Rank, DwcTerm> ct : classificationTerms.entrySet()) {
      if ((minRank == null || ct.getKey().higherThan(minRank)) && n.hasProperty(ct.getValue().simpleName())) {
        RankedName rn = new RankedName();
        rn.name = (String) n.getProperty(ct.getValue().simpleName());
        rn.rank = ct.getKey();
        cl.add(rn);
      }
    }
    Collections.reverse(cl);
    return cl;
  }

  public static RankedName readRankedName(Node n) {
    RankedName rn = new RankedName();
    rn.node = n;
    rn.name = (String) n.getProperty(TaxonProperties.CANONICAL_NAME, "");
    rn.rank = readEnum(n, TaxonProperties.RANK, Rank.class, null);
    return rn;
  }

  public void addIssue(Node n, NameUsageIssue issue) {
    try {
      Set<NameUsageIssue> issues;
      if (n.hasProperty(TaxonProperties.ISSUE)) {
        issues = objMapper.readValue((byte[]) n.getProperty(TaxonProperties.ISSUE), issueType);
      } else {
        issues = Sets.newHashSet();
      }
      issues.add(issue);
      n.setProperty(TaxonProperties.ISSUE, objMapper.writeValueAsBytes(issues));

    } catch (IOException e) {
      // TODO: Handle exception
    }
  }

  /**
   * Adds a string remark to the taxonRemarks property of a usage.
   * Existing remarks are left untouched and the new string is appended.
   */
  public void addRemark(Node n, String remark) {
    if (n.hasProperty(TaxonProperties.REMARKS)) {
      remark = n.getProperty(TaxonProperties.REMARKS) + "; " + remark;
    }
    n.setProperty(TaxonProperties.REMARKS, remark);
  }

  public void setNubKey(Node n, Integer nubKey) {
    if (nubKey != null) {
      n.setProperty(TaxonProperties.NUB_KEY, nubKey);
    } else {
      n.removeProperty(TaxonProperties.NUB_KEY);
    }
  }

}
