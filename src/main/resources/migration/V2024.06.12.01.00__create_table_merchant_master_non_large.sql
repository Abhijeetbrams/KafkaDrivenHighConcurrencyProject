CREATE TABLE IF NOT EXISTS merchant_master_non_large (
    id character varying NOT NULL PRIMARY KEY,
    client_id VARCHAR(255),
    client_name VARCHAR(255),
    merchant_state VARCHAR(255),
    rvp_surcharges INT,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    created_by character varying(255),
    modified_by character varying(255),
    created_by_org character varying(255),
    modified_by_org character varying(255),
    CONSTRAINT client_id_unique UNIQUE(client_id)
);
