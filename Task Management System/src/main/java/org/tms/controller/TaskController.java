package org.tms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.tms.TaskRepository;
import org.tms.audit.AuditService;
import org.tms.entity.Task;
import org.tms.entity.UserEntity;
import org.tms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditService auditService;

    @PostMapping
    public Task createTask(@RequestBody Task task, Principal principal, HttpServletRequest httpRequest) {
        UserEntity userEntity = userRepository.findByUsername(principal.getName()).orElseThrow();
        task.setUser(userEntity);
        Task saved = taskRepository.save(task);
        log.info("Task created by {}: id={}, title={}", principal.getName(), saved.getId(), saved.getTitle());
        auditService.audit("TASK_CREATED", principal.getName(), "id=" + saved.getId());
        auditService.auditDb(
                "TASK_CREATED",
                principal.getName(),
                task,
                saved,
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                null
        );
        return saved;
    }

    @GetMapping
    public List<Task> getUserTasks(Principal principal, HttpServletRequest httpRequest) {
        UserEntity userEntity = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<Task> tasks = taskRepository.findByUserEntity(userEntity);
        log.info("Tasks fetched for user {}: count={}", principal.getName(), tasks.size());
        auditService.audit("TASK_LIST_VIEWED", principal.getName(), "count=" + tasks.size());
        auditService.auditDb(
                "TASK_LIST_VIEWED",
                principal.getName(),
                null,
                tasks,
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                null
        );
        return tasks;
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @RequestBody Task updatedTask, HttpServletRequest httpRequest) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setTitle(updatedTask.getTitle());
        task.setDescription(updatedTask.getDescription());
        task.setStatus(updatedTask.getStatus());
        task.setPriority(updatedTask.getPriority());
        Task saved = taskRepository.save(task);
        log.info("Task updated: id={}, title={}", saved.getId(), saved.getTitle());
        auditService.audit("TASK_UPDATED", null, "id=" + saved.getId());
        auditService.auditDb(
                "TASK_UPDATED",
                null,
                updatedTask,
                saved,
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                null
        );
        return saved;
    }

    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable Long id, HttpServletRequest httpRequest) {
        taskRepository.deleteById(id);
        log.info("Task deleted: id={}", id);
        auditService.audit("TASK_DELETED", null, "id=" + id);
        auditService.auditDb(
                "TASK_DELETED",
                null,
                id,
                "Task deleted",
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                null
        );
        return "Task deleted";
    }
}
