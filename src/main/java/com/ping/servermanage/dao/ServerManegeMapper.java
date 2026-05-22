package com.ping.servermanage.dao;

import com.ping.servermanage.entity.ServerBaseAttr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerManegeMapper extends JpaRepository<ServerBaseAttr, String> {
}
