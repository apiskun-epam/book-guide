-- Splits the single collection into two lists: the rated COLLECTION and the unrated READING_LIST.
-- Existing rows stay in the collection.
ALTER TABLE collection
    ADD COLUMN list_type TEXT NOT NULL DEFAULT 'COLLECTION'
    CHECK (list_type IN ('COLLECTION', 'READING_LIST'));

CREATE INDEX idx_collection_user_list ON collection (user_id, list_type);
