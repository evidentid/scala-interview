CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

/* START RATES_PROVIDERS MODEL */
CREATE TABLE rates_providers(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_name TEXT NOT NULL,
    currency_code TEXT NOT NULL,
    url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (currency_code, url)
);

/* Archive Table */
CREATE TABLE archived_rates_providers AS TABLE rates_providers WITH NO DATA;
ALTER TABLE archived_rates_providers
    ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

/* Archive trigger function */
CREATE FUNCTION archive_rate_provider() RETURNS trigger AS $$
BEGIN
    INSERT INTO archived_rates_providers VALUES((OLD).*);
    RETURN OLD;
END
$$ language plpgsql;

/* Setup archive on delete trigger */
CREATE TRIGGER archiveRateProvider
    BEFORE DELETE ON rates_providers
    FOR EACH ROW EXECUTE PROCEDURE archive_rate_provider();

/* END RATES_PROVIDERS MODEL */

INSERT INTO rates_providers (provider_name, currency_code, url) VALUES ('Test provider', 'TEST', 'http://test.com/TEST');

-- 1. Single provider per currency (uncomment below)
-- INSERT INTO rates_providers (provider_name, currency_code, url) VALUES
--     ('provider_usd', 'USD', 'http://provider_usd_1.com/USD'),
--     ('provider_eur', 'EUR', 'http://provider_eur_1.com/EUR'),
--     ('provider_pln', 'PLN', 'http://provider_eur_2.com/PLN');

-- 2. Multiple providers per currency (uncomment below and comment above)
-- INSERT INTO rates_providers (provider_name, currency_code, url) VALUES
--     ('provider_usd_1', 'USD', 'http://provider_usd_1.com/USD'),
--     ('provider_usd_2', 'USD', 'http://provider_usd_2.com/USD'),
--     ('provider_usd_3', 'USD', 'http://provider_usd_3.com/USD'),
--     ('provider_eur_3', 'EUR', 'http://provider_eur_1.com/USD'),
--     ('provider_eur_3', 'EUR', 'http://provider_eur_2.com/USD'),
--     ('provider_pln_1', 'PLN', 'http://provider_pln_1.com/USD');
