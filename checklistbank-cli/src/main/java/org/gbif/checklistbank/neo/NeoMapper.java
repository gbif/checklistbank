package org.gbif.checklistbank.neo;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A simple mapper for neo4j node or relationships to POJOs.
 * When a new POJO class is first encountered the fields are read and cached.
 */
public class NeoMapper {

    private final static Logger LOG = LoggerFactory.getLogger(NeoMapper.class);
    private final static Map<Class, List<FieldData>> FIELDS = Maps.newHashMap();
    private static NeoMapper singleton;

    enum FieldType {PRIMITIVE, NATIVE, ENUM, DATETIME, URI}

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
    public void store(Node Node, Object obj, boolean deleteNullProperties) {
        Class cl = obj.getClass();
        if (!FIELDS.containsKey(cl)) {
            initModelMap(cl);
        }
        try {
            for (FieldData f : FIELDS.get(cl)) {
                if (f.type == FieldType.PRIMITIVE) {
                    if (f.clazz.equals(long.class)) {
                        Node.setProperty(f.property, f.field.getLong(obj));
                    } else if (f.clazz.equals(int.class)) {
                        Node.setProperty(f.property, f.field.getInt(obj));
                    } else if (f.clazz.equals(boolean.class)) {
                        Node.setProperty(f.property, f.field.getBoolean(obj));
                    } else if (f.clazz.equals(double.class)) {
                        Node.setProperty(f.property, f.field.getDouble(obj));
                    } else if (f.clazz.equals(float.class)) {
                        Node.setProperty(f.property, f.field.getFloat(obj));
                    } else if (f.clazz.equals(char.class)) {
                        Node.setProperty(f.property, f.field.getChar(obj));
                    } else if (f.clazz.equals(byte.class)) {
                        Node.setProperty(f.property, f.field.getByte(obj));
                    }

                } else {

                    Object val = f.field.get(obj);
                    if (val == null) {
                        if (deleteNullProperties) {
                            Node.removeProperty(f.property);
                        }
                    } else {
                        switch (f.type) {
                            case NATIVE:
                                Node.setProperty(f.property, val);
                                break;
                            case ENUM:
                                Node.setProperty(f.property, ((Enum) val).ordinal());
                                break;
                            case DATETIME:
                                Node.setProperty(f.property, ((Date) val).getTime());
                                break;
                            case URI:
                                Node.setProperty(f.property, val.toString());
                                break;
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
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
    public void read(Node n, Object obj) {
        Class cl = obj.getClass();
        if (!FIELDS.containsKey(cl)) {
            initModelMap(cl);
        }
        try {
            for (FieldData f : FIELDS.get(cl)) {

                Object val = n.getProperty(f.property, null);
                if (val == null) {
                    f.field.set(obj, val);

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
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void initModelMap(Class cl) {
        List<FieldData> fields = Lists.newArrayList();
        this.FIELDS.put(cl, fields);

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
            } else {
                LOG.warn("Ignore field {} with unsupported type {}", f.getName(), fCl);
                continue;
            }

            String property = f.getName();
            fields.add(new FieldData(f, property, type, fCl));
            LOG.debug("Map field {} with type {} to neo property {}", f.getName(), fCl, property);
        }
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
