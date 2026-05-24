-- Create gate_vector_store table for Spring AI PGVector storage.
-- By creating this table early in the Flyway migration cycle, we prevent the
-- index creation script from failing, and Spring AI will automatically reuse this table on startup.

CREATE TABLE IF NOT EXISTS gate_vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(768)
);
