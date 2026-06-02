CREATE TABLE channels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stream_url TEXT NOT NULL,
    group_title VARCHAR(255),
    logo_url TEXT,
    tvg_id VARCHAR(255),
    tvg_name VARCHAR(255),
    duration VARCHAR(50),
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    iptv_list_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_channels_iptv_list
        FOREIGN KEY (iptv_list_id) REFERENCES iptv_lists(id),
    CONSTRAINT fk_channels_owner
        FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE INDEX idx_channels_name ON channels(name);
CREATE INDEX idx_channels_group_title ON channels(group_title);
CREATE INDEX idx_channels_iptv_list_id ON channels(iptv_list_id);
CREATE INDEX idx_channels_owner_id ON channels(owner_id);
