package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.HomeModuleConfig;
import org.example.model.SiteOperationLog;
import org.example.model.SitePageConfig;
import org.example.model.SitePageVersion;
import org.example.repository.SiteOperationLogRepository;
import org.example.repository.SitePageVersionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SiteOpsService {
    private final SiteOperationLogRepository logRepository;
    private final SitePageVersionRepository versionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SiteOpsService(SiteOperationLogRepository logRepository, SitePageVersionRepository versionRepository) {
        this.logRepository = logRepository;
        this.versionRepository = versionRepository;
    }

    public void log(String operator, String action, String target, String detail) {
        SiteOperationLog row = new SiteOperationLog();
        row.setOperatorName(operator == null || operator.trim().isEmpty() ? "system" : operator);
        row.setAction(action);
        row.setTarget(target);
        row.setDetail(detail);
        logRepository.save(row);
    }

    public List<SiteOperationLog> recentLogs() {
        return logRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public void savePageSnapshot(SitePageConfig page, List<HomeModuleConfig> modules) {
        try {
            Map<String, Object> snap = new HashMap<String, Object>();
            snap.put("page", page);
            snap.put("modules", modules);
            SitePageVersion row = new SitePageVersion();
            row.setRoutePath(page.getRoutePath());
            row.setSnapshotJson(objectMapper.writeValueAsString(snap));
            versionRepository.save(row);
        } catch (Exception ignore) {
        }
    }

    public SitePageVersion latestSnapshot(String routePath) {
        return versionRepository.findTopByRoutePathOrderByCreatedAtDesc(routePath).orElse(null);
    }

    public Map<String, Object> parseSnapshot(SitePageVersion version) {
        if (version == null) return null;
        try {
            return objectMapper.readValue(version.getSnapshotJson(), Map.class);
        } catch (Exception ex) {
            return null;
        }
    }
}

