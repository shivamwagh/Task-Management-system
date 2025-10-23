package org.tms;



import org.springframework.data.jpa.repository.JpaRepository;
import org.tms.entity.Task;
import org.tms.entity.UserEntity;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserEntity(UserEntity userEntity);
}
