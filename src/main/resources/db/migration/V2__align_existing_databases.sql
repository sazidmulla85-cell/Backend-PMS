SET @auto_renew_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'property'
    AND column_name = 'auto_renew'
);

SET @add_auto_renew_sql = IF(
  @auto_renew_exists = 0,
  'ALTER TABLE property ADD COLUMN auto_renew BOOLEAN NULL',
  'SELECT 1'
);

PREPARE add_auto_renew_statement FROM @add_auto_renew_sql;
EXECUTE add_auto_renew_statement;
DEALLOCATE PREPARE add_auto_renew_statement;

UPDATE property
SET auto_renew = TRUE
WHERE auto_renew IS NULL;

ALTER TABLE property
  MODIFY COLUMN auto_renew BOOLEAN NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS password_reset_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  used_at TIMESTAMP(6) NULL,
  user_account_id BIGINT NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_password_reset_token_hash (token_hash),
  CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_account_id) REFERENCES user_account(id)
) ENGINE=InnoDB;
