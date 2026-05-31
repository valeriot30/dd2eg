package com.dd2eg.backend.projects;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class ProjectService {

    private ProjectMongoRepository projectRepository;

    /**
     * Retrieve all projects
     * @return
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }
}
