package com.ping.servermanage.util;

import com.ping.servermanage.dto.ServiceRuntimeInfo;
import com.ping.servermanage.entity.GlobalConfig;
import com.ping.servermanage.entity.ServerBaseAttr;
import com.ping.servermanage.entity.ServiceConfigItem;
import com.ping.servermanage.entity.ServicePackage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServiceManager {

    private final Map<String, Process> processMap = new ConcurrentHashMap<String, Process>();
    private final Map<String, ServiceRuntimeInfo> runtimeInfoMap = new ConcurrentHashMap<String, ServiceRuntimeInfo>();

    @Value("${server-manage.runtime-dir:runtime}")
    private String runtimeDir;

    public synchronized ServiceRuntimeInfo startService(ServerBaseAttr service,
                                                        ServicePackage servicePackage,
                                                        List<ServiceConfigItem> configItems,
                                                        Map<String, GlobalConfig> globalConfigs) throws IOException {
        if (servicePackage == null) {
            throw new IllegalStateException("Please select a jar package before starting the service");
        }
        Process oldProcess = processMap.get(service.getId());
        if (oldProcess != null && isAlive(oldProcess)) {
            throw new IllegalStateException("Service is already running");
        }

        File logFile = buildLogFile(service);
        List<String> command = buildCommand(service, servicePackage, configItems, globalConfigs);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        Process process = builder.start();

        processMap.put(service.getId(), process);
        ServiceRuntimeInfo runtimeInfo = newRuntimeInfo(service.getId(), "RUNNING", command, logFile);
        runtimeInfoMap.put(service.getId(), runtimeInfo);
        return runtimeInfo;
    }

    public synchronized ServiceRuntimeInfo stopService(String serviceId) {
        Process process = processMap.remove(serviceId);
        if (process != null && isAlive(process)) {
            process.destroy();
        }
        ServiceRuntimeInfo runtimeInfo = runtimeInfoMap.get(serviceId);
        if (runtimeInfo == null) {
            runtimeInfo = new ServiceRuntimeInfo();
            runtimeInfo.setServiceId(serviceId);
        }
        runtimeInfo.setStatus("STOPPED");
        runtimeInfoMap.put(serviceId, runtimeInfo);
        return runtimeInfo;
    }

    public synchronized ServiceRuntimeInfo restartService(ServerBaseAttr service,
                                                          ServicePackage servicePackage,
                                                          List<ServiceConfigItem> configItems,
                                                          Map<String, GlobalConfig> globalConfigs) throws IOException {
        stopService(service.getId());
        return startService(service, servicePackage, configItems, globalConfigs);
    }

    public ServiceRuntimeInfo getRuntimeInfo(String serviceId) {
        ServiceRuntimeInfo runtimeInfo = runtimeInfoMap.get(serviceId);
        if (runtimeInfo == null) {
            runtimeInfo = new ServiceRuntimeInfo();
            runtimeInfo.setServiceId(serviceId);
            runtimeInfo.setStatus("STOPPED");
            runtimeInfo.setCommand(Collections.<String>emptyList());
        }
        Process process = processMap.get(serviceId);
        if (process != null && !isAlive(process)) {
            runtimeInfo.setStatus("STOPPED");
        }
        return runtimeInfo;
    }

    private List<String> buildCommand(ServerBaseAttr service,
                                      ServicePackage servicePackage,
                                      List<ServiceConfigItem> configItems,
                                      Map<String, GlobalConfig> globalConfigs) {
        List<String> command = new ArrayList<String>();
        command.add("java");
        if (service.getMemoryLimit() != null && !service.getMemoryLimit().trim().isEmpty()) {
            command.add("-Xmx" + service.getMemoryLimit().trim());
        }
        command.add("-Dserver.port=" + service.getPort());
        command.add("-Dspring.application.name=" + service.getServiceName());
        if (service.getDatabaseName() != null && !service.getDatabaseName().trim().isEmpty()) {
            command.add("-Dspring.datasource.name=" + service.getDatabaseName().trim());
        }
        addGlobalProperty(command, globalConfigs, "database.address", "common.database.address");
        addGlobalProperty(command, globalConfigs, "middleware.address", "common.middleware.address");
        for (ServiceConfigItem item : configItems) {
            if (item.getParameterPath() != null && !item.getParameterPath().trim().isEmpty()) {
                command.add("-D" + item.getParameterPath().trim() + "=" + nullToEmpty(item.getValue()));
            }
        }
        command.add("-jar");
        command.add(servicePackage.getFilePath());
        return command;
    }

    public File getLogDir(String serviceId) {
        File dir = new File(runtimeDir, "logs" + File.separator + serviceId);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File buildLogFile(ServerBaseAttr service) throws IOException {
        File dir = getLogDir(service.getId());
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        File logFile = new File(dir, service.getServiceName() + "-" + time + ".log");
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
        return logFile;
    }

    private void addGlobalProperty(List<String> command,
                                   Map<String, GlobalConfig> globalConfigs,
                                   String configKey,
                                   String propertyName) {
        GlobalConfig config = globalConfigs.get(configKey);
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().trim().isEmpty()) {
            command.add("-D" + propertyName + "=" + config.getConfigValue().trim());
        }
    }

    private ServiceRuntimeInfo newRuntimeInfo(String serviceId, String status, List<String> command, File logFile) {
        ServiceRuntimeInfo runtimeInfo = new ServiceRuntimeInfo();
        runtimeInfo.setServiceId(serviceId);
        runtimeInfo.setStatus(status);
        runtimeInfo.setCommand(command);
        runtimeInfo.setLogFile(logFile.getAbsolutePath());
        return runtimeInfo;
    }

    private boolean isAlive(Process process) {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException ex) {
            return true;
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public Map<String, GlobalConfig> toConfigMap(List<GlobalConfig> configs) {
        Map<String, GlobalConfig> map = new HashMap<String, GlobalConfig>();
        for (GlobalConfig config : configs) {
            map.put(config.getConfigKey(), config);
        }
        return map;
    }
}
