package cn.cmcc.diseasemonitor.repository;

import cn.cmcc.diseasemonitor.entity.Repertory;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface RepertoryRepository extends JpaRepository<Repertory, Integer>{

}
