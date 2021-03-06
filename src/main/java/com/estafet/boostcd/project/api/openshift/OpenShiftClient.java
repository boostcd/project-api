package com.estafet.boostcd.project.api.openshift;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.estafet.boostcd.project.api.dao.ProductDAO;
import com.estafet.boostcd.project.api.model.Project;
import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IBuildTriggerable;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

@Component
public final class OpenShiftClient {

	@Autowired
	private Tracer tracer;

	@Autowired
	private ProductDAO productDAO;
	
	private IClient getClient() {
		return new ClientBuilder("https://" + System.getenv("OPENSHIFT_HOST_PORT"))
				.withUserName(System.getenv("OPENSHIFT_USER"))
				.withPassword(System.getenv("OPENSHIFT_PASSWORD"))
				.build();
	}
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	public List<IProject> getProjects(String productId) {
		Span span = tracer.buildSpan("OpenShiftClient.getProjects").start();
		try {		 
			Map<String, String> labels = new HashMap<String, String>();
			labels.put("type", productId + "-dq");
			return (List<IProject>)(List<?>) (List<IResource>) getClient().list(ResourceKind.PROJECT, labels);
		} catch (RuntimeException e) {
			throw handleException(span, e);
		} finally {
			span.finish();
		}
	}
	
	@SuppressWarnings("deprecation")
	public IProject getProject(String productId, String project) {
		Span span = tracer.buildSpan("OpenShiftClient.getProject").start();
		try {		 
			return getClient().get(ResourceKind.PROJECT, project, productId + "-cicd");
		} catch (RuntimeException e) {
			throw handleException(span, e);
		} finally {
			span.finish();
		}
	}
	
	@SuppressWarnings("deprecation")
	public void executeCreateEnviromentPipeline(String productId, Project project, String uid) {
		Span span = tracer.buildSpan("OpenShiftClient.getCreateEnviromentPipeline").start();
		try {
			executePipeline((IBuildConfig) getClient().get(ResourceKind.BUILD_CONFIG, "create-environment", productId + "-cicd"), productId, project, uid);
		} catch (RuntimeException e) {
			throw handleException(span, e);
		} finally {
			span.finish();
		}
	}
		
	private void executePipeline(IBuildConfig pipeline, String productId, Project project, String uid) {
		pipeline.accept(new CapabilityVisitor<IBuildTriggerable, IBuild>() {
            @Override
            public IBuild visit(IBuildTriggerable capability) {
            	capability.setEnvironmentVariable("PROJECT_TITLE", project.getTitle());
            	capability.setEnvironmentVariable("USER_NAME", project.getOwner());
            	capability.setEnvironmentVariable("USER_ID", uid);
            	capability.setEnvironmentVariable("PRODUCT", productId);
            	capability.setEnvironmentVariable("OPENSHIFT_HOST_PORT", System.getenv("OPENSHIFT_HOST_PORT"));
            	capability.setEnvironmentVariable("REPO", productDAO.getProduct(productId).getRepo());
                return capability.trigger();
            }
        }, null);
	}
	
	@SuppressWarnings("deprecation")
	public void deleteProject(String productId, String project) {
		Span span = tracer.buildSpan("OpenShiftClient.deleteProject").start();
		try {		 
			getClient().delete(ResourceKind.PROJECT, productId + "-cicd", project);
		} catch (RuntimeException e) {
			throw handleException(span, e);
		} finally {
			span.finish();
		}
	}
	
	@SuppressWarnings("deprecation")
	public void editProject(String productId, Project project, String namespace) {
		Span span = tracer.buildSpan("OpenShiftClient.deleteProject").start();
		try {		 
			IProject iproject = getClient().get(ResourceKind.PROJECT, namespace, productId + "-cicd");
			iproject.setDisplayName(project.getTitle());
			getClient().update(iproject);
		} catch (RuntimeException e) {
			throw handleException(span, e);
		} finally {
			span.finish();
		}
	}

	private RuntimeException handleException(Span span, RuntimeException e) {
		Tags.ERROR.set(span, true);
		Map<String, Object> logs = new HashMap<String, Object>();
		logs.put("event", "error");
		logs.put("error.object", e);
		logs.put("message", e.getMessage());
		StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
		logs.put("stack", sw.toString());
		span.log(logs);
		return e;
	}
	
}
