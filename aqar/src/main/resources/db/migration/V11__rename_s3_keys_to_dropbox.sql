ALTER TABLE listing_images RENAME COLUMN s3_key TO dropbox_path;
ALTER TABLE listing_images RENAME COLUMN thumbnail_key TO thumbnail_path;
