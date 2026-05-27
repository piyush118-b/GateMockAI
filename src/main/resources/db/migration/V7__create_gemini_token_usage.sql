CREATE TABLE gemini_token_usage (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  usage_date  DATE NOT NULL DEFAULT CURRENT_DATE,
  call_type   VARCHAR(30) NOT NULL,  -- 'INGESTION' or 'GENERATION' or 'EXPLANATION'
  input_tokens  INTEGER NOT NULL DEFAULT 0,
  output_tokens INTEGER NOT NULL DEFAULT 0,
  total_tokens  INTEGER NOT NULL DEFAULT 0,
  model_used  VARCHAR(50) NOT NULL DEFAULT 'gemini-2.5-flash',
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_token_usage_date ON gemini_token_usage(usage_date);
