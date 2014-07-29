package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.service.VerbatimNameUsageMapper;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.Term;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
  private static NeoMapper instance;
  private final ObjectMapper objMapper;
  private VerbatimNameUsageMapper verbatimMapper = new VerbatimNameUsageMapper();
  private final TypeFactory tf = TypeFactory.defaultInstance();

  enum FieldType {PRIMITIVE, NATIVE, ENUM, OTHER}

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
  }

  public static synchronized NeoMapper instance() {
    if (instance == null) {
      instance = new NeoMapper();
    }
    return instance;
  }

  /**
   * Store object properties in container.
   *
   * @param deleteNullProperties if true removed properties from the Node which are null in the bean
   */
  public void store(Node n, Object obj, boolean deleteNullProperties) {
    for (Map.Entry<String, Object> entry : propertyMap(obj, deleteNullProperties).entrySet()) {
      if (deleteNullProperties && entry.getValue() == null) {
        n.removeProperty(entry.getKey());
      } else {
        n.setProperty(entry.getKey(), entry.getValue());
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

  public <T> T readEnum(Node n, String property, Class<T> vocab) {
    Object val = n.getProperty(property, null);
    if (val != null) {
      int idx = (Integer) val;
      return (T) vocab.getEnumConstants()[idx];
    }
    return null;
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
      u.setParentKey(getRelatedTaxonKey(n, RelType.PARENT_OF, Direction.INCOMING));
      u.setAcceptedKey(getRelatedTaxonKey(n, RelType.SYNONYM_OF, Direction.OUTGOING));
      u.setBasionymKey(getRelatedTaxonKey(n, RelType.BASIONYM_OF, Direction.INCOMING));
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

  private Integer getRelatedTaxonKey(Node n, RelType type, Direction dir) {
    Relationship rel = n.getSingleRelationship(type, dir);
    if (rel != null) {
      return (int) rel.getOtherNode(n).getId();
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

      String property = f.getName();
      fields.add(new FieldData(f, property, type, fCl, otherType));
      LOG.debug("Map field {} with type {} {} to neo property {}", f.getName(), type, otherType == null ? fCl : otherType, property);
    }
  }

  private Class<?> detectGenericType(Field f) {
    Type gType = f.getGenericType();
    if (gType instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) gType;
      Type[] arr = pType.getActualTypeArguments();
      for (Type tp : arr) {
        return (Class<?>) tp;
      }
    }
    return null;
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

  public static String[] listValue(Node n, Term t) {
    return (String[]) n.getProperty(propertyName(t), new String[0]);
  }

  public static void setValue(Node n, Term t, String value) {
    if (Strings.isNullOrEmpty(value)) {
      n.removeProperty(propertyName(t));
    } else {
      n.setProperty(propertyName(t), value);
    }
  }

}
