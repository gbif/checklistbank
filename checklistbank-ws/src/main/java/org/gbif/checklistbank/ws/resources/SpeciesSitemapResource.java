package org.gbif.checklistbank.ws.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.Map;

/**
 * Species sitemap resource producing text sitemaps for all checklist bank name usages.
 * As sitemaps are limited to a maximum of 50k entries a sitemap index file is also dynamically created.
 *
 * see https://www.sitemaps.org/protocol.html
 */
@Path("/sitemap/species")
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

    @Inject
    public SpeciesSitemapResource(NameUsageMapper nameUsageMapper, @Named("checklistbank.portal.url") String portalUrl, @Named("checklistbank.api.url") String apiUrl) {
        this.nameUsageMapper = nameUsageMapper;
        this.portalUrl = portalUrl;
        this.apiUrl = apiUrl;
    }

    /**
     * Generate a sitemap index to all sitemaps.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
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
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{page}")
    public Response sitemap(@PathParam("page") int page) {
        Preconditions.checkArgument(page > 0, "Page parameter must be positive");

        StreamingOutput stream = os -> {
            Writer writer = new BufferedWriter(new OutputStreamWriter(os, Charsets.UTF_8));
            PagingRequest req = new PagingRequest((page - 1) * SITEMAP_SIZE, SITEMAP_SIZE);
            for (int key : nameUsageMapper.list(null, req)) {
                writer.write(portalUrl);
                writer.write(key);
                writer.write("\n");
            }
            writer.flush();
        };

        return Response.ok(stream).build();
    }

}
