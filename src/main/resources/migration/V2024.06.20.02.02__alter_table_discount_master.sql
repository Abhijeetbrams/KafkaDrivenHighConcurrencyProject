ALTER TABLE discount_master
RENAME COLUMN discount_percentage TO freight_discount_percentage;

ALTER TABLE discount_master
ALTER COLUMN freight_discount_percentage TYPE DOUBLE PRECISION
USING freight_discount_percentage::DOUBLE PRECISION;

ALTER TABLE discount_master
ADD COLUMN IF NOT EXISTS cod_discount_percentage DOUBLE PRECISION;

ALTER TABLE discount_master
ADD COLUMN IF NOT EXISTS activation_status BOOLEAN;
