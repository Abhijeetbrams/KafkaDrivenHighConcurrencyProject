


CREATE TABLE IF NOT EXISTS pincode_master
(
    id character varying NOT NULL PRIMARY KEY,
    pincode character varying NOT NULL,
    state character varying,
    zstate character varying,
    zone character varying,
    tier character varying,
    active boolean DEFAULT false,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    created_by character varying(255),
    modified_by character varying(255),
    created_by_org character varying(255),
    modified_by_org character varying(255),
    city character varying,
    CONSTRAINT pincode_unique UNIQUE(pincode)
)

