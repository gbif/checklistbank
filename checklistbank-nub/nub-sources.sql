CREATE VIEW nub_sources AS 
SELECT d.key as dataset_key, t.value::int as priority, ranks.value as rank, d.title 
 FROM dataset d 
  JOIN dataset_machine_tag dmt on dmt.dataset_key=d.key JOIN machine_tag t on dmt.machine_tag_key=t.key
  LEFT JOIN 
  (select dataset_key, value from dataset_machine_tag JOIN machine_tag on machine_tag_key=key WHERE namespace='nub.gbif.org' and name='rankLimit') as ranks on ranks.dataset_key=d.key
 WHERE t.namespace='nub.gbif.org' and t.name='priority'
 ORDER BY priority;

