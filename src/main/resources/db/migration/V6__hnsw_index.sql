-- TRACK 2: HNSW index on the PGVector embedding column.
-- Uses cosine distance operator class to match the COSINE_DISTANCE similarity type
-- configured in application.yml (spring.ai.vectorstore.pgvector.distance-type).
--
-- m=16: number of bi-directional links per node (higher = better recall, more RAM)
-- ef_construction=64: size of dynamic candidate list during build (higher = better index quality)

CREATE INDEX IF NOT EXISTS gate_vector_store_hnsw_idx
    ON gate_vector_store
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);


