package com.archci.builder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.kohsuke.stapler.DataBoundConstructor;

import com.archci.core.Architecture;
import com.archci.core.DependencyConstraint;
import com.archci.core.DependencyConstraint.ArchitecturalDrift;
import com.archci.exception.DCLException;
import com.archci.exception.ParseException;
import com.archci.parser.DCLParser;
import com.archci.util.DCLUtil;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
 
public final class DCLBuilder extends Builder {
	
	@DataBoundConstructor
	public DCLBuilder() {
	}
 
  @Extension
  public static final class ArchCIBuilderDescriptor extends BuildStepDescriptor<Builder> {
 
	  @Override
	  public String getDisplayName() {
		  return "ArchCI Builder";
	  }
	 
	  @Override
	  public boolean isApplicable(@SuppressWarnings("rawtypes") final Class<? extends AbstractProject> jobType) {
		  return true;
	  }
 
  }
 
  @Override
  public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
	 //this.getDescriptor().clazz;
	 //build.getParent().getWorkspace();
	 //build.getParent().getModuleRoot();
	  listener.getLogger().println("Workspace: "+build.getWorkspace().toString());
	  
	 
	try {
	
		FilePath projectPath = build.getParent().getWorkspace();
		
		 Architecture architecture = new Architecture(projectPath);	

		 List<ArchitecturalDrift> architecturalDrifts = new LinkedList<DependencyConstraint.ArchitecturalDrift>();
		 
		 listener.getLogger().println("MODULES: "+architecture.getModules().toString());

		 for (String classUnderValidation : architecture.getProjectClasses()) {
			 for (DependencyConstraint dc : architecture.getDependencyConstraints()) {
				 Collection<ArchitecturalDrift> result = dc.validate(classUnderValidation, architecture.getModules(),
						 architecture.getProjectClasses(), architecture.getDependencies(classUnderValidation), architecture.getITypeBindings());
				 if (result != null && !result.isEmpty()) {
					 architecturalDrifts.addAll(result);
				 }
			 }
		 }
		 
		 if(architecturalDrifts.size()>0) {
			 int i =0;
			 for(ArchitecturalDrift ad: architecturalDrifts){
				 i++;
				 listener.getLogger().println("VIOLATION "+i+": "+ad.getDetailedMessage());
			 }
			 return false;
		 }
		 else return true;
	
	 } catch (CoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return false;
	
	 } catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return false;
	
	 } catch (DCLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return false;
	}

  }
 
}