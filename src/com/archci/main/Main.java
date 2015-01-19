package com.archci.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ITypeBinding;

import com.archci.core.DependencyConstraint;
import com.archci.dependencies.Dependency;
import com.archci.exception.DCLException;
import com.archci.exception.ParseException;
import com.archci.parser.DCLParser;
import com.archci.util.DCLUtil;

public class Main {
	public static void main(String args[]) throws DCLException, IOException, CoreException, ParseException{
		
		/*
		//String path = "/Users/arthurfp/Documents/workspace/ArchCI_exampleproject";
		String path = "/Users/arthurfp/Movies/DCL-master/dclsuite";
		File folder = new File(path);
		*/
		
		/*/Users/arthurfp/Movies/DCL-master/dclsuite*/
		
		List<File> folders = new ArrayList<File>();
		List<String> classPath = new ArrayList<String>();
		List<String> sourcePath = new ArrayList<String>();
		
		for (String arg : args) {
			
			File folder = new File(arg);
			
			classPath.addAll(DCLUtil.getPath(folder));
			sourcePath.addAll(DCLUtil.getSource(folder));
			
			folders.add(folder);
		}
				
		if(!folders.isEmpty()){		
			for (File folder : folders) {
				
				System.out.println("\nPATH: "+folder.getAbsolutePath()+"\n");
				
				Stack<File> stack = new Stack<File>();
				stack.push(folder);
					while(!stack.isEmpty()) {
						File child = stack.pop();
						if (child.isDirectory()) {
						  for(File f : child.listFiles())
							  stack.push(f);
						} else if (child.isFile() && child.getName().endsWith(".dcl")) {
							printDCLParser(child);
						} else if(child.isFile() && child.getName().endsWith(".java")) {
							printDependencies(child, classPath, sourcePath);
						}
					}
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
		
		public static void printDependencies(File file, List<String> classPath, List<String> sourcePath) throws CoreException, IOException, DCLException{
		
			File f = new File(file.getAbsolutePath());
				
			Collection<Dependency> dependencies;
			dependencies = DCLUtil.getDependenciesUsingAST(f, classPath, sourcePath);
			
			System.out.println("\nDependency List from "+file.getName()+":\n");
			
			for (Dependency dep : dependencies) {
				System.out.println(dep.toString());
			}
		}
}
