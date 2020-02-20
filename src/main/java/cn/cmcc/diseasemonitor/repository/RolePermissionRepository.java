package cn.cmcc.diseasemonitor.repository;

import cn.cmcc.diseasemonitor.entity.RolePermission;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer>{

}
