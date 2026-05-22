package com.ping.servermanage.service.impl;

import com.ping.servermanage.dao.GlobalConfigRepository;
import com.ping.servermanage.dao.ServerManegeMapper;
import com.ping.servermanage.dao.ServiceConfigItemRepository;
import com.ping.servermanage.dao.ServicePackageRepository;
import com.ping.servermanage.dto.ServiceDetail;
import com.ping.servermanage.dto.ServiceRuntimeInfo;
import com.ping.servermanage.entity.GlobalConfig;
import com.ping.servermanage.entity.ServerBaseAttr;
import com.ping.servermanage.entity.ServiceConfigItem;
import com.ping.servermanage.entity.ServicePackage;
import com.ping.servermanage.service.ServerManegeService;
import com.ping.servermanage.util.ServiceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ServerManegeServiceImpl implements ServerManegeService {

    @javax.annotation.Resource
    private ServerManegeMapper serverManegeMapper;

    @javax.annotation.Resource
    private ServiceConfigItemRepository configItemRepository;

    @javax.annotation.Resource
    private ServicePackageRepository packageRepository;

    @javax.annotation.Resource
    private GlobalConfigRepository globalConfigRepository;

    @javax.annotation.Resource
    private ServiceManager serviceManager;

    @Value("${server-manage.runtime-dir:runtime}")
    private String runtimeDir;

    @Override
    public List<ServerBaseAttr> getAllServerList() {
        List<ServerBaseAttr> services = serverManegeMapper.findAll();
        for (ServerBaseAttr service : services) {
            service.setStatus(serviceManager.getRuntimeInfo(service.getId()).getStatus());
        }
        return services;
    }

    @Override
    public ServerBaseAttr getServer(String id) {
        ServerBaseAttr service = findService(id);
        service.setStatus(serviceManager.getRuntimeInfo(id).getStatus());
        return service;
    }

    @Override
    public ServiceDetail getServerDetail(String id) {
        ServiceDetail detail = new ServiceDetail();
        detail.setService(getServer(id));
        detail.setConfigItems(getConfigItems(id));
        detail.setPackages(getPackages(id));
        detail.setRuntimeInfo(runtimeInfo(id));
        return detail;
    }

    @Override
    public ServerBaseAttr addServer(ServerBaseAttr serverBaseAttr) {
        validateService(serverBaseAttr);
        LocalDateTime now = LocalDateTime.now();
        serverBaseAttr.setCreateTime(now);
        serverBaseAttr.setUpdateTime(now);
        serverBaseAttr.setStatus("STOPPED");
        return serverManegeMapper.save(serverBaseAttr);
    }

    @Override
    public ServerBaseAttr updateServer(String id, ServerBaseAttr serverBaseAttr) {
        validateService(serverBaseAttr);
        ServerBaseAttr db = findService(id);
        db.setServiceName(serverBaseAttr.getServiceName());
        db.setPort(serverBaseAttr.getPort());
        db.setMemoryLimit(serverBaseAttr.getMemoryLimit());
        db.setDescription(serverBaseAttr.getDescription());
        db.setDatabaseName(serverBaseAttr.getDatabaseName());
        db.setUpdateTime(LocalDateTime.now());
        return serverManegeMapper.save(db);
    }

    @Override
    @Transactional
    public void deleteServer(String id) {
        stop(id);
        configItemRepository.deleteByServiceId(id);
        packageRepository.deleteByServiceId(id);
        serverManegeMapper.deleteById(id);
    }

    @Override
    public List<ServiceConfigItem> getConfigItems(String serviceId) {
        findService(serviceId);
        return configItemRepository.findByServiceIdOrderByCreateTimeAsc(serviceId);
    }

    @Override
    public ServiceConfigItem addConfigItem(String serviceId, ServiceConfigItem configItem) {
        findService(serviceId);
        LocalDateTime now = LocalDateTime.now();
        configItem.setId(null);
        configItem.setServiceId(serviceId);
        configItem.setCreateTime(now);
        configItem.setUpdateTime(now);
        return configItemRepository.save(configItem);
    }

    @Override
    public ServiceConfigItem updateConfigItem(String configId, ServiceConfigItem configItem) {
        ServiceConfigItem db = findConfigItem(configId);
        db.setName(configItem.getName());
        db.setParameterPath(configItem.getParameterPath());
        db.setValue(configItem.getValue());
        db.setUpdateTime(LocalDateTime.now());
        return configItemRepository.save(db);
    }

    @Override
    public void deleteConfigItem(String configId) {
        configItemRepository.deleteById(configId);
    }

    @Override
    public List<ServicePackage> getPackages(String serviceId) {
        findService(serviceId);
        return packageRepository.findByServiceIdOrderByUploadTimeDesc(serviceId);
    }

    @Override
    public ServicePackage uploadPackage(String serviceId, String version, MultipartFile file) throws IOException {
        findService(serviceId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = safeFileName(file.getOriginalFilename());
        if (!originalName.endsWith(".jar")) {
            throw new IllegalArgumentException("只允许上传 jar 包");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("版本号不能为空");
        }
        File packageDir = new File(runtimeDir, "packages" + File.separator + serviceId);
        if (!packageDir.exists()) {
            packageDir.mkdirs();
        }
        File target = new File(packageDir, System.currentTimeMillis() + "-" + originalName);
        Files.copy(file.getInputStream(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ServicePackage servicePackage = new ServicePackage();
        servicePackage.setServiceId(serviceId);
        servicePackage.setFileName(originalName);
        servicePackage.setVersion(version);
        servicePackage.setFilePath(target.getAbsolutePath());
        servicePackage.setFileSize(target.length());
        servicePackage.setSha256(sha256(target));
        servicePackage.setCurrentUsed(false);
        servicePackage.setUploadTime(LocalDateTime.now());
        return packageRepository.save(servicePackage);
    }

    @Override
    @Transactional
    public ServicePackage usePackage(String serviceId, String packageId) {
        ServerBaseAttr service = findService(serviceId);
        ServicePackage selected = findPackage(packageId);
        if (!serviceId.equals(selected.getServiceId())) {
            throw new IllegalArgumentException("Jar 包不属于当前服务");
        }
        List<ServicePackage> packages = packageRepository.findByServiceId(serviceId);
        for (ServicePackage servicePackage : packages) {
            servicePackage.setCurrentUsed(packageId.equals(servicePackage.getId()));
        }
        packageRepository.saveAll(packages);
        service.setCurrentPackageId(selected.getId());
        service.setUpdateTime(LocalDateTime.now());
        serverManegeMapper.save(service);
        return selected;
    }

    @Override
    public void deletePackage(String packageId) {
        ServicePackage servicePackage = findPackage(packageId);
        ServerBaseAttr service = findService(servicePackage.getServiceId());
        if (packageId.equals(service.getCurrentPackageId())) {
            service.setCurrentPackageId(null);
            serverManegeMapper.save(service);
        }
        packageRepository.deleteById(packageId);
        File file = new File(servicePackage.getFilePath());
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public List<GlobalConfig> getGlobalConfigs() {
        return globalConfigRepository.findAll();
    }

    @Override
    public GlobalConfig saveGlobalConfig(GlobalConfig globalConfig) {
        Optional<GlobalConfig> old = globalConfigRepository.findByConfigKey(globalConfig.getConfigKey());
        GlobalConfig db = old.orElse(globalConfig);
        db.setConfigKey(globalConfig.getConfigKey());
        db.setConfigValue(globalConfig.getConfigValue());
        db.setDescription(globalConfig.getDescription());
        db.setUpdateTime(LocalDateTime.now());
        return globalConfigRepository.save(db);
    }

    @Override
    public void deleteGlobalConfig(String id) {
        globalConfigRepository.deleteById(id);
    }

    @Override
    public ServiceRuntimeInfo start(String serviceId) throws IOException {
        ServerBaseAttr service = findService(serviceId);
        ServiceRuntimeInfo runtimeInfo = serviceManager.startService(
                service,
                findCurrentPackage(service),
                getConfigItems(serviceId),
                serviceManager.toConfigMap(globalConfigRepository.findAll())
        );
        service.setLastStartTime(LocalDateTime.now());
        service.setStatus(runtimeInfo.getStatus());
        serverManegeMapper.save(service);
        return runtimeInfo;
    }

    @Override
    public ServiceRuntimeInfo stop(String serviceId) {
        ServiceRuntimeInfo runtimeInfo = serviceManager.stopService(serviceId);
        if (serverManegeMapper.existsById(serviceId)) {
            ServerBaseAttr service = findService(serviceId);
            service.setStatus(runtimeInfo.getStatus());
            serverManegeMapper.save(service);
        }
        return runtimeInfo;
    }

    @Override
    public ServiceRuntimeInfo restart(String serviceId) throws IOException {
        ServerBaseAttr service = findService(serviceId);
        ServiceRuntimeInfo runtimeInfo = serviceManager.restartService(
                service,
                findCurrentPackage(service),
                getConfigItems(serviceId),
                serviceManager.toConfigMap(globalConfigRepository.findAll())
        );
        service.setLastStartTime(LocalDateTime.now());
        service.setStatus(runtimeInfo.getStatus());
        serverManegeMapper.save(service);
        return runtimeInfo;
    }

    @Override
    public ServiceRuntimeInfo runtimeInfo(String serviceId) {
        findService(serviceId);
        return serviceManager.getRuntimeInfo(serviceId);
    }

    @Override
    public List<String> listLogs(String serviceId) {
        findService(serviceId);
        File[] files = serviceManager.getLogDir(serviceId).listFiles();
        List<String> names = new ArrayList<String>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    names.add(file.getName());
                }
            }
        }
        return names;
    }

    @Override
    public StreamingResponseBody tailLog(String serviceId, String fileName) {
        File logFile = resolveLogFile(serviceId, fileName);
        return outputStream -> {
            long pointer = Math.max(0, logFile.length() - 8192);
            long endTime = System.currentTimeMillis() + 300000L;
            try (RandomAccessFile accessFile = new RandomAccessFile(logFile, "r")) {
                while (System.currentTimeMillis() < endTime) {
                    long length = accessFile.length();
                    if (length < pointer) {
                        pointer = 0;
                    }
                    if (length > pointer) {
                        accessFile.seek(pointer);
                        String line;
                        while ((line = accessFile.readLine()) != null) {
                            outputStream.write((new String(line.getBytes("ISO-8859-1"), "UTF-8") + "\n").getBytes("UTF-8"));
                            outputStream.flush();
                        }
                        pointer = accessFile.getFilePointer();
                    }
                    Thread.sleep(1000L);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };
    }

    @Override
    public Resource downloadLog(String serviceId, String fileName) {
        return new FileSystemResource(resolveLogFile(serviceId, fileName));
    }

    private ServerBaseAttr findService(String id) {
        return serverManegeMapper.findById(id).orElseThrow(() -> new IllegalArgumentException("服务不存在: " + id));
    }

    private void validateService(ServerBaseAttr service) {
        if (service.getServiceName() == null || service.getServiceName().trim().isEmpty()) {
            throw new IllegalArgumentException("服务名称不能为空");
        }
        if (service.getPort() == null || service.getPort() < 1 || service.getPort() > 65535) {
            throw new IllegalArgumentException("端口必须在 1 到 65535 之间");
        }
    }

    private ServiceConfigItem findConfigItem(String id) {
        return configItemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("配置项不存在: " + id));
    }

    private ServicePackage findPackage(String id) {
        return packageRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Jar 包不存在: " + id));
    }

    private ServicePackage findCurrentPackage(ServerBaseAttr service) {
        if (service.getCurrentPackageId() == null || service.getCurrentPackageId().trim().isEmpty()) {
            throw new IllegalStateException("请先选择当前使用的 Jar 包");
        }
        return findPackage(service.getCurrentPackageId());
    }

    private File resolveLogFile(String serviceId, String fileName) {
        findService(serviceId);
        File file = new File(serviceManager.getLogDir(serviceId), safeFileName(fileName));
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("日志文件不存在: " + fileName);
        }
        return file;
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "unknown.jar";
        }
        return new File(fileName).getName();
    }

    private String sha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream inputStream = new DigestInputStream(new FileInputStream(file), digest)) {
                byte[] buffer = new byte[8192];
                while (inputStream.read(buffer) != -1) {
                    // DigestInputStream updates the digest while reading.
                }
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", ex);
        }
    }
}
