CREATE TABLE IF NOT EXISTS group_events (
                              id BIGSERIAL PRIMARY KEY,
                              name VARCHAR(30) NOT NULL,
                              description VARCHAR(200),
                              date DATE,
                              start_time TIME,
                              end_time TIME,
                              owner_id BIGINT,
                              status VARCHAR(20)
);

CREATE TABLE group_event_participants (
                                          group_event_id BIGINT NOT NULL REFERENCES group_events(id) ON DELETE CASCADE,
                                          participant_id BIGINT NOT NULL
);
