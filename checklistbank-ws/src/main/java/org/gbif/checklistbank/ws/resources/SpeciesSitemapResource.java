/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.NameUsageMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * Species sitemap resource producing text sitemaps for all checklist bank name usages.
 * As sitemaps are limited to a maximum of 50k entries a sitemap index file is also dynamically created.
 *
 * see https://www.sitemaps.org/protocol.html
 */
@RestController
@RequestMapping(
  value = "/sitemap/species",
  produces = {MediaType.APPLICATION_JSON_VALUE, "application/x-javascript"}
)
public class SpeciesSitemapResource {

    private static final Logger LOG = LoggerFactory.getLogger(SpeciesSitemapResource.class);
    private static final int SITEMAP_SIZE = 50000;
    private static final String INDEX_TEMPLATE = "sitemapindex.ftl";
    private static final String TEMPLATE_PATH = "/templates";
    private static final Configuration FTL = provideFreemarker();

    private final NameUsageMapper nameUsageMapper;
    private final String portalUrl;
    private final String apiUrl;


    /**
     * Provides a freemarker template loader. It is configured to access the utf8 templates folder on the classpath, i.e.
     * /src/resources/templates
     */
    private static Configuration provideFreemarker() {
        // load templates from classpath by prefixing /templates
        TemplateLoader tl = new ClassTemplateLoader(SpeciesSitemapResource.class, TEMPLATE_PATH);

        Configuration fm = new Configuration(Configuration.VERSION_2_3_25);
        fm.setDefaultEncoding("utf8");
        fm.setTemplateLoader(tl);

        return fm;
    }

    @Autowired
    public SpeciesSitemapResource(NameUsageMapper nameUsageMapper, @Value("checklistbank.portal.url") String portalUrl, @Value("checklistbank.api.url") String apiUrl) {
        this.nameUsageMapper = nameUsageMapper;
        this.portalUrl = portalUrl;
        this.apiUrl = apiUrl;
    }

    /**
     * Generate a sitemap index to all sitemaps.
     */
    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapIndex() throws IOException {
        int cnt = nameUsageMapper.count(null);
        int maps = (int) Math.ceil((double) cnt/SITEMAP_SIZE);
        LOG.info("Requested sitemap index to {} index files with {} usages", maps, cnt);

        try (Writer writer = new StringWriter()) {
            Map<String, Object> data = Maps.newHashMap();
            data.put("apiUrl", apiUrl);
            data.put("maps", maps);
            data.put("cnt", cnt);
            FTL.getTemplate(INDEX_TEMPLATE).process(data, writer);
            return writer.toString();

        } catch (TemplateException e) {
            throw new IOException("Error while processing the sitemapindex template", e);
        }
    }

    /**
     * Generate a single text sitemap with 50k entries.
     */
    @GetMapping(path= "{page}")
    public ResponseEntity<StreamingResponseBody> sitemap(@PathVariable("page") int page) {
        Preconditions.checkArgument(page > 0, "Page parameter must be positive");

        StreamingResponseBody stream = os -> {
            Writer writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            PagingRequest req = new PagingRequest((long) (page - 1) * SITEMAP_SIZE, SITEMAP_SIZE);
            for (int key : nameUsageMapper.list(null, req)) {
                writer.write(portalUrl);
                writer.write(String.valueOf(key));
                writer.write("\n");
            }
            writer.flush();
        };

        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(stream);
    }

}
