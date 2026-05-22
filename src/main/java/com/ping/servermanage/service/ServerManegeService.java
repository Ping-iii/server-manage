package com.ping.servermanage.service;

import com.ping.servermanage.dto.ServiceDetail;
import com.ping.servermanage.dto.ServiceRuntimeInfo;
import com.ping.servermanage.entity.GlobalConfig;
import com.ping.servermanage.entity.ServerBaseAttr;
import com.ping.servermanage.entity.ServiceConfigItem;
import com.ping.servermanage.entity.ServicePackage;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;

public interface ServerManegeService {

    List<ServerBaseAttr> getAllServerList();

    ServerBaseAttr getServer(String id);

    ServiceDetail getServerDetail(String id);

    ServerBaseAttr addServer(ServerBaseAttr serverBaseAttr);

    ServerBaseAttr updateServer(String id, ServerBaseAttr serverBaseAttr);

    void deleteServer(String id);

    List<ServiceConfigItem> getConfigItems(String serviceId);

    ServiceConfigItem addConfigItem(String serviceId, ServiceConfigItem configItem);

    ServiceConfigItem updateConfigItem(String configId, ServiceConfigItem configItem);

    void deleteConfigItem(String configId);

    List<ServicePackage> getPackages(String serviceId);

    ServicePackage uploadPackage(String serviceId, String version, MultipartFile file) throws IOException;

    ServicePackage usePackage(String serviceId, String packageId);

    void deletePackage(String packageId);

    List<GlobalConfig> getGlobalConfigs();

    GlobalConfig saveGlobalConfig(GlobalConfig globalConfig);

    void deleteGlobalConfig(String id);

    ServiceRuntimeInfo start(String serviceId) throws IOException;

    ServiceRuntimeInfo stop(String serviceId);

    ServiceRuntimeInfo restart(String serviceId) throws IOException;

    ServiceRuntimeInfo runtimeInfo(String serviceId);

    List<String> listLogs(String serviceId);

    StreamingResponseBody tailLog(String serviceId, String fileName);

    Resource downloadLog(String serviceId, String fileName) throws IOException;
}
