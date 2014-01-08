
CREATE TABLE col_annotation (
    nub_fk integer NOT NULL,
    gsd text,
    annotated_name text,
    rejected boolean,
    status text,
    note text,
    PRIMARY KEY (nub_fk)
);