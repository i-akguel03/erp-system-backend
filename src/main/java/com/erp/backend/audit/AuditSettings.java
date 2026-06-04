package com.erp.backend.audit;

import com.erp.backend.domain.SystemSetting;
import com.erp.backend.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuditSettings {

    static final String KEY = "audit.excludedEntityTypes";
    private static final String DEFAULT_EXCLUDED = "Notification";

    private final SystemSettingRepository repo;
    private final Set<String> excludedEntityTypes = Collections.synchronizedSet(new HashSet<>());

    public AuditSettings(SystemSettingRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    void load() {
        String raw = repo.findById(KEY)
                .map(SystemSetting::getValue)
                .orElse(DEFAULT_EXCLUDED);
        excludedEntityTypes.clear();
        if (raw != null && !raw.isBlank()) {
            Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(excludedEntityTypes::add);
        }
    }

    public Set<String> getExcludedEntityTypes() {
        return Collections.unmodifiableSet(excludedEntityTypes);
    }

    @Transactional
    public void exclude(String entityType) {
        excludedEntityTypes.add(entityType);
        persist();
    }

    @Transactional
    public void include(String entityType) {
        excludedEntityTypes.remove(entityType);
        persist();
    }

    public boolean isExcluded(String entityType) {
        return excludedEntityTypes.contains(entityType);
    }

    private void persist() {
        String value = excludedEntityTypes.stream()
                .sorted()
                .collect(Collectors.joining(","));
        repo.save(new SystemSetting(KEY, value));
    }
}
