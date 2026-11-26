package io.mosip.authentication.common.service.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Persistable;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for the table ida.nira_decrypted_payload_log.
 *
 * One row holds the (masked) decrypted request and the plaintext response
 * (before response signing) of a single API call. The primary key is assigned
 * by the application, so {@link Persistable} is implemented to let Spring Data
 * issue a plain INSERT instead of a SELECT followed by an INSERT.
 */
@NoArgsConstructor
@Data
@Table(name = "nira_decrypted_payload_log", schema = "ida")
@Entity
public class DecryptedPayloadLogEntity implements Persistable<String> {

	@Id
	@NotNull
	@Column(name = "id")
	private String id;

	@Column(name = "endpoint")
	private String endpoint;

	@Column(name = "http_method")
	private String httpMethod;

	@Column(name = "decrypted_request")
	private String decryptedRequest;

	@Column(name = "decrypted_response")
	private String decryptedResponse;

	@Column(name = "response_status")
	private String responseStatus;

	@Column(name = "error_message")
	private String errorMessage;

	@NotNull
	@Column(name = "request_timestamp")
	private LocalDateTime requestTimestamp;

	@Column(name = "response_timestamp")
	private LocalDateTime responseTimestamp;

	@Column(name = "processing_time_ms")
	private Long processingTimeMs;

	@NotNull
	@Column(name = "created_date")
	private LocalDateTime createdDate;

	@Transient
	private boolean newEntity = true;

	@Override
	public boolean isNew() {
		return newEntity;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.newEntity = false;
	}
}
