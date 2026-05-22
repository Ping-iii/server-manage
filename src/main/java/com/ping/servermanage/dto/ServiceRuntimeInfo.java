package com.ping.servermanage.dto;

import lombok.Data;

import java.util.List;

@Data
public class ServiceRuntimeInfo {

    private String serviceId;

    private String status;

    private List<String> command;

    private String logFile;
}
