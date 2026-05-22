package com.ping.servermanage.dao;

import com.ping.servermanage.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, String> {

    List<ServicePackage> findByServiceIdOrderByUploadTimeDesc(String serviceId);

    List<ServicePackage> findByServiceId(String serviceId);

    void deleteByServiceId(String serviceId);
}
