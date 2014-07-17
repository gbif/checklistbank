package org.gbif.checklistbank.neo;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;

/**
 * A simple mapper for neo4j node or relationships to POJOs or maps.
 * When a new POJO class is first encountered the fields are read and cached.
 */
public class NeoMapper {

    private final static Logger LOG = LoggerFactory.getLogger(NeoMapper.class);
    private final static Map<Class, List<FieldData>> FIELDS = Maps.newHashMap();
    private static NeoMapper singleton;

    enum FieldType {PRIMITIVE, NATIVE, ENUM, DATETIME, URI, SET, LIST}

    static class FieldData {

        public final Field field;
        public final String property;
        public final FieldType type;
        public final Class clazz;

        FieldData(Field field, String property, FieldType type, Class clazz) {
            this.field = field;
            this.property = property;
            this.type = type;
            this.clazz = clazz;
        }
    }

    private NeoMapper() {
    }

    public static synchronized NeoMapper instance() {
        if (singleton == null) {
            singleton = new NeoMapper();
        }
        return singleton;
    }

    /**
     * Store object properties in container.
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

    /**
     * Store object properties in a map.
     * @param keepNullProperties if true keep properties with null values in the map
     */
    public Map<String, Object>  propertyMap(Object obj, boolean keepNullProperties) {
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
                            case DATETIME:
                                props.put(f.property, ((Date) val).getTime());
                                break;
                            case URI:
                                props.put(f.property, val.toString());
                                break;
                            case SET:
                            case LIST:
                                Collection<?> vals = (Collection<?>) val;
                                int idx = 0;
                                if (f.clazz.isEnum()) {
                                    int[] arr = new int[vals.size()];
                                    for (Object v : vals) {
                                        arr[idx++] = ((Enum<?>) v).ordinal();
                                    }
                                    props.put(f.property, arr);
                                } else {
                                    String[] arr = new String[vals.size()];
                                    for (Object v : vals) {
                                        arr[idx++] = v.toString();
                                    }
                                    props.put(f.property, arr);
                                }
                                break;
                        }
                    } else if (keepNullProperties) {
                        props.put(f.property, null);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            LOG.error("Failed to read bean", e);
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

    public void storeEnum(Node n, String property, Enum<?> value) {
        if (value == null) {
            n.removeProperty(property);
        } else {
            n.setProperty(property, value.ordinal());
        }
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
                    f.field.set(obj, null);

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
                        case DATETIME:
                            f.field.set(obj, new Date((long) val));
                            break;
                        case URI:
                            f.field.set(obj, URI.create((String) val));
                            break;
                        case SET:
                            f.field.set(obj, populateCollection(f, val, Sets.newHashSet()));
                            break;
                        case LIST:
                            f.field.set(obj, populateCollection(f, val, Lists.newArrayList()));
                            break;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            LOG.error("Failed to read bean", e);
        }
        return obj;
    }

    private Collection<Object> populateCollection(FieldData f, Object val, Collection<Object> collection) {
        if (f.clazz.isEnum()) {
            for (int x : (int[]) val) {
                collection.add(f.clazz.getEnumConstants()[x]);
            }
        } else {
            for (String x : (String[]) val) {
                collection.add(x);
            }
        }
        return collection;
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
            FieldType type;
            if (fCl.isPrimitive()) {
                type = FieldType.PRIMITIVE;
            } else if (fCl.isEnum()) {
                type = FieldType.ENUM;
            } else if (String.class.isAssignableFrom(fCl)) {
                type = FieldType.NATIVE;
            } else if (Number.class.isAssignableFrom(fCl)) {
                type = FieldType.NATIVE;
            } else if (Boolean.class.isAssignableFrom(fCl)) {
                type = FieldType.NATIVE;
            } else if (Date.class.isAssignableFrom(fCl)) {
                type = FieldType.DATETIME;
            } else if (Character.class.isAssignableFrom(fCl)) {
                type = FieldType.NATIVE;
            } else if (URI.class.isAssignableFrom(fCl)) {
                type = FieldType.URI;
            } else if (Set.class.isAssignableFrom(fCl)) {
                type = FieldType.SET;
                // use generic type as class for set
                fCl = detectGenericType(f);
            } else if (List.class.isAssignableFrom(fCl)) {
                type = FieldType.LIST;
                // use generic type as class for list
                fCl = detectGenericType(f);
            } else {
                LOG.warn("Ignore field {} with unsupported type {}", f.getName(), fCl);
                continue;
            }

            String property = f.getName();
            fields.add(new FieldData(f, property, type, fCl));
            LOG.debug("Map field {} with type {} {} to neo property {}", f.getName(), type, fCl, property);
        }
    }

    private Class<?> detectGenericType(Field f) {
        Type gType = f.getGenericType();
        if (gType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType)gType;
            Type[] arr = pType.getActualTypeArguments();
            for (Type tp: arr) {
                return (Class<?>)tp;
            }
        }
        return null;
    }

    private List<Field> getAllFields(Class<?> cl) {
        List<Field> result = Lists.newArrayList();
        while (cl != null && cl != Object.class) {
            for (Field field : cl.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    result.add(field);
                }
            }
            cl = cl.getSuperclass();
        }
        return result;
    }

}
