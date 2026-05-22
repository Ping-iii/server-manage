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
public class ServicePackage {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "uuid2")
    private String id;

    @Column(nullable = false)
    private String serviceId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false, length = 1000)
    private String filePath;

    private Long fileSize;

    @Column(length = 128)
    private String sha256;

    private Boolean currentUsed;

    private LocalDateTime uploadTime;
}
