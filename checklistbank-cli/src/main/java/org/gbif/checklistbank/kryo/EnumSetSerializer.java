package org.gbif.checklistbank.kryo;

import java.lang.reflect.Field;
import java.util.EnumSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

/**
 * A serializer for {@link EnumSet}s.
 * @see <a href="https://github.com/magro/kryo-serializers">kryo-serializers</a>
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
@SuppressWarnings( { "unchecked", "rawtypes" } )
public class EnumSetSerializer extends Serializer<EnumSet<? extends Enum<?>>> {

    private static final Field TYPE_FIELD;

    static {
        try {
            TYPE_FIELD = EnumSet.class.getDeclaredField( "elementType" );
            TYPE_FIELD.setAccessible( true );
        } catch ( final Exception e ) {
            throw new RuntimeException( "The EnumSet class seems to have changed, could not access expected field.", e );
        }
    }

    @Override
    public EnumSet<? extends Enum<?>> copy (final Kryo kryo, final EnumSet<? extends Enum<?>> original) {
        return original.clone();
    }

    @Override
    public EnumSet read(final Kryo kryo, final Input input, final Class<EnumSet<? extends Enum<?>>> type) {
        final Class<Enum> elementType = kryo.readClass( input ).getType();
        final EnumSet result = EnumSet.noneOf( elementType );
        final int size = input.readInt(true);
        final Enum<?>[] enumConstants = elementType.getEnumConstants();
        for ( int i = 0; i < size; i++ ) {
            result.add( enumConstants[input.readInt(true)] );
        }
        return result;
    }

    @Override
    public void write(final Kryo kryo, final Output output, final EnumSet<? extends Enum<?>> set) {
        kryo.writeClass( output, getElementType( set ) );
        output.writeInt( set.size(), true );
        for (final Enum item : set) {
            output.writeInt(item.ordinal(), true);
        }

        if ( TRACE ) trace( "kryo", "Wrote EnumSet: " + set );
    }

    private Class<? extends Enum<?>> getElementType( final EnumSet<? extends Enum<?>> set ) {
        try {
            return (Class)TYPE_FIELD.get( set );
        } catch ( final Exception e ) {
            throw new RuntimeException( "Could not access keys field.", e );
        }
    }
}