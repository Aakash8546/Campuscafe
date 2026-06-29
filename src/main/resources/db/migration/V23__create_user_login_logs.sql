CREATE TABLE user_login_logs (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT,
    user_id BIGINT,
    user_name VARCHAR(100),
    user_role VARCHAR(50),
    email VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255),
    login_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_login_logs_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_login_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_user_login_logs_merchant_id ON user_login_logs (merchant_id);
CREATE INDEX idx_user_login_logs_user_id ON user_login_logs (user_id);
CREATE INDEX idx_user_login_logs_login_time ON user_login_logs (login_time);
