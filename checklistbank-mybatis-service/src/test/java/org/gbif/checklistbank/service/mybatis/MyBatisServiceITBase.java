package org.gbif.checklistbank.service.mybatis;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Rule;

public class MyBatisServiceITBase<T> {

    private final Class<T> serviceClass;
    private Injector injector;
    protected T service;

    @Rule
    public ClbDbTestRule dbunit = ClbDbTestRule.squirrels();

    public MyBatisServiceITBase(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Before
    public void init() throws Exception {
        Module module = new ChecklistBankServiceMyBatisModule(dbunit.getProperties());
        injector = Guice.createInjector(module);
        service = injector.getInstance(serviceClass);
    }

    public <K> K getInstance(Class<K> clazz) {
        return injector.getInstance(clazz);
    }

}