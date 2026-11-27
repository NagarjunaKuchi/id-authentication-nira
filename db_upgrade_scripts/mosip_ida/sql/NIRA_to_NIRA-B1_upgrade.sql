\c mosip_ida

----------------------------------------------Decrypted payload log table-------------------------------------------------------
-- Used only when nira.logging.decrypted-payload.enabled=true; the table is harmless (empty) otherwise.
CREATE TABLE IF NOT EXISTS ida.nira_decrypted_payload_log(
	id character varying(36) NOT NULL,
	endpoint character varying(512),
	http_method character varying(10),
	decrypted_request text,
	decrypted_response text,
	response_status character varying(64),
	error_message character varying(4000),
	request_timestamp timestamp NOT NULL,
	response_timestamp timestamp,
	processing_time_ms bigint,
	created_date timestamp NOT NULL,
	CONSTRAINT pk_nira_dpl_id PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_nira_dpl_created_date ON ida.nira_decrypted_payload_log (created_date);

COMMENT ON TABLE ida.nira_decrypted_payload_log IS 'nira_decrypted_payload_log: Optional diagnostic log of decrypted requests and plaintext responses (before signing); sensitive fields are masked.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.id IS 'ID: System generated unique id of the log record; one record per request.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.endpoint IS 'Endpoint: Request URI of the API call.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.http_method IS 'HTTP Method: HTTP method of the API call.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.decrypted_request IS 'Decrypted Request: JSON of the request after decryption, with sensitive fields masked. Null if decryption failed or the call has no body.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.decrypted_response IS 'Decrypted Response: Plaintext JSON response before signing, with sensitive fields masked.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.response_status IS 'Response Status: HTTP status and business outcome, e.g. 200/SUCCESS, 200/FAILED, 200/ERROR.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.error_message IS 'Error Message: Error code and text when the request failed with an exception.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.request_timestamp IS 'Request Timestamp: UTC time at which the request was received.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.response_timestamp IS 'Response Timestamp: UTC time at which the response was generated.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.processing_time_ms IS 'Processing Time: Total processing time in milliseconds.';
COMMENT ON COLUMN ida.nira_decrypted_payload_log.created_date IS 'Created Date: UTC time at which this record was created.';

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE ON ida.nira_decrypted_payload_log TO idauser;
GRANT ALL PRIVILEGES ON ida.nira_decrypted_payload_log TO postgres;
