package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.Citation;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.utils.text.StringUtils;

import java.util.UUID;

import org.junit.Before;

public class NameUsageComponentMapperITBase<T extends NameUsageComponentMapper> extends MapperITBase<T> {
    final protected String datasetTitle = "My Favorite Checklist";
    final protected String citation1 = "citeme one";
    protected int citationKey1;
    final protected String citation2 = "citeme two";
    final protected String citation2doi = "doi:10.10003/citeme two";
    final protected String citation2link = "http://purl.org/citeme/two";
    protected int citationKey2;
    protected int usageKey;
    protected int nubKey;
    protected UUID datasetKey;

    private ParsedNameMapper parsedNameMapper;
    private NameUsageMapper nameUsageMapper;
    private NubRelMapper nubRelMapper;
    private DatasetMapper datasetMapper;
    private CitationMapper citationMapper;

    public NameUsageComponentMapperITBase(Class<T> mapperClass) {
        super(mapperClass);
    }

    @Before
    public void initMapper() throws Exception {
        parsedNameMapper = getInstance(ParsedNameMapper.class);
        nameUsageMapper = getInstance(NameUsageMapper.class);
        nubRelMapper = getInstance(NubRelMapper.class);
        datasetMapper = getInstance(DatasetMapper.class);
        citationMapper = getInstance(CitationMapper.class);

        ParsedName pn = new ParsedName();
        pn.setType(NameType.SCIENTIFIC);
        pn.setScientificName(StringUtils.randomSpecies());
        parsedNameMapper.create(pn, pn.getScientificName());
        final int nameKey = pn.getKey();

        datasetKey = UUID.randomUUID();
        datasetMapper.insert(datasetKey, datasetTitle);

        NameUsageWritable uw = new NameUsageWritable();
        uw.setNameKey(nameKey);
        uw.setDatasetKey(datasetKey);
        uw.setOrigin(Origin.SOURCE);
        uw.setRank(Rank.SPECIES);
        uw.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
        nameUsageMapper.insert(uw);
        usageKey = uw.getKey();

        NameUsageWritable nub = new NameUsageWritable();
        nub.setNameKey(nameKey);
        nub.setDatasetKey(Constants.NUB_DATASET_KEY);
        nub.setOrigin(Origin.SOURCE);
        nub.setRank(Rank.SPECIES);
        nub.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
        nameUsageMapper.insert(nub);
        nubKey = nub.getKey();

        nubRelMapper.insert(datasetKey, usageKey, nubKey);

        Citation c = new Citation(citation1);
        citationMapper.insert(c);
        citationKey1 = c.getKey();

        c = new Citation(citation2);
        c.setDoi(citation2doi);
        c.setLink(citation2link);
        citationMapper.insert(c);
        citationKey2 = c.getKey();
    }

}
