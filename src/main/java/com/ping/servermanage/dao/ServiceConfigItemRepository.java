package com.ping.servermanage.dao;

import com.ping.servermanage.entity.ServiceConfigItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceConfigItemRepository extends JpaRepository<ServiceConfigItem, String> {

    List<ServiceConfigItem> findByServiceIdOrderByCreateTimeAsc(String serviceId);

    void deleteByServiceId(String serviceId);
}
