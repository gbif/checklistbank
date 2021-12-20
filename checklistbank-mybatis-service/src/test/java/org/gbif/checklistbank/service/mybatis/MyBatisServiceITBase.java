package org.gbif.checklistbank.service.mybatis;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisConfiguration;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;

import java.lang.annotation.Annotation;
import java.util.Optional;
import javax.annotation.Nullable;


import com.google.inject.Injector;
import com.google.inject.Key;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MyBatisServiceITBase<T> {

  private final Class<T> serviceClass;
  private final Class<? extends Annotation> annotationType;
  private AnnotationConfigApplicationContext ctx;
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
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ChecklistBankServiceMyBatisConfiguration.class);
    if (annotationType != null) {
      service = getInstance(serviceClass, annotationType);
    } else {
      service = ctx.getBean(serviceClass);
    }
  }

  public <K> K getInstance(Class<K> clazz) {
    return ctx.getBean(clazz);
  }

  public <K> K getInstance(Class<K> clazz, Class<? extends Annotation> annotationType) {
    return (K)ctx.getBeansWithAnnotation(annotationType).values().stream().filter(clazz::isInstance).findFirst().orElse(null);
  }

}