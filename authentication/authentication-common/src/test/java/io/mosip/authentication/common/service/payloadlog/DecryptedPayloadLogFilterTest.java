package io.mosip.authentication.common.service.payloadlog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.authentication.common.service.entity.DecryptedPayloadLogEntity;
import io.mosip.authentication.common.service.filter.BaseAuthFilter;
import io.mosip.authentication.common.service.filter.ResettableStreamHttpServletRequest;
import io.mosip.authentication.common.service.integration.KeyManager;
import io.mosip.authentication.common.service.repository.DecryptedPayloadLogRepository;
import io.mosip.authentication.common.service.transaction.manager.IdAuthSecurityManager;
import io.mosip.authentication.common.service.util.EnvUtil;
import io.mosip.authentication.common.service.util.IdaRequestResponsConsumerUtil;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.exception.IdAuthenticationAppException;
import io.mosip.authentication.core.partner.dto.MispPolicyDTO;

/**
 * Runs the real BaseIDAFilter/BaseAuthFilter pipeline (wrappers, decipher hook,
 * validation, response capture, error handling) with the payload logger wired in.
 */
public class DecryptedPayloadLogFilterTest {

	private static final String BODY = "{\"id\":\"mosip.identity.auth\",\"version\":\"1.0\",\"transactionID\":\"t-1\","
			+ "\"request\":\"ENCRYPTED\"}";

	private final ObjectMapper mapper = new ObjectMapper();
	private final List<DecryptedPayloadLogEntity> saved = Collections.synchronizedList(new ArrayList<>());
	private DecryptedPayloadLogRepository repository;
	private KeyManager keyManager;
	private TestFilter filter;

	/** Behaviour switches for the test filter. */
	private boolean failDecryption;
	private boolean failValidation;
	private boolean signing;

	class TestFilter extends BaseAuthFilter {
		@Override
		protected Map<String, Object> decipherRequest(Map<String, Object> requestBody)
				throws IdAuthenticationAppException {
			if (failDecryption) {
				throw new IdAuthenticationAppException(IdAuthenticationErrorConstants.UNABLE_TO_PROCESS);
			}
			Map<String, Object> plain = new HashMap<>();
			plain.put("otp", "123456");
			plain.put("individualId", "9876543210");
			requestBody.replace("request", plain);
			return requestBody;
		}

		@Override
		protected void validateDecipheredRequest(ResettableStreamHttpServletRequest requestWrapper,
				Map<String, Object> decipherRequest) throws IdAuthenticationAppException {
			if (failValidation) {
				throw new IdAuthenticationAppException(IdAuthenticationErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
						"invalid");
			}
		}

		@Override
		protected void authenticateRequest(ResettableStreamHttpServletRequest requestWrapper) {
		}

		@Override
		protected String fetchId(ResettableStreamHttpServletRequest requestWrapper, String attribute) {
			return "";
		}

		@Override
		protected boolean isSigningRequired() {
			return signing;
		}

		@Override
		protected boolean isSignatureVerificationRequired() {
			return false;
		}

		@Override
		protected boolean isThumbprintValidationRequired() {
			return false;
		}

		@Override
		protected boolean isTrustValidationRequired() {
			return false;
		}

		@Override
		protected boolean isMispPolicyValidationRequired() {
			return false;
		}

		@Override
		protected boolean isCertificateValidationRequired() {
			return false;
		}

		@Override
		protected boolean isAMRValidationRequired() {
			return false;
		}

		@Override
		protected void checkAllowedAMRForKBT(Map<String, Object> requestBody, Set<String> allowedAMRs) {
		}

		@Override
		protected void checkMispPolicyAllowed(MispPolicyDTO mispPolicy) {
		}
	}

	@Before
	public void setUp() throws Exception {
		EnvUtil.setDateTimePattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		EnvUtil.setSignResponse("Signature");
		repository = mock(DecryptedPayloadLogRepository.class);
		doAnswer(inv -> {
			saved.add(inv.getArgument(0));
			return inv.getArgument(0);
		}).when(repository).save(any(DecryptedPayloadLogEntity.class));
		keyManager = mock(KeyManager.class);
		when(keyManager.signResponse(anyString())).thenReturn("signed");
		failDecryption = false;
		failValidation = false;
		signing = false;
		filter = new TestFilter();
		wire(true);
	}

	private void wire(boolean enabled) {
		ReflectionTestUtils.setField(filter, "env", mock(EnvUtil.class));
		ReflectionTestUtils.setField(filter, "mapper", mapper);
		ReflectionTestUtils.setField(filter, "keyManager", keyManager);
		ReflectionTestUtils.setField(filter, "securityManager", mock(IdAuthSecurityManager.class));
		ReflectionTestUtils.setField(filter, "requestResponsConsumerUtil", mock(IdaRequestResponsConsumerUtil.class));
		ReflectionTestUtils.setField(filter, "payloadLogService", new DecryptedPayloadLogService(repository, mapper,
				enabled, "bioValue,otp,requestSessionKey,requestHMAC", "individualId", Runnable::run));
	}

	private MockHttpServletResponse run(FilterChain chain) throws IOException, ServletException {
		MockHttpServletRequest req = new MockHttpServletRequest("POST", "/idauthentication/v1/auth/p/k");
		req.setContent(BODY.getBytes());
		MockHttpServletResponse res = new MockHttpServletResponse();
		filter.doFilter(req, res, chain);
		res.getWriter().flush();
		return res;
	}

	private FilterChain successChain(String responseJson, boolean status) {
		return (rq, rs) -> {
			((ResettableStreamHttpServletRequest) rq).putMetadata(IdAuthCommonConstants.STATUS, status);
			java.io.PrintWriter w = rs.getWriter(); // the wrapper returns a new writer per call
			w.write(responseJson);
			w.flush();
		};
	}

	// 1 + 3. enabled, successful authentication
	@Test
	public void successfulAuthPersistsMaskedRequestAndPlainResponse() throws Exception {
		MockHttpServletResponse res = run(successChain("{\"response\":{\"authStatus\":true}}", true));
		assertEquals("{\"response\":{\"authStatus\":true}}", res.getContentAsString());
		assertEquals(1, saved.size());
		DecryptedPayloadLogEntity e = saved.get(0);
		assertEquals("200/SUCCESS", e.getResponseStatus());
		assertEquals("/idauthentication/v1/auth/p/k", e.getEndpoint());
		assertTrue(e.getDecryptedRequest().contains("\"transactionID\":\"t-1\""));
		assertFalse("otp masked", e.getDecryptedRequest().contains("123456"));
		assertTrue("individualId partially masked", e.getDecryptedRequest().contains("****3210"));
		assertFalse(e.getDecryptedRequest().contains("ENCRYPTED"));
		assertTrue(e.getDecryptedResponse().contains("\"authStatus\":true"));
	}

	// 4. failed authentication (business failure)
	@Test
	public void failedAuthIsRecordedAsFailed() throws Exception {
		run(successChain("{\"response\":{\"authStatus\":false}}", false));
		assertEquals(1, saved.size());
		assertEquals("200/FAILED", saved.get(0).getResponseStatus());
	}

	// 2. disabled
	@Test
	public void disabledPersistsNothingAndBehavesTheSame() throws Exception {
		wire(false);
		MockHttpServletResponse res = run(successChain("{\"response\":{\"authStatus\":true}}", true));
		assertEquals("{\"response\":{\"authStatus\":true}}", res.getContentAsString());
		verify(repository, never()).save(any(DecryptedPayloadLogEntity.class));
	}

	// filter works when the bean is absent (otp / internal services)
	@Test
	public void worksWithoutThePayloadLogBean() throws Exception {
		ReflectionTestUtils.setField(filter, "payloadLogService", null);
		MockHttpServletResponse res = run(successChain("{\"ok\":true}", true));
		assertEquals("{\"ok\":true}", res.getContentAsString());
		verify(repository, never()).save(any(DecryptedPayloadLogEntity.class));
	}

	// 5. decryption failure: error response returned, no decrypted request stored
	@Test
	public void decryptionFailureIsLoggedWithoutAnyRequestPayload() throws Exception {
		failDecryption = true;
		MockHttpServletResponse res = run((rq, rs) -> {
			throw new AssertionError("controller must not run");
		});
		assertTrue(res.getContentAsString().contains("errors"));
		assertEquals(1, saved.size());
		DecryptedPayloadLogEntity e = saved.get(0);
		assertNull("never store the still-encrypted body", e.getDecryptedRequest());
		assertEquals("200/ERROR", e.getResponseStatus());
		assertTrue(e.getErrorMessage().startsWith(IdAuthenticationErrorConstants.UNABLE_TO_PROCESS.getErrorCode()));
		assertTrue(e.getDecryptedResponse().contains("errors"));
	}

	// decrypted OK but validation fails: the decrypted request is kept
	@Test
	public void validationFailureKeepsDecryptedRequest() throws Exception {
		failValidation = true;
		run((rq, rs) -> {
			throw new AssertionError("controller must not run");
		});
		assertEquals(1, saved.size());
		DecryptedPayloadLogEntity e = saved.get(0);
		assertTrue(e.getDecryptedRequest().contains("individualId"));
		assertFalse(e.getDecryptedRequest().contains("123456"));
		assertEquals("200/ERROR", e.getResponseStatus());
	}

	// 6. response signing failure: client still gets the plaintext response, record is written
	@Test
	public void responseSigningFailureStillLogsAndReturnsResponse() throws Exception {
		signing = true;
		when(keyManager.signResponse(anyString()))
				.thenThrow(new IdAuthenticationAppException(IdAuthenticationErrorConstants.UNABLE_TO_PROCESS));
		MockHttpServletResponse res = run(successChain("{\"response\":{\"authStatus\":true}}", true));
		assertEquals("{\"response\":{\"authStatus\":true}}", res.getContentAsString());
		assertEquals(1, saved.size());
		assertTrue(saved.get(0).getDecryptedResponse().contains("authStatus"));
	}

	// 7. database failure never changes the API response
	@Test
	public void databaseFailureDoesNotAffectResponse() throws Exception {
		doAnswer(inv -> {
			throw new RuntimeException("db down");
		}).when(repository).save(any(DecryptedPayloadLogEntity.class));
		MockHttpServletResponse res = run(successChain("{\"response\":{\"authStatus\":true}}", true));
		assertEquals("{\"response\":{\"authStatus\":true}}", res.getContentAsString());
	}

	// 10. exactly one record, request and response together, for each call
	@Test
	public void eachCallProducesExactlyOneRecordWithBothPayloads() throws Exception {
		run(successChain("{\"r\":1}", true));
		run(successChain("{\"r\":2}", true));
		assertEquals(2, saved.size());
		assertFalse(saved.get(0).getId().equals(saved.get(1).getId()));
		for (DecryptedPayloadLogEntity e : saved) {
			assertTrue(e.getDecryptedRequest() != null && e.getDecryptedResponse() != null);
		}
	}
}
