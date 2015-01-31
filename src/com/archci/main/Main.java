package com.archci.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import com.archci.ast.DCLDeepDependencyVisitor;
import com.archci.core.DependencyConstraint;
import com.archci.dependencies.Dependency;
import com.archci.exception.DCLException;
import com.archci.exception.ParseException;
import com.archci.parser.DCLParser;
import com.archci.util.DCLUtil;

public class Main {
	public static void main(String args[]) throws DCLException, IOException, CoreException, ParseException{
		if(args.length>0){
			
			for (String arg : args) {
				
				File folder = new File(arg);
				
				System.out.println("PATH: "+folder.getAbsolutePath());
				
				File dclFile = DCLUtil.getDCLFile(folder);
				
				if(dclFile!=null) printDCLParser(dclFile);
				printDependencies(folder);
			}
		}
		else System.out.println("No arguments (path) passed");
	}
		
		public static void printDCLParser(File file) throws IOException, ParseException{
			File dcl = new File(file.getAbsolutePath());
			
			InputStream inputStream = new FileInputStream(dcl);
			
			Map<String, String> moduleDesc;
			moduleDesc = DCLParser.parseModules(inputStream);
			
			inputStream = new FileInputStream(dcl);
			
			Collection<DependencyConstraint> dependencyConst = new LinkedList<DependencyConstraint>();
			dependencyConst = DCLParser.parseDependencyConstraints(inputStream);
			
			System.out.println("\n===================\nModules:\n");
			System.out.println(moduleDesc.toString());
			
			System.out.println("\n===================\nDependency Constraints:\n");
			for (DependencyConstraint dc : dependencyConst) {
				System.out.println(dc);
				System.out.println("-------\n\n");
			}
		}
		
		public static void printDependencies(File projectPath) throws CoreException, IOException, DCLException, ParseException{
			List<String> classPath = new LinkedList<String>();
			List<String> sourcePath = new LinkedList<String>();
			
			classPath.addAll(DCLUtil.getPath(projectPath));
			sourcePath.addAll(DCLUtil.getSource(projectPath));
			
			String[] classPathEntries = classPath.toArray(new String[classPath.size()]);
			String[] sourcePathEntries = sourcePath.toArray(new String[sourcePath.size()]);

			for (File f : DCLUtil.getFilesFromProject(projectPath)) {
				DCLDeepDependencyVisitor ddv = DCLUtil.useAST(f, classPathEntries, sourcePathEntries);
			
				Collection<Dependency> dependencies;
				dependencies = ddv.getDependencies();
				
				System.out.println("\nDependency List from "+ddv.getClassName()+":\n");
				
				for (Dependency dep : dependencies) {
					System.out.println(dep.toString());
				}
			}
		}
	}
