package org.gbif.checklistbank.utils;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VerbatimNameUsageMapperKryoTest {

  private VerbatimNameUsageMapper mapper = new VerbatimNameUsageMapperKryo();

  class Pair<X,Y> {
    public X x;
    public Y y;
  }

  public class VerbSerde implements Callable<Boolean> {
    Random rnd = new Random();

    @Override
    public Boolean call() throws Exception {
      int max = 1000 + rnd.nextInt(1000);
      System.out.println("Run " + max + " serde tests in new thread");
      List<Pair<VerbatimNameUsage, byte[]>> usages = Lists.newArrayList();
      for (int idx=0; idx < max; idx++) {
        VerbatimNameUsage v = new VerbatimNameUsage();
        Pair<VerbatimNameUsage, byte[]> pair = new Pair<VerbatimNameUsage, byte[]>();
        pair.x = v;
        pair.y = mapper.write(v);
      }

      for (Pair<VerbatimNameUsage, byte[]> pair : usages) {
        VerbatimNameUsage v2 = mapper.read(pair.y);
        assertEquals(pair.x, v2);
      }
      return true;
    }
  }

  @Test
  public void testThreadSafety() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    ExecutorCompletionService<Boolean> ecs = new ExecutorCompletionService(executor);
    List<Future<Boolean>> futures = Lists.newArrayList();

    for (int i=0; i < 500; i++) {
      futures.add(ecs.submit(new VerbSerde()));
    }

    for (Future<Boolean> f : futures) {
      assertTrue(f.get());
    }
    System.out.println("Finished all threads successfully");
  }

}