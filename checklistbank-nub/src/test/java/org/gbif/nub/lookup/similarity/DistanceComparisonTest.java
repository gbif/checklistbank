package org.gbif.nub.lookup.similarity;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistanceComparisonTest {
  private static final Logger LOG = LoggerFactory.getLogger(DistanceComparisonTest.class);

  private final StringSimilarity DL = new DamerauLevenshtein();
  private final StringSimilarity MDL2= new ModifiedDamerauLevenshtein(2);
  private final StringSimilarity MDL3= new ModifiedDamerauLevenshtein(3);
  private final StringSimilarity JW = new JaroWinkler();
  private final StringSimilarity MJW= new ModifiedJaroWinkler();


  private List<String[]> names = ImmutableList.of(
    new String[] {"Helga", "Markus"},
    new String[] {"Abies", "Apis"},
    new String[] {"Aneplus", "Anelus"},
    new String[] {"Aneplus vulgaris", "Anelus vulgaris"},
    new String[] {"Aneplus", "Anephlus"},
    new String[] {"Anelus", "Anephlus"},
    new String[] {"Abies alba", "Abies alba"},
    new String[] { "Apies alba", "Abies alba" },
    new String[] { "Apbies alba", "Abies alba" },
    new String[] { "Abbies alba", "Abies alba" },
    new String[] { "Obies alba", "Abies alba" },
    new String[] { "Abies allba", "Abies alba" },
    new String[] { "Abies ahlba", "Abies alba" },
    new String[] { "Abbies ahlba", "Abies alba" },
    new String[] { "Puma concolor", "Abies alba" },
    new String[] { "Pumac oncolor", "Puma concolor" },
    new String[] { "Pumaco color", "Puma concolor" },
    new String[] { "Pumae concolour", "Puma concolor" },
    new String[] { "Cnaemidophorus rhododactyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Cnaemidophorus rododactyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Caemidophorus rhododactyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Cnamidophorus rhododactyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Cnaemidophora rhododactyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Cnaemidophorhus rhododactyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Cnaemydophorus rhododactyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Cneamidophorus rhododactyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Cnaemidophorus rododactyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Cnaemidophorus rhododatyla", "Cnaemidophorus rhododactyla" },
    new String[] { "Cnaemidophorus rhododactula", "Cnaemidophorus rhododactyla" },
    new String[] { "Cnaemidophorus rhododactulla", "Cnaemidophorus rhododactyla" },

    new String[] { "Yigoga forcipula", "Yigoga forcipula" },
    new String[] { "Igoga forcipula", "Yigoga forcipula" },
    new String[] { "Yiogoga forcipula", "Yigoga forcipula" },
    new String[] { "Yigoga forzipula", "Yigoga forcipula" },

    new String[] { "Agrotis ripae", "Agrotis ripae" },
    new String[] { "Agrostis ripae", "Agrotis ripae" },
    new String[] { "Agrotis ripa", "Agrotis ripae" },
    new String[] { "Agrotis ripea", "Agrotis ripae" },

    new String[] { "Lasionycta leucocycla", "Lasionycta leucocycla" },
    new String[] { "Lasionicta leucocycla", "Lasionycta leucocycla" },
    new String[] { "Lasionykta leucocycla", "Lasionycta leucocycla" },
    new String[] { "Lasionycta leucocicla", "Lasionycta leucocycla" },

    new String[] { "Ophthalmis lincea", "Ophthalmis lincea" },
    new String[] { "Oftalmis lincea", "Ophthalmis lincea" },
    new String[] { "Ophthalmis linzea", "Ophthalmis lincea" }
  );

  @Test
  public void testGetSimilarity() throws Exception {
    for (String[] ns : names) {
      LOG.debug(ns[0] + "  x  " + ns[1]);
      doitTime("DL  ", DL, ns[0], ns[1]);
      doitTime("MDL2", MDL2, ns[0], ns[1]);
      doitTime("MDL3", MDL3, ns[0], ns[1]);
      doitTime("JW  ", JW, ns[0], ns[1]);
      doitTime("MJW ", MJW, ns[0], ns[1]);
    }
  }

  public static void main (String[] args) {
    int[] sim = new int[]{60,70,80,85,90,91,92,93,94,95,96,97,98,99,100,101,102,105,110,115,120,130,140,150,160,175};
    for (int s : sim){
      int ns = s <= 90 ? s*10 : 900 + (int) (100d * (Math.log10((s-80d)*1.1) - 1) );

      System.out.println(s + " => " + (Math.log10((s-80d)*1.1)) );
      System.out.println(s + " => " + ns );
    }
  }

  private double doit(String name, StringSimilarity sim, String x1, String x2) {
    double s = sim.getSimilarity(x1, x2);
    LOG.debug(" {}={}", name, s);
    return s;
  }

  private double doitTime(String name, StringSimilarity sim, String x1, String x2) {
    StopWatch watch = new StopWatch();
    watch.start();
    double s = sim.getSimilarity(x1, x2);
    int repeat = 5000;
    while (repeat > 0) {
      sim.getSimilarity(x1, x2);
      repeat--;
    }
    LOG.debug(" {}={}   1000x in {} microsec", name, s, watch.getNanoTime() / 1000);
    return s;
  }


}
