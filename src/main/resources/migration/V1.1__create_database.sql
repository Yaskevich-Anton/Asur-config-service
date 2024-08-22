CREATE TABLE IF NOT EXISTS regions(
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS users(
    id SERIAL PRIMARY KEY,
    region_fk INTEGER REFERENCES regions(id),
    name VARCHAR(32) NOT NULL,
    last_name VARCHAR(32) NOT NULL,
    middle_name VARCHAR(32),
    email VARCHAR(32) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    position VARCHAR(128) NOT NULL,
    manager_user_fk INTEGER REFERENCES users(id),
    region_div_fk INTEGER,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS region_divisions(
    id SERIAL PRIMARY KEY,
    short_name VARCHAR(128) NOT NULL,
    long_name VARCHAR(256) NOT NULL,
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    lead_user_fk INTEGER REFERENCES users(id),
    parent_division_fk INTEGER REFERENCES region_divisions(id)
);

CREATE TABLE IF NOT EXISTS user_avatars(
    id INT PRIMARY KEY REFERENCES users(id),
    avatar BYTEA,
    ext VARCHAR(4)
);

CREATE TABLE IF NOT EXISTS user_pwd_requests(
    id SERIAL PRIMARY KEY,
    creator_user_fk INTEGER NOT NULL REFERENCES users(id),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_done BOOLEAN NOT NULL
);

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_region_div_fk_fkey;
ALTER TABLE users ADD CONSTRAINT users_region_div_fk_fkey FOREIGN KEY (region_div_fk) REFERENCES region_divisions(id);