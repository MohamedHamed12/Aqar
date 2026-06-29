DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'refresh_tokens' AND column_name = 'token_hash'
  ) THEN
    ALTER TABLE refresh_tokens RENAME COLUMN token_hash TO token;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refresh_tokens' AND column_name = 'family_id') THEN
    ALTER TABLE refresh_tokens DROP COLUMN family_id;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refresh_tokens' AND column_name = 'revoked_at') THEN
    ALTER TABLE refresh_tokens DROP COLUMN revoked_at;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refresh_tokens' AND column_name = 'replaced_by_token_id') THEN
    ALTER TABLE refresh_tokens DROP COLUMN replaced_by_token_id;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refresh_tokens' AND column_name = 'updated_at') THEN
    ALTER TABLE refresh_tokens DROP COLUMN updated_at;
  END IF;
END $$;

DROP INDEX IF EXISTS idx_refresh_tokens_token_hash;
DROP INDEX IF EXISTS idx_refresh_tokens_family_id;

ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS uq_refresh_tokens_token_hash;

ALTER TABLE refresh_tokens ALTER COLUMN token TYPE VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens (token);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uq_refresh_tokens_token'
  ) THEN
    ALTER TABLE refresh_tokens ADD CONSTRAINT uq_refresh_tokens_token UNIQUE (token);
  END IF;
END $$;
