package com.ping.servermanage.controller;

import com.ping.servermanage.dto.ServiceDetail;
import com.ping.servermanage.dto.ServiceRuntimeInfo;
import com.ping.servermanage.entity.GlobalConfig;
import com.ping.servermanage.entity.ServerBaseAttr;
import com.ping.servermanage.entity.ServiceConfigItem;
import com.ping.servermanage.entity.ServicePackage;
import com.ping.servermanage.service.ServerManegeService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/serverManege")
public class ServerManageController {

    @javax.annotation.Resource
    private ServerManegeService serverManegeService;

    @GetMapping("/getAllServerList")
    public List<ServerBaseAttr> getAllServerList() {
        return serverManegeService.getAllServerList();
    }

    @PostMapping("/addServer")
    public ServerBaseAttr addServer(ServerBaseAttr serverBaseAttr) {
        return serverManegeService.addServer(serverBaseAttr);
    }

    @GetMapping("/services")
    public List<ServerBaseAttr> services() {
        return serverManegeService.getAllServerList();
    }

    @PostMapping("/services")
    public ServerBaseAttr createService(@RequestBody ServerBaseAttr service) {
        return serverManegeService.addServer(service);
    }

    @GetMapping("/services/{id}")
    public ServiceDetail serviceDetail(@PathVariable String id) {
        return serverManegeService.getServerDetail(id);
    }

    @PutMapping("/services/{id}")
    public ServerBaseAttr updateService(@PathVariable String id, @RequestBody ServerBaseAttr service) {
        return serverManegeService.updateServer(id, service);
    }

    @DeleteMapping("/services/{id}")
    public Map<String, String> deleteService(@PathVariable String id) {
        serverManegeService.deleteServer(id);
        return success();
    }

    @GetMapping("/services/{serviceId}/configs")
    public List<ServiceConfigItem> configItems(@PathVariable String serviceId) {
        return serverManegeService.getConfigItems(serviceId);
    }

    @PostMapping("/services/{serviceId}/configs")
    public ServiceConfigItem addConfigItem(@PathVariable String serviceId, @RequestBody ServiceConfigItem configItem) {
        return serverManegeService.addConfigItem(serviceId, configItem);
    }

    @PutMapping("/configs/{configId}")
    public ServiceConfigItem updateConfigItem(@PathVariable String configId, @RequestBody ServiceConfigItem configItem) {
        return serverManegeService.updateConfigItem(configId, configItem);
    }

    @DeleteMapping("/configs/{configId}")
    public Map<String, String> deleteConfigItem(@PathVariable String configId) {
        serverManegeService.deleteConfigItem(configId);
        return success();
    }

    @GetMapping("/services/{serviceId}/packages")
    public List<ServicePackage> packages(@PathVariable String serviceId) {
        return serverManegeService.getPackages(serviceId);
    }

    @PostMapping(value = "/services/{serviceId}/packages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServicePackage uploadPackage(@PathVariable String serviceId,
                                        @RequestParam String version,
                                        @RequestParam MultipartFile file) throws IOException {
        return serverManegeService.uploadPackage(serviceId, version, file);
    }

    @PostMapping("/services/{serviceId}/packages/{packageId}/use")
    public ServicePackage usePackage(@PathVariable String serviceId, @PathVariable String packageId) {
        return serverManegeService.usePackage(serviceId, packageId);
    }

    @DeleteMapping("/packages/{packageId}")
    public Map<String, String> deletePackage(@PathVariable String packageId) {
        serverManegeService.deletePackage(packageId);
        return success();
    }

    @GetMapping("/global-configs")
    public List<GlobalConfig> globalConfigs() {
        return serverManegeService.getGlobalConfigs();
    }

    @PostMapping("/global-configs")
    public GlobalConfig saveGlobalConfig(@RequestBody GlobalConfig globalConfig) {
        return serverManegeService.saveGlobalConfig(globalConfig);
    }

    @DeleteMapping("/global-configs/{id}")
    public Map<String, String> deleteGlobalConfig(@PathVariable String id) {
        serverManegeService.deleteGlobalConfig(id);
        return success();
    }

    @PostMapping("/services/{serviceId}/start")
    public ServiceRuntimeInfo start(@PathVariable String serviceId) throws IOException {
        return serverManegeService.start(serviceId);
    }

    @PostMapping("/services/{serviceId}/stop")
    public ServiceRuntimeInfo stop(@PathVariable String serviceId) {
        return serverManegeService.stop(serviceId);
    }

    @PostMapping("/services/{serviceId}/restart")
    public ServiceRuntimeInfo restart(@PathVariable String serviceId) throws IOException {
        return serverManegeService.restart(serviceId);
    }

    @GetMapping("/services/{serviceId}/runtime")
    public ServiceRuntimeInfo runtime(@PathVariable String serviceId) {
        return serverManegeService.runtimeInfo(serviceId);
    }

    @GetMapping("/services/{serviceId}/logs")
    public List<String> logs(@PathVariable String serviceId) {
        return serverManegeService.listLogs(serviceId);
    }

    @GetMapping(value = "/services/{serviceId}/logs/{fileName:.+}/tail", produces = MediaType.TEXT_PLAIN_VALUE)
    public StreamingResponseBody tailLog(@PathVariable String serviceId, @PathVariable String fileName) {
        return serverManegeService.tailLog(serviceId, fileName);
    }

    @GetMapping("/services/{serviceId}/logs/{fileName:.+}/download")
    public ResponseEntity<Resource> downloadLog(@PathVariable String serviceId, @PathVariable String fileName) throws IOException {
        Resource resource = serverManegeService.downloadLog(serviceId, fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        Map<String, String> result = new HashMap<String, String>();
        result.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(result);
    }

    private Map<String, String> success() {
        Map<String, String> result = new HashMap<String, String>();
        result.put("message", "success");
        return result;
    }
}
