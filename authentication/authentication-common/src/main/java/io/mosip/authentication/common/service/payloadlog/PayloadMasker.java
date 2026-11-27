package io.mosip.authentication.common.service.payloadlog;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Masks sensitive JSON fields (at any depth) before a payload is persisted.
 * Field names are compared case-insensitively.
 */
public class PayloadMasker {

	/** Replacement for fully masked values. */
	public static final String MASK = "****";

	/** Stored instead of a payload that cannot be parsed, so nothing leaks unmasked. */
	public static final String UNPARSEABLE = "[PAYLOAD NOT LOGGED: NOT VALID JSON]";

	/** Number of trailing characters kept for partially masked fields. */
	private static final int VISIBLE_TAIL = 4;

	private final ObjectMapper mapper;
	private final Set<String> fullyMasked;
	private final Set<String> partiallyMasked;

	/**
	 * @param mapper          mapper used to parse and write JSON
	 * @param fullyMasked     lower-cased names whose values are replaced by {@link #MASK}
	 * @param partiallyMasked lower-cased names whose values keep only the last 4 characters
	 */
	public PayloadMasker(ObjectMapper mapper, Set<String> fullyMasked, Set<String> partiallyMasked) {
		this.mapper = mapper;
		this.fullyMasked = fullyMasked;
		this.partiallyMasked = partiallyMasked;
	}

	/**
	 * Returns the masked JSON text, or null when the input is null.
	 */
	public String mask(String json) {
		if (json == null) {
			return null;
		}
		try {
			JsonNode root = mapper.readTree(json);
			if (root == null) {
				return UNPARSEABLE;
			}
			maskNode(root);
			return mapper.writeValueAsString(root);
		} catch (IOException e) {
			return UNPARSEABLE;
		}
	}

	private void maskNode(JsonNode node) {
		if (node.isObject()) {
			ObjectNode obj = (ObjectNode) node;
			Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
			while (it.hasNext()) {
				Map.Entry<String, JsonNode> entry = it.next();
				String name = entry.getKey().toLowerCase(Locale.ROOT);
				JsonNode value = entry.getValue();
				if (fullyMasked.contains(name)) {
					entry.setValue(mapper.getNodeFactory().textNode(MASK));
				} else if (partiallyMasked.contains(name) && !value.isContainerNode()) {
					entry.setValue(mapper.getNodeFactory().textNode(partial(value.asText())));
				} else {
					maskNode(value);
				}
			}
		} else if (node.isArray()) {
			for (JsonNode child : (ArrayNode) node) {
				maskNode(child);
			}
		}
	}

	private static String partial(String value) {
		if (value == null || value.length() <= VISIBLE_TAIL) {
			return MASK;
		}
		return MASK + value.substring(value.length() - VISIBLE_TAIL);
	}
}
