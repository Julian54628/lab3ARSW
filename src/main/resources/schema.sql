CREATE TABLE IF NOT EXISTS blueprints (
    author VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (author, name)
    );

CREATE TABLE IF NOT EXISTS points (
    id SERIAL PRIMARY KEY,
    author VARCHAR(255) NOT NULL,
    bpname VARCHAR(255) NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    FOREIGN KEY (author, bpname) REFERENCES blueprints(author, name) ON DELETE CASCADE
    );

INSERT INTO blueprints(author, name) VALUES('jane', 'garden'),('john', 'garage'),('john', 'house')
    ON CONFLICT (author, name) DO NOTHING;

INSERT INTO points(author, bpname, x, y) VALUES('jane', 'garden', 6, 7),('jane', 'garden', 3, 4),('jane', 'garden', 2, 2)
    ON CONFLICT DO NOTHING;

INSERT INTO points(author, bpname, x, y) VALUES('john', 'garage', 15, 5),('john', 'garage', 5, 5)
    ON CONFLICT DO NOTHING;

INSERT INTO points(author, bpname, x, y) VALUES('john', 'house', 0, 10),('john', 'house', 10, 10),('john', 'house', 10, 0),('john', 'house', 0, 0)
    ON CONFLICT DO NOTHING;