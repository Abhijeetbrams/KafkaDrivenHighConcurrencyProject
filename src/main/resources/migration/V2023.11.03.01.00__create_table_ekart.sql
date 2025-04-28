

CREATE TABLE IF NOT EXISTS ekart
(
    tracking_id character varying NOT NULL PRIMARY KEY,
    client_id character varying NOT NULL,
    merchant_id character varying NOT NULL,
    is_processed boolean DEFAULT false,
    data text NOT NULL,
    is_updated boolean DEFAULT false,
    created timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    created_by character varying(255),
    modified_by character varying(255),
    created_by_org character varying(255),
    modified_by_org character varying(255)
)

