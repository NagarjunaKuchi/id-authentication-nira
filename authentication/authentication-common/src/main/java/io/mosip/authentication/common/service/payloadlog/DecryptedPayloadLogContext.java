package io.mosip.authentication.common.service.payloadlog;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-request holder for the data collected while a request flows through the
 * filter chain. One instance is created per HTTP request (only when logging is
 * enabled) and is stored as a request attribute, so concurrent requests never
 * share state.
 */
public final class DecryptedPayloadLogContext {

	/** Request attribute under which the context is stored. */
	public static final String REQUEST_ATTRIBUTE = DecryptedPayloadLogContext.class.getName();

	private final String id;
	private final String endpoint;
	private final String httpMethod;
	private final LocalDateTime requestTimestamp;
	private final long startNanos;
	private final AtomicBoolean completed = new AtomicBoolean(false);

	private volatile String decryptedRequest;

	DecryptedPayloadLogContext(String id, String endpoint, String httpMethod,
			LocalDateTime requestTimestamp, long startNanos) {
		this.id = id;
		this.endpoint = endpoint;
		this.httpMethod = httpMethod;
		this.requestTimestamp = requestTimestamp;
		this.startNanos = startNanos;
	}

	/** Id of the log record; generated at request start and used as its primary key. */
	public String getId() {
		return id;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public LocalDateTime getRequestTimestamp() {
		return requestTimestamp;
	}

	long getStartNanos() {
		return startNanos;
	}

	public String getDecryptedRequest() {
		return decryptedRequest;
	}

	void setDecryptedRequest(String decryptedRequest) {
		this.decryptedRequest = decryptedRequest;
	}

	/**
	 * Marks the context as completed.
	 *
	 * @return true only for the first caller; guarantees one record per request
	 */
	boolean markCompleted() {
		return completed.compareAndSet(false, true);
	}

	public boolean isCompleted() {
		return completed.get();
	}
}
