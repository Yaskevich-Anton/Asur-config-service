CREATE TABLE IF NOT EXISTS d_fact_names(
    id BIGSERIAL PRIMARY KEY,
    modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_since TIMESTAMP NOT NULL,
    actual_until TIMESTAMP,
    parent_dim_val_id BIGINT,
    is_actual BOOLEAN NOT NULL,
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    name VARCHAR(1024) NOT NULL,
    short_name VARCHAR(256),
    goal VARCHAR(1024) NOT NULL
);

CREATE TABLE IF NOT EXISTS d_measurement_units(
    id SERIAL PRIMARY KEY,
    modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_since TIMESTAMP NOT NULL,
    actual_until TIMESTAMP,
    parent_dim_val_id BIGINT,
    is_actual BOOLEAN NOT NULL,
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    name VARCHAR(256) NOT NULL,
    short_name VARCHAR(12) NOT NULL,
    UNIQUE (region_fk, name)
);

CREATE TABLE IF NOT EXISTS d_dimension_types(
    id BIGSERIAL PRIMARY KEY,
    modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_since TIMESTAMP NOT NULL,
    actual_until TIMESTAMP,
    parent_dim_val_id BIGINT,
    is_actual BOOLEAN NOT NULL,
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    name VARCHAR(64) NOT NULL,
    UNIQUE (region_fk, name)
);

CREATE TABLE IF NOT EXISTS d_dimension_levels(
    id BIGSERIAL PRIMARY KEY,
    modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_since TIMESTAMP NOT NULL,
    actual_until TIMESTAMP,
    parent_dim_val_id BIGINT,
    is_actual BOOLEAN NOT NULL,
    dimension_type_fk INTEGER NOT NULL REFERENCES d_dimension_types(id),
    name VARCHAR(128) NOT NULL,
    parent_level_fk INTEGER REFERENCES d_dimension_levels(id),
    UNIQUE (dimension_type_fk, name)
);

CREATE TABLE IF NOT EXISTS d_mgmt_focuses(
    id BIGSERIAL PRIMARY KEY,
    modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_since TIMESTAMP NOT NULL,
    actual_until TIMESTAMP,
    parent_dim_val_id BIGINT,
    is_actual BOOLEAN NOT NULL,
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    name VARCHAR(64) NOT NULL,
    order_num INTEGER NOT NULL,
    UNIQUE (region_fk, name)
);

CREATE TABLE IF NOT EXISTS d_norm_acts(
    id SERIAL PRIMARY KEY,
    modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_since TIMESTAMP NOT NULL,
    actual_until TIMESTAMP,
    parent_dim_val_id BIGINT,
    is_actual BOOLEAN NOT NULL,
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    name VARCHAR(1024) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    UNIQUE (region_fk, name)
);

CREATE TABLE IF NOT EXISTS dimensions (
    id SERIAL PRIMARY KEY,
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    name VARCHAR(256) NOT NULL,
    short_name VARCHAR(256) NOT NULL,
    code VARCHAR(256) NOT NULL,
    is_hierarchical BOOLEAN NOT NULL,
    parent_dimension_fk INTEGER REFERENCES dimensions(id),
    dimension_level_fk INTEGER NOT NULL REFERENCES d_dimension_levels(id),
    level_num INTEGER,
    UNIQUE (code, region_fk)
);

CREATE TABLE IF NOT EXISTS facts(
    id SERIAL PRIMARY KEY,
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    fact_name_fk BIGINT NOT NULL REFERENCES d_fact_names(id),
    measurement_unit_fk INTEGER NOT NULL REFERENCES d_measurement_units(id),
    goal_type INTEGER,
    high_threshold NUMERIC(16,2),
    low_threshold NUMERIC(16,2),
    high_threshold_pct NUMERIC(16,2),
    low_threshold_pct NUMERIC(16,2),
    norm_act_fk INTEGER REFERENCES d_norm_acts(id),
    actual_since TIMESTAMP NOT NULL,
    period INTEGER NOT NULL,
    plan_load_days INTEGER NOT NULL,
    fact_load_date TIMESTAMP NOT NULL,
    UNIQUE (region_fk, fact_name_fk)
);

CREATE TABLE IF NOT EXISTS view_modules(
    id SERIAL PRIMARY KEY,
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    name VARCHAR(1024) NOT NULL,
    UNIQUE (region_fk, name)
);

CREATE TABLE IF NOT EXISTS fact_passports(
    id INTEGER PRIMARY KEY REFERENCES facts(id),
    resp_user_fk INTEGER REFERENCES users(id),
    authorize_user_fk INTEGER REFERENCES users(id),
    view_module_fk INTEGER REFERENCES view_modules(id),
    status INTEGER NOT NULL,
    image BYTEA,
    image_ext VARCHAR(4)
);

CREATE TABLE IF NOT EXISTS fact_relatives(
    id BIGSERIAL PRIMARY KEY,
    fact_passport_fk INTEGER NOT NULL REFERENCES fact_passports(id),
    fact_fk INTEGER NOT NULL REFERENCES facts(id),
    name VARCHAR(32) NOT NULL,
    order_num INTEGER NOT NULL,
    report_offset INTEGER NOT NULL,
    UNIQUE (fact_passport_fk, fact_fk)
);

CREATE TABLE IF NOT EXISTS fact_effectiveness(
    fact_passport_fk INTEGER NOT NULL REFERENCES fact_passports(id),
    mgmt_focus_fk BIGINT NOT NULL REFERENCES d_mgmt_focuses(id),
    effectiveness INTEGER NOT NULL,
    UNIQUE (fact_passport_fk, mgmt_focus_fk)
);

CREATE TABLE IF NOT EXISTS user_modules(
    id SERIAL PRIMARY KEY,
    user_fk INTEGER NOT NULL REFERENCES users(id),
    module_fk INTEGER NOT NULL REFERENCES view_modules(id),
    order_num INTEGER NOT NULL,
    UNIQUE (user_fk, module_fk)
);

CREATE TABLE IF NOT EXISTS user_facts(
    id SERIAL PRIMARY KEY,
    user_fk INTEGER NOT NULL REFERENCES users(id),
    fact_fk INTEGER NOT NULL REFERENCES facts(id),
    module_fk INTEGER REFERENCES view_modules(id),
    order_num INTEGER NOT NULL,
    UNIQUE (user_fk, fact_fk, module_fk)
);

CREATE TABLE IF NOT EXISTS user_dashboards(
    id SERIAL PRIMARY KEY,
    user_fk INTEGER NOT NULL REFERENCES users(id),
    fact_fk INTEGER NOT NULL REFERENCES facts(id),
    dim_values VARCHAR(16384),
    view_params VARCHAR(16384),
    UNIQUE (user_fk, fact_fk)
);

CREATE TABLE IF NOT EXISTS fact_dimensions(
    id SERIAL PRIMARY KEY,
    dimension_fk INTEGER NOT NULL REFERENCES dimensions(id),
    fact_fk INTEGER NOT NULL REFERENCES facts(id),
    UNIQUE (dimension_fk, fact_fk)
);

CREATE TABLE IF NOT EXISTS data_sources(
    id SERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    region_fk INTEGER NOT NULL REFERENCES regions(id),
    segment INTEGER NOT NULL,
    resp_div_fk INTEGER NOT NULL REFERENCES region_divisions(id),
    resp_user_fk INTEGER NOT NULL REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS fact_data_sources(
    fact_fk INTEGER NOT NULL REFERENCES facts(id),
    data_source_fk INTEGER NOT NULL REFERENCES data_sources(id),
    UNIQUE (fact_fk, data_source_fk)
);

CREATE TABLE IF NOT EXISTS fact_passport_files(
    id SERIAL PRIMARY KEY,
    fact_fk INTEGER NOT NULL REFERENCES facts(id),
    name VARCHAR(256) NOT NULL,
    content BYTEA NOT NULL
);

ALTER TABLE facts ADD COLUMN IF NOT EXISTS data_source_fk INTEGER NOT NULL REFERENCES data_sources(id);
ALTER TABLE fact_relatives ADD COLUMN IF NOT EXISTS data_source_fk INTEGER NOT NULL REFERENCES data_sources(id);

ALTER TABLE fact_passports DROP COLUMN IF EXISTS view_module_fk;
ALTER TABLE facts ADD COLUMN IF NOT EXISTS view_module_fk INTEGER REFERENCES view_modules(id);

ALTER TABLE fact_passports ADD COLUMN IF NOT EXISTS formula VARCHAR(4096);
ALTER TABLE facts ADD COLUMN IF NOT EXISTS resp_user_fk INTEGER REFERENCES users(id);

ALTER TABLE region_divisions ADD COLUMN IF NOT EXISTS is_department BOOLEAN DEFAULT false;
