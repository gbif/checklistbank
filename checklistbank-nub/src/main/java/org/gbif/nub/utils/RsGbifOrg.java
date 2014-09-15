package org.gbif.nub.utils;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class knowing the url layout of rs.gbif.org to access authority and dictionary files.
 */
public class RsGbifOrg {
  private static final Logger LOG = LoggerFactory.getLogger(RsGbifOrg.class);
  private static final Joiner PATH_JOINER = Joiner.on("/").skipNulls();
  private static final String DICT_DIR = "dictionaries";
  public static final String DOMAIN = "http://rs.gbif.org/";
  public static final String FILENAME_BLACKLIST = "blacklisted.txt";

  /**
   * @param path given as array of individual names that will be concatenated
   * @return url to file inside rs.gbif.org
   */
  public static URL url(String ... path) {
    try {
      if (path == null){
        return new URL(DOMAIN);
      }
      return new URL(DOMAIN + PATH_JOINER.join(path));
    } catch (MalformedURLException e) {
      LOG.error("Cannot insert rs.gbif.org url for path " + PATH_JOINER.join(path), e);
    }
    return null;
  }

  /**
  * @param filename of dictionary file requested
  * @return url to file inside to dictionary folder of rs.gbif.org
  */
  public static URL dictionaryUrl(String filename) {
    return url(DICT_DIR, filename);
  }

  /**
   * @param filename of authority dictionary file requested
   * @return url to file inside to authority folder of rs.gbif.org
   */
  public static URL authorityUrl(String filename) {
    return url(DICT_DIR, "authority", filename);
  }

  /**
   * @param filename of synonyms file requested
   * @return url to file inside to synonyms dictionary folder of rs.gbif.org
   */
  public static URL synonymUrl(String filename) {
    return url(DICT_DIR, "synonyms", filename);
  }
}
