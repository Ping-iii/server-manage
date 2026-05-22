package com.ping.servermanage.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
public class ServerBaseAttr {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "uuid2")
    private String id;

    private String serviceName;

    private Integer port;

    private String memoryLimit;

    @Column(length = 1000)
    private String description;

    private String databaseName;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LocalDateTime lastStartTime;

    private String currentPackageId;

    public String getServerName() {
        return serviceName;
    }

    public void setServerName(String serverName) {
        this.serviceName = serverName;
    }

    public String getServerPort() {
        return port == null ? null : String.valueOf(port);
    }

    public void setServerPort(String serverPort) {
        this.port = serverPort == null || serverPort.trim().isEmpty() ? null : Integer.valueOf(serverPort);
    }

    public String getServerStatus() {
        return status;
    }

    public void setServerStatus(String serverStatus) {
        this.status = serverStatus;
    }
}
