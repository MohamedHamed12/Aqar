INSERT INTO users (id, email, password_hash, role, created_at)
VALUES
    (1, 'admin@aqar.test', '$2a$10$demoAdminPasswordHash', 'ADMIN', NOW() - INTERVAL '30 days'),
    (2, 'agent@aqar.test', '$2a$10$demoAgentPasswordHash', 'AGENT', NOW() - INTERVAL '29 days'),
    (3, 'buyer@aqar.test', '$2a$10$demoBuyerPasswordHash', 'USER', NOW() - INTERVAL '28 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO agent_profiles (id, user_id, created_at)
VALUES
    (1, 2, NOW() - INTERVAL '29 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cities (id, name, boundary)
VALUES
    (
        1,
        'Cairo',
        ST_GeomFromText('POLYGON((31.10 30.00,31.10 30.20,31.35 30.20,31.35 30.00,31.10 30.00))', 4326)
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO neighborhoods (id, city_id, name, boundary)
VALUES
    (
        1,
        1,
        'Nasr City',
        ST_GeomFromText('POLYGON((31.22 30.03,31.22 30.10,31.30 30.10,31.30 30.03,31.22 30.03))', 4326)
    ),
    (
        2,
        1,
        'New Cairo',
        ST_GeomFromText('POLYGON((31.35 30.00,31.35 30.10,31.45 30.10,31.45 30.00,31.35 30.00))', 4326)
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO listings (
    id,
    owner_id,
    neighborhood_id,
    title,
    content,
    status,
    purpose,
    type,
    price,
    area_sqm,
    bedrooms,
    location,
    created_at,
    updated_at
)
VALUES
    (
        1,
        2,
        1,
        'Sunny Apartment in Nasr City',
        'Modern apartment close to services and public transport.',
        'ACTIVE',
        'SALE',
        'APARTMENT',
        2500000,
        125,
        3,
        ST_GeomFromText('POINT(31.30 30.08)', 4326),
        NOW() - INTERVAL '10 days',
        NOW() - INTERVAL '2 days'
    ),
    (
        2,
        2,
        2,
        'Compact Studio in New Cairo',
        'Ideal for students or first-time buyers looking for a small footprint.',
        'ACTIVE',
        'RENT',
        'STUDIO',
        18000,
        45,
        1,
        ST_GeomFromText('POINT(31.40 30.05)', 4326),
        NOW() - INTERVAL '9 days',
        NOW() - INTERVAL '1 day'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO listing_images (id, listing_id, dropbox_path, thumbnail_path, display_order, created_at)
VALUES
    (1, 1, '/Apps/Aqar/demo/listings/1/hero.jpg', '/Apps/Aqar/demo/thumbnails/1/hero.jpg', 0, NOW() - INTERVAL '10 days'),
    (2, 1, '/Apps/Aqar/demo/listings/1/living-room.jpg', '/Apps/Aqar/demo/thumbnails/1/living-room.jpg', 1, NOW() - INTERVAL '10 days'),
    (3, 2, '/Apps/Aqar/demo/listings/2/hero.jpg', '/Apps/Aqar/demo/thumbnails/2/hero.jpg', 0, NOW() - INTERVAL '9 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO price_history (id, listing_id, price, recorded_at)
VALUES
    (1, 1, 2450000, NOW() - INTERVAL '14 days'),
    (2, 1, 2500000, NOW() - INTERVAL '2 days'),
    (3, 2, 17500, NOW() - INTERVAL '7 days'),
    (4, 2, 18000, NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

INSERT INTO favorite_listings (user_id, listing_id, created_at)
VALUES
    (3, 1, NOW() - INTERVAL '1 day'),
    (3, 2, NOW() - INTERVAL '1 day')
ON CONFLICT (user_id, listing_id) DO NOTHING;

-- Sync sequences after seeding with explicit IDs so new rows don't collide
SELECT setval('users_id_seq',            (SELECT COALESCE(MAX(id), 0) FROM users));
SELECT setval('agent_profiles_id_seq',   (SELECT COALESCE(MAX(id), 0) FROM agent_profiles));
SELECT setval('cities_id_seq',           (SELECT COALESCE(MAX(id), 0) FROM cities));
SELECT setval('neighborhoods_id_seq',    (SELECT COALESCE(MAX(id), 0) FROM neighborhoods));
SELECT setval('listings_id_seq',         (SELECT COALESCE(MAX(id), 0) FROM listings));
SELECT setval('listing_images_id_seq',   (SELECT COALESCE(MAX(id), 0) FROM listing_images));
SELECT setval('price_history_id_seq',    (SELECT COALESCE(MAX(id), 0) FROM price_history));