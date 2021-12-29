package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
public class NubMatchingServiceTestManual implements CommandLineRunner {

  private final NameUsageMatchingService matcher;

  @Autowired
  public NubMatchingServiceTestManual(NameUsageMatchingService matcher) {
    this.matcher = matcher;
  }

  public void testMatching() {
    LinneanClassification cl = new NameUsageMatch();
    // test identical
    matcher.match("Animalia", null, cl, true, true);
    matcher.match("Animals", null, cl, true, true);
    matcher.match("Insects", null, cl, true, true);
    cl.setKingdom("Animalia");
    matcher.match("Puma concolor", null, cl, true, true);
    cl.setKingdom("Plantae");
    matcher.match("Puma concolor", null, cl, true, true);
  }

  public static void main(String[] args) {
    SpringApplication.run(NubMatchingServiceTestManual.class, args);
  }

  @Override
  @SneakyThrows
  public void run(String... args) {
    testMatching();
  }
}
