package org.gbif.checklistbank.service.mybatis;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;

import java.lang.annotation.Annotation;
import javax.annotation.Nullable;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Rule;

public class MyBatisServiceITBase<T> {

  private final Class<T> serviceClass;
  private final Class<? extends Annotation> annotationType;
  private Injector injector;
  protected T service;

  @Rule
  public ClbDbTestRule dbSetup = ClbDbTestRule.squirrels();

  public MyBatisServiceITBase(Class<T> serviceClass) {
    this(serviceClass, null);
  }

  public MyBatisServiceITBase(Class<T> serviceClass, @Nullable Class<? extends Annotation> annotationType) {
    this.serviceClass = serviceClass;
    this.annotationType = annotationType;
  }

  @Before
  public void init() throws Exception {
    Module module = new ChecklistBankServiceMyBatisModule(dbSetup.getProperties());
    injector = Guice.createInjector(module);
    if (annotationType != null) {
      service = injector.getInstance(Key.get(serviceClass, annotationType));
    } else {
      service = injector.getInstance(serviceClass);
    }
  }

  public <K> K getInstance(Class<K> clazz) {
    return injector.getInstance(clazz);
  }

}