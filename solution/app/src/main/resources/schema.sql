CREATE TABLE IF NOT EXISTS config (
    id integer constraint config_pk primary key NOT NULL,
    value varchar(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS metric_history (
    date int NOT NULL ,
    metric_name varchar(255) NOT NULL,
    amount decimal(12,2) DEFAULT 0.00,
    PRIMARY KEY (metric_name, date)
);
INSERT INTO config (id, value) values (1, 0) ON CONFLICT DO NOTHING;
CREATE TABLE IF NOT EXISTS clients (
    id uuid constraint clients_pk primary key NOT NULL,
    login varchar(255) NOT NULL,
    age integer NOT NULL,
    location varchar(255) NOT NULL,
    gender smallint NOT NULL
);
CREATE TABLE IF NOT EXISTS advertisers (
    id uuid constraint advertisers_pk primary key,
    name varchar(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS mlscore (
    client_id uuid NOT NULL,
    advertiser_id uuid NOT NULL,
    PRIMARY KEY (client_id, advertiser_id),
    ml_score integer DEFAULT 0
);
CREATE TABLE IF NOT EXISTS filtered_campaigns (
    campaign_id uuid DEFAULT gen_random_uuid (),
    PRIMARY KEY (campaign_id),
    advertiser_id uuid NOT NULL,
    impressions_limit integer NOT NULL,
    clicks_limit integer NOT NULL,
    cost_per_impression decimal(12,2) NOT NULL,
    cost_per_click decimal(12,2) NOT NULL,
    ad_title varchar(255) NOT NULL,
    ad_text text NOT NULL,
    start_date integer NOT NULL,
    end_date integer NOT NULL,
    gender smallint,
    age_from integer,
    age_to integer,
    location varchar(255),
    is_new boolean NOT NULL
);
CREATE TABLE IF NOT EXISTS campaigns (
    campaign_id uuid DEFAULT gen_random_uuid (),
    PRIMARY KEY (campaign_id),
    advertiser_id uuid NOT NULL,
    impressions_limit integer NOT NULL,
    clicks_limit integer NOT NULL,
    cost_per_impression decimal(12,2) NOT NULL,
    cost_per_click decimal(12,2) NOT NULL,
    ad_title varchar(255) NOT NULL,
    ad_text text NOT NULL,
    start_date integer NOT NULL,
    end_date integer NOT NULL,
    gender smallint,
    age_from integer,
    age_to integer,
    location varchar(255),
    has_image boolean default false,
    v_age_from integer GENERATED ALWAYS AS (coalesce(age_from, 0)) STORED,
    v_age_to integer GENERATED ALWAYS AS (coalesce(age_to, 200)) STORED,
    v_gender smallint GENERATED ALWAYS AS (coalesce(gender, 2)) STORED,
    v_impression_danger_limit integer GENERATED ALWAYS AS (floor(coalesce(impressions_limit, 0)::float / 100 * 104)) STORED
);
CREATE TABLE IF NOT EXISTS stat_records (
    id integer generated always as identity,
    advertiser_id uuid NOT NULL,
    campaign_id uuid NOT NULL,
    date integer NOT NULL,
    type smallint NOT NULL,
    spent decimal(12,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS redeem_records (
    client_id uuid NOT NULL ,
    campaign_id uuid NOT NULL ,
    PRIMARY KEY (client_id, campaign_id)
);
CREATE TABLE IF NOT EXISTS show_records (
    client_id uuid NOT NULL,
    campaign_id uuid NOT NULL,
    PRIMARY KEY (client_id, campaign_id)
);

-- Campaigns table indexes
CREATE INDEX IF NOT EXISTS idx_campaigns_dates
ON campaigns (start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_campaigns_targeting
ON campaigns (v_age_from, v_age_to, location, v_gender);

CREATE INDEX IF NOT EXISTS idx_campaigns_advertiser
ON campaigns (advertiser_id);

-- Show records indexes
CREATE INDEX IF NOT EXISTS idx_show_records_campaign
ON show_records (campaign_id);

CREATE INDEX IF NOT EXISTS idx_show_records_client
ON show_records (client_id, campaign_id);

-- Redeem records indexes
CREATE INDEX IF NOT EXISTS idx_redeem_records_campaign
ON redeem_records (campaign_id);

-- ML Score indexes
CREATE INDEX IF NOT EXISTS idx_mlscore_combined
ON mlscore (client_id, advertiser_id);

-- Previous indexes
CREATE INDEX IF NOT EXISTS idx_campaigns_dates
ON campaigns (start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_campaigns_targeting
ON campaigns (v_age_from, v_age_to, location, v_gender);

CREATE INDEX IF NOT EXISTS idx_campaigns_advertiser
ON campaigns (advertiser_id);

-- Modified indexes for show_records with included columns for aggregations
CREATE INDEX IF NOT EXISTS idx_show_records_campaign_covering
ON show_records (campaign_id)
INCLUDE (client_id);

-- Modified indexes for redeem_records with included columns
CREATE INDEX IF NOT EXISTS idx_redeem_records_campaign_covering
ON redeem_records (campaign_id)
INCLUDE (client_id);

-- Index for campaigns cost calculations
CREATE INDEX IF NOT EXISTS idx_campaigns_costs
ON campaigns (campaign_id)
INCLUDE (cost_per_impression, cost_per_click);

-- Composite index for mlscore including the score for window functions
CREATE INDEX IF NOT EXISTS idx_mlscore_score
ON mlscore (client_id, advertiser_id)
INCLUDE (ml_score);

-- Optional: If you need to optimize window functions
CREATE INDEX IF NOT EXISTS idx_campaigns_revenue_calc
ON campaigns (campaign_id)
INCLUDE (cost_per_impression, cost_per_click, impressions_limit, clicks_limit);