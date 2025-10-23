package org.tms.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.tms.entity.AuditLog;
import org.tms.repository.AuditLogRepository;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuditService {
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("\"password\"\s*:\s*\".*?\"");

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void audit(String event, String username, String details) {
        if (StringUtils.hasText(username)) {
            MDC.put("user", username);
        }
        try {
            String safeDetails = details == null ? "" : details;
            AUDIT_LOGGER.info("{} | {}", event, safeDetails);
        } finally {
            MDC.remove("user");
        }
    }

    public AuditLog auditDb(String event,
                            String username,
                            Object requestPayload,
                            Object responsePayload,
                            String requestUri,
                            String httpMethod,
                            String requestId) {
        AuditLog log = new AuditLog();
        log.setEvent(event);
        log.setUsername(username);
        log.setRequestId(Optional.ofNullable(requestId).orElseGet(() -> MDC.get("requestId")));
        log.setRequestUri(requestUri);
        log.setHttpMethod(httpMethod);
        log.setRequestPayload(redactJsonSafe(serialize(requestPayload)));
        log.setResponsePayload(redactJsonSafe(serialize(responsePayload)));
        return auditLogRepository.save(log);
    }

    private String serialize(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof String) return (String) obj;
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    private String redactJsonSafe(String json) {
        if (json == null) return null;
        return PASSWORD_PATTERN.matcher(json).replaceAll("\"password\":\"***\"");
    }
}
