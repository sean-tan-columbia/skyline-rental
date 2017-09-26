CREATE TABLE eventbus.USERS (
    id VARCHAR(18) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255) NOT NULL,
    wechat_id VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    status VARCHAR(10),
    last_login_timestamp TIMESTAMP,
    created_timestamp TIMESTAMP NOT NULL,
    last_updated_timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_users_email_status ON eventbus.USERS (email,status);