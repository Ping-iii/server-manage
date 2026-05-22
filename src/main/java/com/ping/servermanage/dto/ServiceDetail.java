package com.ping.servermanage.dto;

import com.ping.servermanage.entity.ServerBaseAttr;
import com.ping.servermanage.entity.ServiceConfigItem;
import com.ping.servermanage.entity.ServicePackage;
import lombok.Data;

import java.util.List;

@Data
public class ServiceDetail {

    private ServerBaseAttr service;

    private List<ServiceConfigItem> configItems;

    private List<ServicePackage> packages;

    private ServiceRuntimeInfo runtimeInfo;
}
