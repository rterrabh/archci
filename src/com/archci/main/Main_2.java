/*package com.archci.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;

import com.archci.ast.DCLDeepDependencyVisitor;
import com.archci.core.DependencyConstraint;
import com.archci.dependencies.Dependency;
import com.archci.exception.DCLException;
import com.archci.exception.ParseException;
import com.archci.parser.DCLParser;
import com.archci.util.DCLUtil;

public class Main_2 {
	public static void main(String args[]) throws DCLException, IOException, CoreException, ParseException{
		
		
		//File f = new File("/Users/arthurfp/Movies/DCL-master/dclsuite/src/dclsuite/util/DateUtil.java");
		File f = new File("/Users/arthurfp/Documents/workspace/ArchCI_exampleproject/src/com/archci/main/Main.java");
		//File dcl = new File("/Users/arthurfp/Movies/DCL-master/dclsuite/architecture.dcl");
		//File dcl = new File("/Users/arthurfp/Documents/architecture.dcl");
		File dcl = new File("/Users/arthurfp/Documents/workspace/ArchCI_exampleproject/architecture.dcl");
		
		Collection<Dependency> dependencies;
		dependencies = DCLUtil.getDependenciesUsingAST(f);
		
		InputStream inputStream = new FileInputStream(dcl);
		
		Map<String, String> moduleDesc;
		moduleDesc = DCLParser.parseModules(inputStream);
		
		inputStream = new FileInputStream(dcl);
		
		Collection<DependencyConstraint> dependencyConst = new LinkedList<DependencyConstraint>();
		dependencyConst = DCLParser.parseDependencyConstraints(inputStream);
		
		System.out.println("Modules:\n");
		System.out.println(moduleDesc.toString());
		
		System.out.println("\n===================\nDependency Constraints:\n");
		for (DependencyConstraint dc : dependencyConst) {
			System.out.println(dc);
			System.out.println("-------\n\n");
		}
		
		System.out.println("Dependency List:\n");
		
		for (Dependency dep : dependencies) {
			System.out.println(dep.toString());
		}
		
		
	}
}
*/