package org.gbif.nub.build;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.model.Usage;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

public class NubSourceDataExporter {
  private final File dir;
  private final UsageService uService;
  private final ParsedNameService nService;
  private final ObjectMapper mapper = new ObjectMapper();

  public static final UUID MAMMALS = UUID.fromString("672aca30-f1b5-43d3-8a2b-c1606125fa1b");
  public static final UUID HOMONYMS = UUID.fromString("714c64e3-2dc1-4bb7-91e4-54be5af4da12");
  public static final UUID TOORSACORRIDOR = UUID.fromString("d9f426e7-845c-4a63-be0d-18506f235357");
  public static final UUID GRACILLARIIDAE = UUID.fromString("bb5f507f-f7de-4a5a-ae7f-ad8abbe68bef");
  public static final UUID CICADELLINAE = UUID.fromString("26bca1b5-3ef6-4e97-9672-0058c79185fb");
  public static final UUID IOCBIRDS = UUID.fromString("c696e5ee-9088-4d11-bdae-ab88daffab78");
  public static final UUID VASCAN = UUID.fromString("3f8a1297-3259-4700-91fc-acc4170b27ce");
  // only families and above!
  public static final UUID COL = NubGenerator.COL_KEY;

  public NubSourceDataExporter(File dir, UsageService uService, ParsedNameService nService) {
    this.dir = dir;
    this.uService = uService;
    this.nService = nService;
  }

  private void export(UUID datasetKey) throws IOException {
    JsonFactory jfactory = new JsonFactory();
    jfactory.setCodec(mapper);

    exportNames(datasetKey, jfactory);
    exportUsages(datasetKey, jfactory);
  }

  private void exportUsages(UUID datasetKey, JsonFactory jfactory) throws IOException {
    File json = new File(dir, datasetKey.toString() + "-usages.json");
    System.out.println("Start dumping usages for dataset " + datasetKey + " to " + json.getAbsolutePath());
    JsonGenerator jGen = jfactory.createJsonGenerator(json, JsonEncoding.UTF8);
    jGen.useDefaultPrettyPrinter();
    jGen.writeStartArray();
    PagingRequest page = new PagingRequest(0, 10000);
    PagingResponse<Usage> resp;
    do {
      resp = uService.list(datasetKey, page);
      for (Usage u : resp.getResults()) {
        jGen.writeObject(u);
      }
      page.nextPage();
    }
    while (!resp.isEndOfRecords());
    jGen.writeEndArray();
    System.out.println("Done.");
    jGen.close();
  }

  private void exportNames(UUID datasetKey, JsonFactory jfactory) throws IOException {
    File json = new File(dir, datasetKey.toString() + "-names.json");
    System.out.println("Start dumping names for dataset " + datasetKey + " to " + json.getAbsolutePath());
    JsonGenerator jGen = jfactory.createJsonGenerator(json, JsonEncoding.UTF8);
    jGen.useDefaultPrettyPrinter();
    jGen.writeStartArray();
    PagingRequest page = new PagingRequest(0, 10000);
    PagingResponse<ParsedName> respPN;
    do {
      respPN = nService.listNames(datasetKey, page);
      for (ParsedName n : respPN.getResults()) {
        jGen.writeObject(n);
      }
      page.nextPage();
    }
    while (!respPN.isEndOfRecords());
    jGen.writeEndArray();
    System.out.println("Done.");
    jGen.close();
  }

  public static void main(String[] args) throws IOException {
    Properties properties = PropertiesUtil.loadProperties("checklistbank.properties");
    Injector inj = Guice.createInjector(new ChecklistBankServiceMyBatisModule(properties));
    UsageService uService = inj.getInstance(UsageService.class);
    ParsedNameService nService = inj.getInstance(ParsedNameService.class);
    NubSourceDataExporter exporter = new NubSourceDataExporter(new File("/Users/mdoering/Desktop"), uService, nService);
    // mammals ca 13.5k
    exporter.export(UUID.fromString("672aca30-f1b5-43d3-8a2b-c1606125fa1b"));
    // IRMNG homonyms ca 90k
    exporter.export(UUID.fromString("714c64e3-2dc1-4bb7-91e4-54be5af4da12"));
    // Toorsa Jigme Dorji Corridor, Plants ca 300
    exporter.export(UUID.fromString("d9f426e7-845c-4a63-be0d-18506f235357"));
    // Gracillariidae Lepidoptera, ca 3k
    exporter.export(UUID.fromString("bb5f507f-f7de-4a5a-ae7f-ad8abbe68bef"));
    // 3i - Cicadellinae, ca 5k
    exporter.export(UUID.fromString("26bca1b5-3ef6-4e97-9672-0058c79185fb"));
    // IOC birds, ca 13k
    exporter.export(UUID.fromString("c696e5ee-9088-4d11-bdae-ab88daffab78"));
    // VASCAN ca 26k
    exporter.export(UUID.fromString("3f8a1297-3259-4700-91fc-acc4170b27ce"));
    // CoL
//    exporter.export(NubGenerator.COL_KEY);
  }

}
