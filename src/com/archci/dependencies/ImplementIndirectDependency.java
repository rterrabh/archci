package com.archci.dependencies;

import com.archci.dependencies.ImplementDependency;

public final class ImplementIndirectDependency extends ImplementDependency {

	public ImplementIndirectDependency(String classNameA, String classNameB, Integer lineNumberA, Integer offset, Integer length) {
		super(classNameA, classNameB, lineNumberA, offset, length);
	}
	
	
	@Override
	public String toString() {
		return "'" + 
				this.classNameA + "' indirectly implements '" + this.classNameB + "'";
	}

	
}
