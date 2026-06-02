ALTER TABLE channels ADD COLUMN test_status VARCHAR(30);
ALTER TABLE channels ADD COLUMN test_http_status INTEGER;
ALTER TABLE channels ADD COLUMN test_message VARCHAR(255);
ALTER TABLE channels ADD COLUMN last_test_at TIMESTAMP;

CREATE INDEX idx_channels_test_status ON channels(test_status);
