package org.gbif.checklistbank.col;

import org.gbif.checklistbank.model.ColAnnotation;
import org.gbif.checklistbank.service.ColAnnotationService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.Archive;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class to run a full import of CoL GSD annotations for the GBIF Backbone, populating the col_annotation table
 * in Checklist Bank.
 * @see <a href="http://dev.gbif.org/issues/browse/CLB-248">CLB-248</a>
 */
public class ColAnnotationImport {
  private static final Logger LOG = LoggerFactory.getLogger(ColAnnotationImport.class);
  private Properties props;
  private ColAnnotationService annotationService;
  @VisibleForTesting
  protected Pattern ANNOTATION_SPLITTER = Pattern.compile("^\\s*(.*)\\s*\\|\\s*(.*)\\s*\\|\\s*(.*)\\s*$");

  public ColAnnotationImport(String propsFile) throws IOException {
    props = PropertiesUtil.readFromFile(propsFile);
    // init guice
    ChecklistBankServiceMyBatisModule myBatisModule = new ChecklistBankServiceMyBatisModule(props);
    Injector inj = Guice.createInjector(myBatisModule);
    annotationService = inj.getInstance(ColAnnotationService.class);
  }

  public void importAnnotations() throws IOException, URISyntaxException {
    Archive arch = downloadArchive();
    int counter = 0;
    int failures = 0;
    int empty = 0;
    for (StarRecord star : arch) {
      Record rec = star.core();
      counter++;
      try {
        int nubKey = counter;//TODO: use taxonIDs once available: Integer.parseInt(rec.value(DwcTerm.taxonID));
        Matcher m = ANNOTATION_SPLITTER.matcher(rec.value(DwcTerm.taxonRemarks));
        if (m.find()) {
          ColAnnotation annotation = new ColAnnotation(nubKey, rec.value(DwcTerm.datasetName), rec.value(DwcTerm.scientificName),
            isRejected(m.group(1)), m.group(3), m.group(2));
          annotationService.insertAnnotation(annotation);

        } else {
          empty++;
          LOG.warn("No annotation given for {}", rec.value(DwcTerm.scientificName));
        }
      } catch (NumberFormatException e) {
        failures++;
        LOG.warn("No valid taxonID given for {}", rec.value(DwcTerm.scientificName));
      }
    }
    LOG.info("{} annotations processed, {} lack notes, {} failed to import", counter, empty, failures);
  }

  @VisibleForTesting
  protected static boolean isRejected(String annotation) {
    if (annotation.toLowerCase().startsWith("placed")) {
      return false;
    }
    return true;
  }

  private Archive downloadArchive() throws IOException, URISyntaxException {
    // download url
    final String url = props.getProperty("col.annotation.url");
    // local work dir
    File workDir = FileUtils.createTempDir();
    workDir.deleteOnExit();
    // local zip file
    File zip = new File(workDir, "annotation.zip");
    // local decompressed dwca
    File dwca = new File(workDir, "annotation");
    // insert folders
    org.apache.commons.io.FileUtils.forceMkdir(dwca);

    // use a 10 minutes timeout
    HttpClient client = HttpUtil.newMultithreadedClient(600000, 10, 10);
    // authentication
    HttpContext authContext = new BasicHttpContext();
    URI authUri = new URI(url);
    AuthScope scope = new AuthScope(authUri.getHost(), AuthScope.ANY_PORT, AuthScope.ANY_REALM);

    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(scope, new UsernamePasswordCredentials(props.getProperty("col.annotation.user"), props.getProperty("col.annotation.password")));
    authContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);

    HttpGet get = new HttpGet(url);
    HttpResponse response = client.execute(get, authContext);

    if (response.getStatusLine().getStatusCode() != 200) {
      LOG.error("{} error downloading annotations from {}: {}", response.getStatusLine(), url,
        response.getStatusLine().getReasonPhrase());
      System.exit(1);
    }

    HttpEntity entity = response.getEntity();
    if (entity != null) {
      // copy stream to local file
      OutputStream fos = new FileOutputStream(zip, false);
      try {
        entity.writeTo(fos);
      } finally {
        fos.close();
      }
    }

    LOG.info("Successfully downloaded {} to {}", url, zip.getAbsolutePath());

    // open archive
    Archive arch = DwcFiles.fromCompressed(zip.toPath(), dwca.toPath());
    return arch;
  }

  public static void main (String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Pass the full path to the config properties file as first argument please");
    }
    ColAnnotationImport imp = new ColAnnotationImport(args[0]);
    imp.importAnnotations();
  }

}
