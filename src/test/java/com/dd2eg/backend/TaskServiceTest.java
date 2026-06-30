package com.dd2eg.backend;

import com.dd2eg.backend.DTO.*;
import com.dd2eg.backend.model.*;
import com.dd2eg.backend.repository.*;
import com.dd2eg.backend.service.ProjectService;
import com.dd2eg.backend.service.TaskService;
import com.dd2eg.backend.utils.TaskStatus;
import com.dd2eg.backend.utils.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserMongoRepository userRepository;

    @Mock
    private ProjectMongoRepository projectRepository;


    @Mock
    private ProjectService projectService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void fundTask_ShouldThrowException_WhenEnterpriseIsNull() {
        FundTaskRequestDTO request = new FundTaskRequestDTO();
        assertThrows(RuntimeException.class, () -> {
            taskService.fundTask("task1", request, null);
        }, "Enterprise is null");
    }

    @Test
    void fundTask_ShouldThrowException_WhenUserIsNotEnterprise() {
        User dev = new User();
        dev.setUserType(UserType.DEVELOPER);
        FundTaskRequestDTO request = new FundTaskRequestDTO();

        assertThrows(RuntimeException.class, () -> {
            taskService.fundTask("task1", request, dev);
        }, "Only enterprises can fund tasks");
    }

    @Test
    void fundTask_ShouldThrowException_WhenTaskNotFound() {
        User ent = new User();
        ent.setUserType(UserType.ENTERPRISE);
        FundTaskRequestDTO request = new FundTaskRequestDTO();
        when(taskRepository.findById("task1")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            taskService.fundTask("task1", request, ent);
        }, "Task not found");
    }

    @Test
    void fundTask_ShouldThrowException_WhenAmountIsZeroOrNegative() {
        User ent = new User();
        ent.setUserType(UserType.ENTERPRISE);
        FundTaskRequestDTO request = new FundTaskRequestDTO();
        request.setAmount(0);

        Task task = new Task();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));

        assertThrows(IllegalArgumentException.class, () -> {
            taskService.fundTask("task1", request, ent);
        }, "Funding amount must be greater than zero");
    }

    @Test
    void fundTask_ShouldSaveSponsorshipAndFireEvent() {
        User ent = new User();
        ent.setId("ent1");
        ent.setUsername("enterprise1");
        ent.setUserType(UserType.ENTERPRISE);

        FundTaskRequestDTO request = new FundTaskRequestDTO();
        request.setAmount(500);

        Task task = new Task();
        task.setId("task1");
        task.setSponsorships(new ArrayList<>());

        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.fundTask("task1", request, ent);

        assertNotNull(result);
        assertEquals(1, result.getSponsorships().size());
        assertEquals("enterprise1", result.getSponsorships().get(0).getName());
        assertEquals(500, result.getSponsorships().get(0).getAmount());

        verify(eventRepository).save(any(Event.class));
        verify(taskRepository).save(task);
    }

    @Test
    void addCommentToTask_ShouldThrowException_WhenTaskNotFound() {
        User author = new User();
        assertThrows(RuntimeException.class, () -> {
            taskService.addCommentToTask("task1", "Nice task!", author);
        });
    }

    @Test
    void addCommentToTask_ShouldAddCommentAndSave() {
        User author = new User();
        author.setId("author1");

        Task task = new Task();
        task.setId("task1");
        task.setComments(new ArrayList<>());

        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.addCommentToTask("task1", "Good job", author);

        assertNotNull(result);
        assertEquals(1, result.getComments().size());
        assertEquals("Good job", result.getComments().get(0).getContent());
        assertEquals("author1", result.getComments().get(0).getAuthorId());

        verify(taskRepository).save(task);
    }

    @Test
    void addCommitToTask_ShouldThrowException_WhenUserIsNull() {
        CreateCommitDTO dto = new CreateCommitDTO();
        assertThrows(RuntimeException.class, () -> {
            taskService.addCommitToTask("task1", dto, null);
        }, "Authenticated user is required to commit on a task");
    }

    @Test
    void addCommitToTask_ShouldThrowException_WhenTaskNotFound() {
        CreateCommitDTO dto = new CreateCommitDTO();
        User dev = new User();
        when(taskRepository.findById("task1")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            taskService.addCommitToTask("task1", dto, dev);
        }, "Task not found");
    }

    @Test
    void addCommitToTask_ShouldThrowException_WhenTaskDoesNotBelongToProject() {
        CreateCommitDTO dto = new CreateCommitDTO();
        User dev = new User();

        Task task = new Task();
        task.setProjectId(null); // or empty

        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));

        assertThrows(RuntimeException.class, () -> {
            taskService.addCommitToTask("task1", dto, dev);
        }, "Task does not belong to a project");
    }

    @Test
    void addCommitToTask_ShouldThrowException_WhenMaxCommitsReached() {
        CreateCommitDTO dto = new CreateCommitDTO();
        User dev = new User();

        Task task = new Task(1);
        task.setNumMaxCommits(1);
        task.setProjectId("proj1");
        // Pre-fill the commit to make current index 1 (max commits is 1)
        task.getCommits().get(0).setHash("hash123");

        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));

        assertThrows(RuntimeException.class, () -> {
            taskService.addCommitToTask("task1", dto, dev);
        }, "Max commits reached for this task");
    }

    @Test
    void addCommitToTask_ShouldSaveCommitAndFireEvent() {
        CreateCommitDTO dto = new CreateCommitDTO();
        dto.setHash("hash123");
        dto.setComment("Fixed issue");
        dto.setNumLines(45);

        User dev = new User();
        dev.setId("dev1");
        dev.setUsername("developer1");

        Task task = new Task(2);
        task.setNumMaxCommits(2);
        task.setId("task1");
        task.setProjectId("proj1");

        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Commit result = taskService.addCommitToTask("task1", dto, dev);

        assertNotNull(result);
        assertEquals("hash123", result.getHash());
        assertEquals("Fixed issue", result.getComment());
        assertEquals(45, result.getNumLines());
        assertEquals("task1", task.getId());
        assertEquals("proj1", task.getProjectId());
        assertEquals("dev1", result.getAuthorId());
        assertEquals("developer1", result.getAuthorUsername());

        verify(projectService).addContributorToProjectIfMissing("proj1", dev);
        verify(eventRepository).save(any(Event.class));
        verify(taskRepository).save(task);
    }

    @Test
    void getTasksByProjectId_ShouldReturnList() {
        Task t1 = new Task();
        Task t2 = new Task();
        when(taskRepository.findByProjectId("proj1")).thenReturn(List.of(t1, t2));

        List<Task> result = taskService.getTasksByProjectId("proj1");

        assertEquals(2, result.size());
    }

    @Test
    void createTask_ShouldThrowException_WhenProjectNotFound() {
        CreateTaskDTO request = new CreateTaskDTO();
        request.setProjectId("proj1");
        User dev = new User();
        when(projectRepository.findById("proj1")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            taskService.createTask(request, dev);
        });
    }

    @Test
    void createTask_ShouldSaveTask() {
        CreateTaskDTO request = new CreateTaskDTO();
        request.setProjectId("proj1");
        request.setTitle("New Task");
        request.setDescription("Desc");
        request.setBody("Body");
        request.setPriority("HIGH");
        request.setNumMaxCommits(3);
        request.setSkills(List.of("Java"));

        Project project = new Project();
        project.setId("proj1");

        User dev = new User();

        when(projectRepository.findById("proj1")).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.createTask(request, dev);

        assertNotNull(result);
        assertEquals("New Task", result.getTitle());
        assertEquals("Desc", result.getDescription());
        assertEquals("Body", result.getBody());
        assertEquals("HIGH", result.getPriority());
        assertEquals(3, result.getNumMaxCommits());
        assertEquals(List.of("Java"), result.getSkills());
        assertEquals("proj1", result.getProjectId());
        assertEquals(TaskStatus.PENDING, result.getStatus());

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void acceptTask_ShouldThrowException_WhenTaskNotFound() {
        User dev = new User();
        when(taskRepository.findById("task1")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            taskService.acceptTask("task1", dev);
        });
    }

    @Test
    void acceptTask_ShouldThrowException_WhenStatusIsNotPending() {
        User dev = new User();
        Task task = new Task();
        task.setStatus(TaskStatus.OPEN);
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));

        assertThrows(RuntimeException.class, () -> {
            taskService.acceptTask("task1", dev);
        });
    }

    @Test
    void acceptTask_ShouldThrowException_WhenProjectNotFound() {
        User dev = new User();
        Task task = new Task();
        task.setStatus(TaskStatus.PENDING);
        task.setProjectId("proj1");

        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        when(projectRepository.findById("proj1")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            taskService.acceptTask("task1", dev);
        });
    }

    @Test
    void acceptTask_ShouldThrowException_WhenUserIsNotProjectCreator() {
        User dev = new User();
        dev.setId("differentUser");

        Task task = new Task();
        task.setStatus(TaskStatus.PENDING);
        task.setProjectId("proj1");

        Project project = new Project();
        project.setCreatorId("creatorUser");

        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        when(projectRepository.findById("proj1")).thenReturn(Optional.of(project));

        assertThrows(RuntimeException.class, () -> {
            taskService.acceptTask("task1", dev);
        });
    }

    @Test
    void acceptTask_ShouldUpdateStatusAndFireEvent() {
        User dev = new User();
        dev.setId("creatorUser");

        Task task = new Task();
        task.setId("task1");
        task.setStatus(TaskStatus.PENDING);
        task.setProjectId("proj1");
        task.setSkills(List.of("Java"));

        Project project = new Project();
        project.setId("proj1");
        project.setCreatorId("creatorUser");

        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        when(projectRepository.findById("proj1")).thenReturn(Optional.of(project));
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.acceptTask("task1", dev);

        assertNotNull(result);
        assertEquals(TaskStatus.OPEN, result.getStatus());

        verify(eventRepository).save(any(Event.class));
        verify(taskRepository).save(task);
    }

    @Test
    void getTaskById_ShouldThrowException_WhenNotFound() {
        when(taskRepository.findById("task1")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            taskService.getTaskById("task1");
        });
    }

    @Test
    void getTaskById_ShouldReturnTask_WhenFound() {
        Task task = new Task();
        task.setId("task1");
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));

        Task result = taskService.getTaskById("task1");

        assertNotNull(result);
        assertEquals("task1", result.getId());
    }
}
