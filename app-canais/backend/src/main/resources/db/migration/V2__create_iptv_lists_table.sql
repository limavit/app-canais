CREATE TABLE iptv_lists (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    source_type VARCHAR(20) NOT NULL,
    source_url TEXT,
    original_file_name VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    total_channels INTEGER DEFAULT 0,
    last_import_at TIMESTAMP,
    error_message TEXT,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_iptv_lists_owner
        FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE INDEX idx_iptv_lists_owner_id ON iptv_lists(owner_id);
