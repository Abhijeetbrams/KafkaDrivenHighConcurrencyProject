CREATE TABLE IF NOT EXISTS state_client_tax_master(
    id character varying NOT NULL PRIMARY KEY,
    client_code VARCHAR(255),
    state VARCHAR(700),
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    created_by character varying(255),
    modified_by character varying(255),
    created_by_org character varying(255),
    modified_by_org character varying(255),
    CONSTRAINT client_code_unique UNIQUE(client_code)
);
