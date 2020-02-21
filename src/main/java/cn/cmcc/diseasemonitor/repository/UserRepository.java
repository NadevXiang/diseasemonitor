package cn.cmcc.diseasemonitor.repository;

import cn.cmcc.diseasemonitor.entity.User;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>{

    User findByUserName(String username);

}
