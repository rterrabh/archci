package com.archci.tests;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import com.archci.ast.DCLDeepDependencyVisitor;
import com.archci.dependencies.Dependency;
import com.archci.exception.DCLException;

public class TypeDeclarationVisitorTest6 extends DCLDeepDependencyVisitor {
	
private static File file;

@BeforeClass
public static void SetUp(){
	file = new File(testUtil.sourcePath+"typeDeclarationExamples/A8.java");
	}
	
	public TypeDeclarationVisitorTest6() throws DCLException {
		super(file, null, new String[]{testUtil.sourcePath});
	}

	@Test
	public void testTypeDeclarationVisitor() {
		ArrayList<String> dependencies = new ArrayList<String>();
		
		for(Dependency dep : this.getDependencies())
			dependencies.add(dep.toString());
		
		
		assertThat(dependencies, hasItems("'A8' directly implements 'java.awt.event.ComponentListener'",
				"'A8' indirectly implements 'java.util.EventListener'", 
				"'A8' indirectly extends 'java.lang.Object'"));
	}

}
