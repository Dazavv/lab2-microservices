CREATE TABLE IF NOT EXISTS users(
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(20) NOT NULL,
                       name VARCHAR(20) NOT NULL,
                       surname VARCHAR(30) NOT NULL
);

