package org.gbif.checklistbank.index;

import org.gbif.checklistbank.index.model.NameUsageAvro;

import java.io.File;

import com.google.common.collect.Lists;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;

public class AvroTest {


  public static void main(String[] args) throws  Exception {
    File file = new File("nus.avro");
    ClassLoader classLoader = AvroTest.class.getClassLoader();
    Schema schema = new Schema.Parser().parse(classLoader.getResource("solr.avrsc").openStream());
    DatumWriter<NameUsageAvro> datumWriter = new SpecificDatumWriter<>(NameUsageAvro.class);
    DataFileWriter<NameUsageAvro> dataFileWriter = new DataFileWriter<NameUsageAvro>(datumWriter);
    dataFileWriter.create(schema, file);
    NameUsageAvro nu = new NameUsageAvro();
    nu.setKey(1);
    nu.setVernacularName(Lists.newArrayList("a","b"));
    dataFileWriter.append(nu);
    dataFileWriter.close();
  }
}
