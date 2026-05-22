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
public class ServiceConfigItem {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "uuid2")
    private String id;

    @Column(nullable = false)
    private String serviceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String parameterPath;

    @Column(name = "CONFIG_VALUE", length = 2000)
    private String value;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
