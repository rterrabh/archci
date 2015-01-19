package com.archci.dependencies;

import com.archci.dependencies.Dependency;

public abstract class HandleDependency extends Dependency {

	protected HandleDependency(String classNameA, String classNameB, Integer lineNumberA, Integer offset, Integer length) {
		super(classNameA, classNameB, lineNumberA, offset, length);
	}
	
}