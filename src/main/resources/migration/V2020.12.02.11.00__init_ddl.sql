

CREATE TABLE IF NOT EXISTS variable (
	id varchar(255) NOT NULL,
	created timestamp NOT NULL,
	modified timestamp NOT NULL,
        created_by varchar(255) NULL,
	modified_by varchar(255) NULL,
        created_by_org varchar(255) NULL,
	modified_by_org varchar(255) NULL,
        info jsonb,
        notes varchar(2048) NULL,
        "key" varchar(255) NOT NULL,
        "value" varchar(2048) NULL,
        CONSTRAINT uk_key UNIQUE ("key","created_by_org"),
        CONSTRAINT pk_variable PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_org ON variable (created_by_org);
CREATE INDEX IF NOT EXISTS idx_org_user ON variable (created_by_org,created_by);