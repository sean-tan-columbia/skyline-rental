CREATE TABLE eventbus.RENTALS (
    id VARCHAR(18) NOT NULL,
    poster_id VARCHAR(18) NOT NULL,
    address VARCHAR(255),
    rental_type VARCHAR(20),
    neighborhood VARCHAR(255),
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    price DECIMAL(8,2),
    quantifier VARCHAR(10),
    bedroom VARCHAR(10),
    bathroom VARCHAR(10),
    start_date DATE,
    end_date DATE,
    description TEXT,
    image_id VARCHAR(512),
    status VARCHAR(10),
    created_timestamp TIMESTAMP NOT NULL,
    last_updated_timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_rentals_posterId ON eventbus.RENTALS (poster_id);

ALTER TABLE eventbus.RENTALS ADD CONSTRAINT fnk_rentals_users_posterId FOREIGN KEY (poster_id) REFERENCES eventbus.USERS ON DELETE CASCADE;