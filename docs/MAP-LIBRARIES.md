# Primitive collection frameworks
Measuring the memory footprint of various collection libraries handling int primitive maps which we use to map neo4j node keys in the checklistbank-cli module.

We decided to use the fastutil library in the production code as it is still very memory efficient ( over 5 times less memory than the regular java HashMap) for large maps, introduces no further dependencies and implementes the java interfaces.

## Measuring object memory
We used the com.github.jbellis jamm agent library to implement an instrumentation to measure memory usage.
The surefire plugin was configured to execute the JVM with the jamm.jar as an agent, see [build section in pom.xml](https://github.com/gbif/checklistbank/blob/master/checklistbank-cli/pom.xml#L91).

# Libraries compared
For comparison we created a map object with each library and stored 100.000, 1.000.000 or 10.000.000 entries of randomly generated primitive ints in the map.

## Java collections
From the native java collections we tested the HashMap and TreeMap classes.

## HPPC 0.7.1
http://labs.carrotsearch.com/hppc.html

License: APL 2
Size:  1.1 MB
No further dependencies

    <dependency>
       <groupId>com.carrotsearch</groupId>
       <artifactId>hppc</artifactId>
       <version>0.7.1</version>
    </dependency>


## Goldman Sachs 6.1.0
https://github.com/goldmansachs/gs-collections

License:
Size: 8.2 MB
Dependencies:

 - net.jcip » jcip-annotations 1.0
 - 

    <dependency>
      <groupId>com.goldmansachs</groupId>
      <artifactId>gs-collections</artifactId>
      <version>6.1.0</version>
    </dependency>

## Trove 3.0.3
http://trove.starlight-systems.com/

License: LGPL
Size: 2.5 MB
No further dependencies

    <dependency>
      <groupId>net.sf.trove4j</groupId>
      <artifactId>trove4j</artifactId>
      <version>3.0.3</version>
    </dependency>

## FastUtils 7.0.13
http://fastutil.di.unimi.it

License: APL 2
Size: 16 MB
No further dependencies


    <dependency>
      <groupId>it.unimi.dsi</groupId>
      <artifactId>fastutil</artifactId>
      <version>7.0.6</version>
    </dependency>

## Koloboke 0.6.7
https://github.com/OpenHFT/Koloboke

License: APL 2
Size: 18 MB
Dependencies:

 - com.google.code.findbugs » jsr305
 - com.google.auto.value » auto-value
 - 
 
    <dependency>
      <groupId>net.openhft</groupId>
      <artifactId>koloboke-api-jdk6-7</artifactId>
      <version>0.6.7</version>
    </dependency>
    <dependency>
      <groupId>net.openhft</groupId>
      <artifactId>koloboke-impl-jdk6-7</artifactId>
      <version>0.6.7</version>
    </dependency>


# Memory Usage for 100.000 random int entries

    -----------------------------------
    class java.util.HashMap
    size: 48 B
    retained size: 7.1 MB
    inner object count: 299999
    -----------------------------------
    class java.util.TreeMap
    size: 48 B
    retained size: 6.9 MB
    inner object count: 300001
    -----------------------------------
    class com.carrotsearch.hppc.IntIntHashMap
    size: 56 B
    retained size: 2.0 MB
    inner object count: 5
    -----------------------------------
    class com.carrotsearch.hppc.IntIntScatterMap
    size: 56 B
    retained size: 2.0 MB
    inner object count: 4
    -----------------------------------
    class net.openhft.koloboke.collect.impl.hash.MutableLHashParallelKVIntIntMap
    size: 40 B
    retained size: 2.0 MB
    inner object count: 8
    -----------------------------------
    class com.gs.collections.impl.map.mutable.primitive.IntIntHashMap
    size: 32 B
    retained size: 2.0 MB
    inner object count: 2
    -----------------------------------
    class it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
    size: 64 B
    retained size: 2.0 MB
    inner object count: 3
    -----------------------------------
    class gnu.trove.map.hash.TIntIntHashMap
    size: 64 B
    retained size: 1.8 MB
    inner object count: 4
    -----------------------------------
   

# Memory Usage for 1.000.000 random int entries

    -----------------------------------
    class java.util.HashMap
    size: 48 B
    retained size: 69.0 MB
    inner object count: 2999669
    -----------------------------------
    class java.util.TreeMap
    size: 48 B
    retained size: 68.7 MB
    inner object count: 2999602
    -----------------------------------
    class com.carrotsearch.hppc.IntIntHashMap
    size: 56 B
    retained size: 16.0 MB
    inner object count: 5
    -----------------------------------
    class com.carrotsearch.hppc.IntIntScatterMap
    size: 56 B
    retained size: 16.0 MB
    inner object count: 4
    -----------------------------------
    class net.openhft.koloboke.collect.impl.hash.MutableLHashParallelKVIntIntMap
    size: 40 B
    retained size: 16.0 MB
    inner object count: 8
    -----------------------------------
    class com.gs.collections.impl.map.mutable.primitive.IntIntHashMap
    size: 32 B
    retained size: 16.0 MB
    inner object count: 2
    -----------------------------------
    class it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
    size: 64 B
    retained size: 16.0 MB
    inner object count: 3
    -----------------------------------
    class gnu.trove.map.hash.TIntIntHashMap
    size: 64 B
    retained size: 28.3 MB
    inner object count: 4
    -----------------------------------

   
# Memory Usage for 10.000.000 random int entries

    -----------------------------------
    class java.util.HashMap
    size: 48 B
    retained size: 673.6 MB
    inner object count: 29965448
    -----------------------------------
    class java.util.TreeMap
    size: 48 B
    retained size: 685.9 MB
    inner object count: 29965258
    -----------------------------------
    class com.carrotsearch.hppc.IntIntHashMap
    size: 56 B
    retained size: 128.0 MB
    inner object count: 5
    -----------------------------------
    class com.carrotsearch.hppc.IntIntScatterMap
    size: 56 B
    retained size: 128.0 MB
    inner object count: 4
    -----------------------------------
    class net.openhft.koloboke.collect.impl.hash.MutableLHashParallelKVIntIntMap
    size: 40 B
    retained size: 128.0 MB
    inner object count: 8
    -----------------------------------
    class com.gs.collections.impl.map.mutable.primitive.IntIntHashMap
    size: 32 B
    retained size: 256.0 MB
    inner object count: 2
    -----------------------------------
    class it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
    size: 64 B
    retained size: 128.0 MB
    inner object count: 3
    -----------------------------------
    class gnu.trove.map.hash.TIntIntHashMap
    size: 64 B
    retained size: 226.1 MB
    inner object count: 4
    -----------------------------------
