CREATE TABLE IF NOT EXISTS events (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(20) NOT NULL,
                        description VARCHAR(200),
                        date DATE NOT NULL,
                        start_time TIME NOT NULL,
                        end_time TIME NOT NULL,
                        owner_id BIGINT
);
