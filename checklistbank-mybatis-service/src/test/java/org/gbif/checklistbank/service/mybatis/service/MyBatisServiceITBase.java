package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.checklistbank.service.mybatis.persistence.ChecklistBankMyBatisConfiguration;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;

import java.lang.annotation.Annotation;
import javax.annotation.Nullable;

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
    ctx.register(ChecklistBankMyBatisConfiguration.class);
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