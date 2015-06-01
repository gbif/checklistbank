package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.utils.HumanSize;

import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntIntScatterMap;
import com.gs.collections.impl.map.mutable.primitive.IntIntHashMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.openhft.koloboke.collect.map.hash.HashIntIntMap;
import net.openhft.koloboke.collect.map.hash.HashIntIntMaps;
import org.github.jamm.MemoryMeter;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Manual test to compare memory usage for primitive int to int maps of various collection frameworks.
 * For results please see MAP-LIBRARIES.md
 */
@Ignore
public class IntMapMemoryTest {

  private final int amount = 100000;
  private final Random rnd = new Random();
  private MemoryMeter meter = new MemoryMeter();

  @Test
  public void testIntMapMemory() throws Exception {
    // TEST MEMORY CONSUMPTION
    measure(buildJavaHashMap());
    measure(buildJavaTreeMap());
    measure(buildHppcHashMap());
    measure(buildHppcScatterMap());
    measure(buildKoloboke());
    measure(buildGoldmanSachs());
    measure(buildFastUtils());
    measure(buildTrove());
  }

  private Object buildJavaHashMap() throws Exception {
    HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
    for (int i=0; i<amount; i++) {
      map.put(rnd.nextInt(), rnd.nextInt());
    }
    return map;
  }

  private Object buildJavaTreeMap() throws Exception {
    TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
    for (int i=0; i<amount; i++) {
      map.put(rnd.nextInt(), rnd.nextInt());
    }
    return map;
  }

  private Object buildHppcHashMap() throws Exception {
    IntIntMap map = new com.carrotsearch.hppc.IntIntHashMap();
    for (int i=0; i<amount; i++) {
      map.put(rnd.nextInt(), rnd.nextInt());
    }
    return map;
  }

  private Object buildHppcScatterMap() throws Exception {
    IntIntMap map = new IntIntScatterMap();
    for (int i=0; i<amount; i++) {
      map.put(rnd.nextInt(), rnd.nextInt());
    }
    return map;
  }

  private Object buildKoloboke() throws Exception {
    HashIntIntMap map = HashIntIntMaps.newMutableMap();
    for (int i=0; i<amount; i++) {
      map.put(rnd.nextInt(), rnd.nextInt());
    }
    return map;
  }

  private Object buildGoldmanSachs() throws Exception {
    IntIntHashMap map = new IntIntHashMap();
    for (int i=0; i<amount; i++) {
      map.put(rnd.nextInt(), rnd.nextInt());
    }
    return map;
  }

  private Object buildFastUtils() throws Exception {
    Int2IntMap map = new Int2IntOpenHashMap();
    for (int i=0; i<amount; i++) {
      map.put(rnd.nextInt(), rnd.nextInt());
    }
    return map;
  }

  private Object buildTrove() throws Exception {
    TIntIntMap map = new TIntIntHashMap();
    for (int i=0; i<amount; i++) {
      map.put(rnd.nextInt(), rnd.nextInt());
    }
    return map;
  }

  private void measure(Object obj) {
    System.out.println("-----------------------------------");
    System.out.println(obj.getClass());
    System.out.printf("size: %s\n", HumanSize.bytes(meter.measure(obj)));
    System.out.printf("retained size: %s\n", HumanSize.bytes(meter.measureDeep(obj)));
    System.out.printf("inner object count: %d\n", meter.countChildren(obj));
  }

}
