package com.estafet.boostcd.project.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.estafet.boostcd.project.api.model.Project;
import com.estafet.boostcd.project.api.service.ProjectService;
import com.estafet.openshift.boost.commons.lib.model.API;

@RestController
public class ProjectController {

	@Autowired
	private ProjectService projectService;

	@Value("${app.version}")
	private String appVersion;

	@GetMapping("/api")
	public API getAPI() {
		return new API(appVersion);
	}

	@GetMapping("/projects")
	public List<Project> getProjects() {
		return projectService.getProjects();
	}
	
	@GetMapping("/project/{namespace}")
	public Project getProject(@PathVariable String namespace) {
		return projectService.getProject(namespace);
	}

	@PostMapping("/project")
	public ResponseEntity<String> createProject(@RequestBody Project project) {
		return new ResponseEntity<String>(projectService.createProject(project), HttpStatus.OK);
	}
	
	@DeleteMapping("/project/{project}")
	public ResponseEntity<String> deleteProject(@PathVariable String project) {
		return new ResponseEntity<String>(projectService.deleteProject(project), HttpStatus.OK);
	}
	
	@PutMapping("/project/{namespace}")
	public ResponseEntity<String> deleteProject(@PathVariable String namespace, @RequestBody Project project) {
		return new ResponseEntity<String>(projectService.editProject(project, namespace), HttpStatus.OK);
	}

}