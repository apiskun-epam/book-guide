-- Create books collection table
CREATE TABLE collection (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    title TEXT NOT NULL,
    author TEXT,
    isbn TEXT,
    cover_url TEXT,
    publish_year INTEGER,
    description TEXT,
    rating INTEGER CHECK (rating >= 0 AND rating <= 5),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for user queries
CREATE INDEX idx_collection_user_id ON collection(user_id);

-- Index for ISBN lookups
CREATE INDEX idx_collection_isbn ON collection(isbn);
