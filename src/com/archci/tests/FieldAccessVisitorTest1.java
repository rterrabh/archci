package com.archci.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;

import com.archci.ast.DCLDeepDependencyVisitor;
import com.archci.dependencies.Dependency;
import com.archci.exception.DCLException;

public class FieldAccessVisitorTest1 extends DCLDeepDependencyVisitor {
	
private static File file;

@BeforeClass
public static void SetUp(){
	file = new File(testUtil.sourcePath+"fieldAccessExamples/H1.java");
	}
	
	public FieldAccessVisitorTest1() throws DCLException {
		super(file, null, new String[]{testUtil.sourcePath});
	}

	@Test
	public void testMarkerAnnotationVisitor() {
		ArrayList<String> dependencies = new ArrayList<String>();
		
		for(Dependency dep : this.getDependencies())
			dependencies.add(dep.toString());
		
		
		assertThat(dependencies, hasItems("'H1' contains the method 'Method' that "
				+ "accesses the field 'x' of an object of 'fieldAccessExamples.H1'"));
	}

}
