package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Rule;

public class MapperITBase<T> {
    private static final String PREFIX = "checklistbank.db.";

    protected T mapper;
    private Injector injector;
    private final Class<T> mapperClass;

    @Rule
    public ClbDbTestRule sbSetup = ClbDbTestRule.empty();

    public MapperITBase(Class<T> mapperClass) {
        this.mapperClass = mapperClass;
    }

    @Before
    public void init() throws Exception {
        Module module = new InternalChecklistBankServiceMyBatisModule(strippedProperties(sbSetup.getProperties()), 500);
        injector = Guice.createInjector(module);
        mapper = injector.getInstance(mapperClass);
    }

    /**
     * Filtered properties also removing the given property prefix string
     */
    private Properties strippedProperties(Properties properties) {
        Properties filtered = new Properties();

        for(String key : properties.stringPropertyNames()) {
            if (key.startsWith(PREFIX)) {
                filtered.setProperty(key.substring(PREFIX.length()), properties.getProperty(key));
            }
        }
        return filtered;
    }

    public <K> K getInstance(Class<K> clazz) {
        return injector.getInstance(clazz);
    }
}