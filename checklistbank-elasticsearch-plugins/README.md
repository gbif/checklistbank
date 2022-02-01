# ChecklistBank Solr Plugins

This small module provides a shaded jar for custom solr/lucene filters, analyzers and their factory
to be used in solr schema configurations.

## Installation
To use the custom classes in Solr you should copy the shaded jar to one of the already configured lib folders of solr.

For solr cloud installations in GBIF the jar needs to be copied to each node of the cluster as we also make use
of the solr schema during batch index build using an oozie workflow. You can use the script ```bin/copylibs.sh``` to
scp the jar to all nodes of a given GBIF cluster, e.g ```./copylibs.sh uat 2.46``` 
to copy the version 2.46 of the shaded jar to all uat/prod cluster nodes in GBIF.
 
 