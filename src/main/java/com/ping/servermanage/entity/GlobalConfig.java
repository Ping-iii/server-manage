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
public class GlobalConfig {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "uuid2")
    private String id;

    @Column(nullable = false, unique = true)
    private String configKey;

    @Column(name = "CONFIG_VALUE", length = 2000)
    private String configValue;

    @Column(length = 500)
    private String description;

    private LocalDateTime updateTime;
}
