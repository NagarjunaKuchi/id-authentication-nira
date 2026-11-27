-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_ida
-- Table Name 	: ida.nira_decrypted_payload_log
-- Purpose    	: nira_decrypted_payload_log: Optional diagnostic log of the decrypted request and the plaintext
--                response (before signing) of authentication-service API calls. Populated only when
--                nira.logging.decrypted-payload.enabled=true. Sensitive fields are masked before insert.
--
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Nov-2026				NIRA				Created nira_decrypted_payload_log table
-- ------------------------------------------------------------------------------------------

-- object: ida.nira_decrypted_payload_log | type: TABLE --
-- DROP TABLE IF EXISTS ida.nira_decrypted_payload_log CASCADE;
CREATE TABLE ida.nira_decrypted_payload_log(
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
-- ddl-end --
CREATE INDEX IF NOT EXISTS idx_nira_dpl_created_date ON ida.nira_decrypted_payload_log (created_date);
-- ddl-end --
COMMENT ON TABLE ida.nira_decrypted_payload_log IS 'nira_decrypted_payload_log: Optional diagnostic log of decrypted requests and plaintext responses (before signing); sensitive fields are masked.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.id IS 'ID: System generated unique id of the log record; one record per request.';
-- ddl-end --
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.endpoint IS 'Endpoint: Request URI of the API call.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.http_method IS 'HTTP Method: HTTP method of the API call.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.decrypted_request IS 'Decrypted Request: JSON of the request after decryption, with sensitive fields masked. Null if decryption failed or the call has no body.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.decrypted_response IS 'Decrypted Response: Plaintext JSON response before signing, with sensitive fields masked.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.response_status IS 'Response Status: HTTP status and business outcome, e.g. 200/SUCCESS, 200/FAILED, 200/ERROR.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.error_message IS 'Error Message: Error code and text when the request failed with an exception.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.request_timestamp IS 'Request Timestamp: UTC time at which the request was received.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.response_timestamp IS 'Response Timestamp: UTC time at which the response was generated.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.processing_time_ms IS 'Processing Time: Total processing time in milliseconds.';
-- ddl-end --
COMMENT ON COLUMN ida.nira_decrypted_payload_log.created_date IS 'Created Date: UTC time at which this record was created.';
-- ddl-end --
