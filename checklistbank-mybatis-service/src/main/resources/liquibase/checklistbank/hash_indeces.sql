drop index citation_hashtext_idx;
create index on citation using hash (citation);

drop index identifier_hashtext_idx;
create index on identifier using hash (identifier);
