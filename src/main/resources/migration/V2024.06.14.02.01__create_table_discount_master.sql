CREATE TABLE IF NOT EXISTS discount_master(
    id character varying NOT NULL PRIMARY KEY,
    discount_client_id VARCHAR(255),
    discount_percentage VARCHAR(255),
    applied_on VARCHAR(255),
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    created_by character varying(255),
    modified_by character varying(255),
    created_by_org character varying(255),
    modified_by_org character varying(255),
    CONSTRAINT discount_client_id_unique UNIQUE(discount_client_id)
);