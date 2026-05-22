package com.ping.servermanage.dao;

import com.ping.servermanage.entity.GlobalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalConfigRepository extends JpaRepository<GlobalConfig, String> {

    Optional<GlobalConfig> findByConfigKey(String configKey);
}
