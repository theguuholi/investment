CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portfolios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE assets (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    name VARCHAR(100),
    quantity DECIMAL(19,4) NOT NULL,
    purchase_price DECIMAL(19,4) NOT NULL,
    CONSTRAINT fk_portfolio
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolios(id)
        ON DELETE CASCADE
);
