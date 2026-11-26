package io.mosip.authentication.common.service.repository;

import org.springframework.stereotype.Repository;

import io.mosip.authentication.common.service.entity.DecryptedPayloadLogEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * Repository for entity {@link DecryptedPayloadLogEntity}.
 */
@Repository
public interface DecryptedPayloadLogRepository extends BaseRepository<DecryptedPayloadLogEntity, String> {
}
