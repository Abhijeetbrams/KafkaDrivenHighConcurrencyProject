CREATE TABLE IF NOT EXISTS weight_related(
    id character varying NOT NULL PRIMARY KEY,
    client_id VARCHAR(255),
    reason VARCHAR(255),
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    created_by character varying(255),
    modified_by character varying(255),
    created_by_org character varying(255),
    modified_by_org character varying(255)
);