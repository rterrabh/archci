package com.archci.dependencies;

import com.archci.enums.DependencyType;
import com.archci.util.DCLUtil;

public class AnnotateDependency extends Dependency {
	
	public AnnotateDependency(String classNameA, String classNameB, Integer lineNumberA, Integer offset, Integer length) {
		super(classNameA,classNameB,lineNumberA,offset,length);
	}
	
	@Override
	public DependencyType getDependencyType() {
		return DependencyType.USEANNOTATION;
	}
	
	@Override
	public String toShortString() {
		return "The annotation @" + DCLUtil.getSimpleClassName(this.classNameB) + " is disallowed for this location w.r.t. the architecture";
	}

}