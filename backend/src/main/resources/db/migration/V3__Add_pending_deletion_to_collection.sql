-- Soft delete with an undo window: a removed book keeps its row until this time passes,
-- then a scheduled purge deletes it for good.
ALTER TABLE collection ADD COLUMN deletion_scheduled_at TIMESTAMP;

CREATE INDEX idx_collection_deletion_scheduled_at ON collection(deletion_scheduled_at);
