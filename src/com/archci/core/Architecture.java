package com.archci.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.dom.ITypeBinding;

import com.archci.ast.DCLDeepDependencyVisitor;
import com.archci.core.DependencyConstraint.ArchitecturalDrift;
import com.archci.dependencies.Dependency;
import com.archci.enums.DependencyType;
import com.archci.exception.DCLException;
import com.archci.exception.ParseException;
import com.archci.parser.DCLParser;
import com.archci.util.DCLUtil;

public class Architecture {
	private static final boolean DEBUG = false;

	/**
	 * String: class name Collection<Dependency>: Collection of established
	 * dependencies
	 */
	public Map<String, Collection<Dependency>> projectClasses = null;

	/**
	 * String: module name String: module description
	 */
	public Map<String, String> modules = null;

	/**
	 * Collection<DependencyConstraint>: Collection of dependency constraints
	 */
	
	/**
	 * List of all ITypeBinding
	 * 
	 */
	public List<ITypeBinding> typeBindings = null;

	
	public Collection<DependencyConstraint> dependencyConstraints = null;

	public Architecture(File projectPath) throws CoreException, ParseException, IOException, DCLException {
		if (DEBUG) {
			System.out.println("Time BEFORE generate architecture (without dependencies): " + new Date());
		}
		this.projectClasses = new HashMap<String, Collection<Dependency>>();
		this.typeBindings = new ArrayList<ITypeBinding>();
		this.modules = new ConcurrentHashMap<String, String>();
		
		List<String> classPath = new LinkedList<String>();
		List<String> sourcePath = new LinkedList<String>();
		
		classPath.addAll(DCLUtil.getPath(projectPath));
		sourcePath.addAll(DCLUtil.getSource(projectPath));
		
		String[] classPathEntries = classPath.toArray(new String[classPath.size()]);
		String[] sourcePathEntries = sourcePath.toArray(new String[sourcePath.size()]);
		
		for (File f : DCLUtil.getFilesFromProject(projectPath)) {
			DCLDeepDependencyVisitor ddv = DCLUtil.useAST(f, classPathEntries, sourcePathEntries);
			this.projectClasses.put(ddv.getClassName(), ddv.getDependencies());
			this.typeBindings.add(ddv.getITypeBinding());
		}
		
		this.initializeDependencyConstraints(DCLUtil.getDCLFile(projectPath));
		
		if (DEBUG) {
			System.out.println("Time AFTER generate architecture (without dependencies): " + new Date());
		}
	}

	private void initializeDependencyConstraints(File dclFile) throws CoreException, ParseException {
		try {
			if(dclFile!=null){
				File dcl = new File(dclFile.getAbsolutePath());
				InputStream inputStream = new FileInputStream(dcl);
				
				this.modules.putAll(DCLParser.parseModules(inputStream));
	
				/* Define implicit modules */
				this.modules.put("$java", DCLUtil.getJavaModuleDefinition());
				/*
				 * Module $system has its behavior in
				 * DCLUtil.hasClassNameByDescription
				 */
	
				this.dependencyConstraints = DCLParser.parseDependencyConstraints(inputStream);
			}
			else this.dependencyConstraints = new LinkedList<DependencyConstraint>();
		} catch (ParseException e) {
			throw e;
		} catch (Throwable e) {
			//MarkerUtils.addErrorMarker(project, "The " + DCLUtil.DC_FILENAME + " is invalid.");
			throw new CoreException(Status.CANCEL_STATUS);
		}
	}

	public void updateDependencyConstraints(File projectPath) throws CoreException, ParseException {
		this.modules.clear();
		System.gc(); /* Suggesting the execution of the Garbage Collector */
		this.initializeDependencyConstraints(projectPath);
	}

	public Set<String> getProjectClasses() {
		return projectClasses.keySet();
	}

	public Collection<Dependency> getDependencies(String className) {
		return projectClasses.get(className);
	}

	public Dependency getDependency(String classNameA, String classNameB, Integer lineNumberA, DependencyType dependencyType) {
		Collection<Dependency> dependencies = projectClasses.get(classNameA);
		for (Dependency d : dependencies) {
			if (lineNumberA == null) {

			}
			if ((lineNumberA == null) ? d.getLineNumber() == null : lineNumberA.equals(d.getLineNumber())
					&& d.getClassNameB().equals(classNameB) && d.getDependencyType().equals(dependencyType)) {
				return d;
			}
		}
		return null;
	}

	public void updateDependencies(String className, Collection<Dependency> dependencies) {
		projectClasses.put(className, dependencies);
	}

	public Collection<DependencyConstraint> getDependencyConstraints() {
		return this.dependencyConstraints;
	}

	public Map<String, String> getModules() {
		return this.modules;
	}

	/**
	 * Method used to check if a particular dependency is allowed or not. It is
	 * used, e.g., for the DCLfix module.
	 */
	public boolean can(String classNameA, String classNameB, DependencyType dependencyType, List<ITypeBinding> typeBindings) throws CoreException {
		final Collection<Dependency> dependencies = new ArrayList<Dependency>(1);
		dependencies.add(dependencyType.createGenericDependency(classNameA, classNameB));

		for (DependencyConstraint dc : this.getDependencyConstraints()) {
			List<ArchitecturalDrift> violations = dc.validate(classNameA, modules, this.getProjectClasses(), dependencies, typeBindings);
			if (violations != null && !violations.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Method used to check if some class of the system is allowed to establish
	 * a particular dependency. It is used, e.g., for the DCLfix module.
	 */
	public boolean someclassCan(String classNameB, DependencyType dependencyType, List<ITypeBinding> typeBindings) throws CoreException {

		for (String classNameA : this.getProjectClasses()) {
			if (classNameA.equals(classNameB)) {
				continue;
			}

			final Collection<Dependency> dependencies = new ArrayList<Dependency>(1);
			dependencies.add(dependencyType.createGenericDependency(classNameA, classNameB));

			boolean flag = true; /* It initially considers that it can */
			for (DependencyConstraint dc : this.getDependencyConstraints()) {
				/*
				 * Case we find some violation in this dependency in any
				 * dependency constraint, we set flag false
				 */
				if (dc.validate(classNameA, modules, this.getProjectClasses(), dependencies, typeBindings) != null
						&& !dc.validate(classNameA, modules, this.getProjectClasses(), dependencies, typeBindings).isEmpty()) {
					flag = false;
				}
			}
			/* If we did not find any violation for this class, it can! */
			if (flag) {
				return true;
			}
		}

		return false;
	}

	public Set<String> getUsedClasses(final String className) {
		Set<String> set = new HashSet<String>();

		for (Dependency d : this.getDependencies(className)) {
			set.add(d.getClassNameB());
		}

		return set;
	}

	public Set<String> getUsedClasses(final String className, DependencyType dependencyType) {
		/* In this case, it only considers the type */
		if (dependencyType == null) {
			return getUsedClasses(className);
		}

		Set<String> set = new HashSet<String>();

		/*
		 * Here, two cases: 
		 * if (dependencyType == DEPEND) -> dep[*,T] 
		 * if (dependencyType == other) -> dep[other,T] 
		 * For example: dep[access,T]
		 */
		for (Dependency d : this.getDependencies(className)) {
			if (dependencyType.equals(DependencyType.DEPEND) || d.getDependencyType().equals(dependencyType)) {
				set.add("dep[" + d.getDependencyType().getValue() + "," + d.getClassNameB() + "]");	
			}
		}

		return set;
	}

	public Set<String> getUniverseOfUsedClasses() {
		Set<String> set = new HashSet<String>();

		for (Collection<Dependency> col : projectClasses.values()) {
			for (Dependency d : col) {
				set.add(d.getClassNameB());
			}
		}

		return set;
	}

	public Set<String> getUniverseOfUsedClasses(DependencyType dependencyType) {
		if (dependencyType == null) {
			return getUniverseOfUsedClasses();
		}
		Set<String> set = new HashSet<String>();

		/*
		 * Here, two cases: 
		 * if (dependencyType == DEPEND) -> dep[*,T] 
		 * if (dependencyType == other) -> dep[other,T] 
		 * For example: dep[access,T]
		 */
		for (Collection<Dependency> col : projectClasses.values()) {
			for (Dependency d : col) {
				if (dependencyType.equals(DependencyType.DEPEND) || d.getDependencyType().equals(dependencyType)) {
					set.add("dep[" + d.getDependencyType().getValue() + "," + d.getClassNameB() + "]");	
				}
			}
		}

		return set;
	}
	
	
	
	
	public List<String> getUsedClasses2(final String className) {
		List<String> list = new ArrayList<String>();

		for (Dependency d : this.getDependencies(className)) {
			list.add(d.getClassNameB());
		}

		return list;
	}

	public List<String> getUsedClasses2(final String className, DependencyType dependencyType) {
		/* In this case, it only considers the type */
		if (dependencyType == null) {
			return getUsedClasses2(className);
		}

		List<String> list = new ArrayList<String>();

		/*
		 * Here, two cases: 
		 * if (dependencyType == DEPEND) -> dep[*,T] 
		 * if (dependencyType == other) -> dep[other,T] 
		 * For example: dep[access,T]
		 */
		for (Dependency d : this.getDependencies(className)) {
			if (dependencyType.equals(DependencyType.DEPEND) || d.getDependencyType().equals(dependencyType)) {
				list.add("dep[" + d.getDependencyType().getValue() + "," + d.getClassNameB() + "]");	
			}
		}

		return list;
	}

	public List<String> getUniverseOfUsedClasses2() {
		List<String> list = new ArrayList<String>();

		for (Collection<Dependency> col : projectClasses.values()) {
			for (Dependency d : col) {
				list.add(d.getClassNameB());
			}
		}

		return list;
	}

	public List<String> getUniverseOfUsedClasses2(DependencyType dependencyType) {
		if (dependencyType == null) {
			return getUniverseOfUsedClasses2();
		}
		List<String> list = new ArrayList<String>();

		/*
		 * Here, two cases: 
		 * if (dependencyType == DEPEND) -> dep[*,T] 
		 * if (dependencyType == other) -> dep[other,T] 
		 * For example: dep[access,T]
		 */
		for (Collection<Dependency> col : projectClasses.values()) {
			for (Dependency d : col) {
				if (dependencyType.equals(DependencyType.DEPEND) || d.getDependencyType().equals(dependencyType)) {
					list.add("dep[" + d.getDependencyType().getValue() + "," + d.getClassNameB() + "]");	
				}
			}
		}

		return list;
	}
	
	public List<ITypeBinding> getITypeBindings(){
		return typeBindings;
	}
	

}
