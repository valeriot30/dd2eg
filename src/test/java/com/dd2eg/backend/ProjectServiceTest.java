package com.dd2eg.backend;

import com.dd2eg.backend.DTO.CreateProjectDTO;
import com.dd2eg.backend.model.Event;
import com.dd2eg.backend.model.Project;
import com.dd2eg.backend.model.User;
import com.dd2eg.backend.repository.EventRepository;
import com.dd2eg.backend.repository.ProjectMongoRepository;
import com.dd2eg.backend.service.ProjectService;
import com.dd2eg.backend.utils.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectMongoRepository projectRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_ShouldSaveProjectAndFireEvent() {

        User mockUser = new User();
        mockUser.setUsername("johndoe");

        CreateProjectDTO newProjectDTO = new CreateProjectDTO();
        newProjectDTO.setName("Test Project");
        newProjectDTO.setDescription("Test Project description");

        Project mockSavedProject = new Project();
        mockSavedProject.setName("Test Project");
        mockSavedProject.setCreatorId("johndoe");
        mockSavedProject.setDescription("Test Project description");
        mockSavedProject.setStatus(ProjectStatus.OPEN);
        when(projectRepository.save(any(Project.class))).thenReturn(mockSavedProject);

        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        Project result = projectService.createProject(newProjectDTO, mockUser);

        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        assertEquals("johndoe", result.getCreatorId());

        verify(projectRepository, times(1)).save(any(Project.class));
        verify(eventRepository, times(1)).save(any(Event.class));
    }
}