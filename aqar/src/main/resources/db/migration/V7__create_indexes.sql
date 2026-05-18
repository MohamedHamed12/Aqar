CREATE INDEX idx_listings_location_gist
    ON listings
    USING GIST (location);

CREATE INDEX idx_listings_status_purpose_type_price_bedrooms
    ON listings (status, purpose, type, price, bedrooms);

CREATE INDEX idx_listings_title_trgm
    ON listings
    USING GIN (title gin_trgm_ops);

CREATE INDEX idx_listings_content_arabic_fts
    ON listings
    USING GIN (to_tsvector('arabic', COALESCE(content, '')));

CREATE INDEX idx_listings_owner_id
    ON listings (owner_id);

CREATE INDEX idx_listings_neighborhood_id
    ON listings (neighborhood_id);