

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS fwd_month integer;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS pos_month integer;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS rto_month integer;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS rvp_month integer;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS direct_delivered_month integer;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS id text;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS int_file_upload_time timestamp without time zone;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS int_file_last_retry_time timestamp without time zone;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS int_file_failed_reason text;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS int_file_invocation_id text;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS int_file_status text;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS int_file_filename text;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS int_file_retry_count integer;

ALTER TABLE ekart
ADD COLUMN IF NOT EXISTS rate_updated boolean DEFAULT false;