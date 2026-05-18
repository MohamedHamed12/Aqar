CREATE TABLE favorite_listings (
    user_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_favorite_listings PRIMARY KEY (user_id, listing_id),
    CONSTRAINT fk_favorite_listings_user_id
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_favorite_listings_listing_id
        FOREIGN KEY (listing_id)
        REFERENCES listings (id)
        ON DELETE CASCADE
);