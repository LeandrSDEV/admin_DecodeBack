package br.com.portal.decode_api.service;

import br.com.portal.decode_api.dtos.SystemMetricSampleResponse;
import br.com.portal.decode_api.dtos.SystemOverviewResponse;
import br.com.portal.decode_api.entity.SystemMetricSampleEntity;
import br.com.portal.decode_api.repository.SystemMetricSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemMetricsService {

    private final SystemMetricSampleRepository repo;
    private final Object cpuLock = new Object();


    private final SystemInfo si = new SystemInfo();
    private final CentralProcessor cpu = si.getHardware().getProcessor();
    private final GlobalMemory mem = si.getHardware().getMemory();

    // guarda ticks anteriores para calcular o delta corretamente
    private volatile long[] prevTicks = cpu.getSystemCpuLoadTicks();

    @Scheduled(fixedDelayString = "${app.monitoring.system-sample-interval-ms:60000}")
    public void collectSample() {
        repo.save(snapshotEntity());
    }

    public SystemOverviewResponse overview() {
        SystemMetricSampleEntity latest = repo.findTop1ByOrderBySampledAtDesc()
                .orElseGet(this::snapshotEntity);

        return new SystemOverviewResponse(
                latest.getCpuLoad(),
                latest.getMemUsedBytes(),
                latest.getMemTotalBytes(),
                latest.getDiskUsedBytes(),
                latest.getDiskTotalBytes(),
                latest.getUptimeSeconds(),
                latest.getSampledAt()
        );
    }

    public List<SystemMetricSampleResponse> listSamplesSince(LocalDateTime since) {
        return repo.findBySampledAtGreaterThanEqualOrderBySampledAtAsc(since).stream()
                .map(s -> new SystemMetricSampleResponse(
                        s.getId(),                 // <- ajuste DTO se não for Long
                        s.getSampledAt(),
                        s.getCpuLoad(),
                        s.getMemUsedBytes(),
                        s.getMemTotalBytes(),
                        s.getDiskUsedBytes(),
                        s.getDiskTotalBytes(),
                        s.getUptimeSeconds()
                ))
                .toList();
    }

    private SystemMetricSampleEntity snapshotEntity() {
        double cpuLoad;

        synchronized (cpuLock) {
            long[] ticksNow = cpu.getSystemCpuLoadTicks();

            // Se for a primeira coleta, ou se o OSHI retornou null, faz fallback e re-inicializa
            if (prevTicks == null || ticksNow == null) {
                cpuLoad = cpuLoad = cpu.getSystemCpuLoad(200) * 100.0;
                prevTicks = (ticksNow != null) ? ticksNow : cpu.getSystemCpuLoadTicks();
            } else {
                cpuLoad = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100.0;
                prevTicks = ticksNow;
            }
        }

        long totalMem = mem.getTotal();
        long availMem = mem.getAvailable();
        long usedMem = Math.max(0, totalMem - availMem);

        long[] disk = diskUsage();
        long usedDisk = disk[0];
        long totalDisk = disk[1];

        long uptimeSeconds = si.getOperatingSystem().getSystemUptime();

        return SystemMetricSampleEntity.builder()
                .sampledAt(LocalDateTime.now())
                .cpuLoad(round2(cpuLoad))
                .memUsedBytes(usedMem)
                .memTotalBytes(totalMem)
                .diskUsedBytes(usedDisk)
                .diskTotalBytes(totalDisk)
                .uptimeSeconds(uptimeSeconds)
                .build();
    }

    private long[] diskUsage() {
        try {
            FileSystem fs = si.getOperatingSystem().getFileSystem();
            long total = 0L;
            long used = 0L;
            for (OSFileStore store : fs.getFileStores()) {
                long t = store.getTotalSpace();
                long u = t - store.getUsableSpace();
                if (t > 0) {
                    total += t;
                    used += Math.max(0, u);
                }
            }
            return new long[]{used, total};
        } catch (Exception ex) {
            return new long[]{0L, 0L};
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}