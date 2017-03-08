package com.archci.core;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ITypeBinding;

import com.archci.dependencies.Dependency;
import com.archci.dependencies.ExtendIndirectDependency;
import com.archci.dependencies.ImplementIndirectDependency;
import com.archci.enums.Constraint;
import com.archci.enums.ConstraintType;
import com.archci.util.DCLUtil;

public class DependencyConstraint implements Comparable<DependencyConstraint> {
	private final String moduleDescriptionA;
	private final String moduleDescriptionB;
	private final Constraint constraint;
	private final boolean warning;

	public DependencyConstraint(String moduleDescriptionA, String moduleDescriptionB, Constraint constraint, boolean warning) {
		super();
		this.moduleDescriptionA = moduleDescriptionA;
		this.moduleDescriptionB = moduleDescriptionB;
		this.constraint = constraint;
		this.warning=warning;
	}

	public List<ArchitecturalDrift> validate(String className, final Map<String, String> modules, Set<String> projectClasses,
			Collection<Dependency> dependencies, List<ITypeBinding> typeBindings) throws CoreException {
		switch (this.constraint.getConstraintType()) {
		case ONLY_CAN:
			if (DCLUtil.hasClassNameByDescription(className, moduleDescriptionA, modules, projectClasses, typeBindings)) {
				return null;
			}
			return this.validateCannot(className, moduleDescriptionB, this.constraint.getDependencyType().getDependencyClass(), modules,
					projectClasses, dependencies, typeBindings);

		case CANNOT:
			if (!DCLUtil.hasClassNameByDescription(className, moduleDescriptionA, modules, projectClasses, typeBindings)) {
				return null;
			}
			return this.validateCannot(className, moduleDescriptionB, this.constraint.getDependencyType().getDependencyClass(), modules,
					projectClasses, dependencies, typeBindings);

		case CAN_ONLY:
			if (!DCLUtil.hasClassNameByDescription(className, moduleDescriptionA, modules, projectClasses, typeBindings)) {
				return null;
			}
			return this.validateCanOnly(className, moduleDescriptionB, this.constraint.getDependencyType().getDependencyClass(), modules,
					projectClasses, dependencies, typeBindings);

		case MUST:
			if (!DCLUtil.hasClassNameByDescription(className, moduleDescriptionA, modules, projectClasses, typeBindings)) {
				return null;
			}
			return this.validateMust(className, moduleDescriptionB, this.constraint.getDependencyType().getDependencyClass(), modules,
					projectClasses, dependencies, typeBindings);
		}

		return null;
	}

	/**
	 * cannot
	 */
	private List<ArchitecturalDrift> validateCannot(String className, String moduleDescriptionB,
			Class<? extends Dependency> dependencyClass, Map<String, String> modules, Set<String> projectClasses,
			Collection<Dependency> dependencies, List<ITypeBinding> typeBindings) {
		List<ArchitecturalDrift> architecturalDrifts = new LinkedList<ArchitecturalDrift>();
		/* For each dependency */
		for (Dependency d : dependencies) {
			if (dependencyClass.isAssignableFrom(d.getClass())) {
				if (d.getClassNameB().equals(d.getClassNameA())) {
					continue;
				}
				/* We disregard indirect dependencies to divergences */
				if (d instanceof ExtendIndirectDependency || d instanceof ImplementIndirectDependency){
					continue;
				}
				
				if (DCLUtil.hasClassNameByDescription(d.getClassNameB(), moduleDescriptionB, modules, projectClasses, typeBindings)) {
					architecturalDrifts.add(new DivergenceArchitecturalDrift(this, d, warning));
				}
			}
		}
		return architecturalDrifts;
	}

	/**
	 * can only
	 */
	private List<ArchitecturalDrift> validateCanOnly(String className, String moduleDescriptionB,
			Class<? extends Dependency> dependencyClass, Map<String, String> modules, Set<String> projectClasses,
			Collection<Dependency> dependencies, List<ITypeBinding> typeBindings) {
		List<ArchitecturalDrift> architecturalDrifts = new LinkedList<ArchitecturalDrift>();

		/* For each dependency */
		for (Dependency d : dependencies) {
			if (dependencyClass.isAssignableFrom(d.getClass())) {
				if (d.getClassNameB().equals(d.getClassNameA())) {
					continue;
				}
				/* We disregard indirect dependencies to divergences */
				if (d instanceof ExtendIndirectDependency || d instanceof ImplementIndirectDependency){
					continue;
				}
				if (!DCLUtil.hasClassNameByDescription(d.getClassNameB(), moduleDescriptionB, modules, projectClasses, typeBindings)) {
					architecturalDrifts.add(new DivergenceArchitecturalDrift(this, d, warning));
				}

			}
		}
		return architecturalDrifts;
	}

	/**
	 * must
	 */
	private List<ArchitecturalDrift> validateMust(String className, String moduleDescriptionB, Class<? extends Dependency> dependencyClass,
			Map<String, String> modules, Set<String> projectClasses, Collection<Dependency> dependencies, List<ITypeBinding> typeBindings) {
		List<ArchitecturalDrift> architecturalDrifts = new LinkedList<ArchitecturalDrift>();

		// TODO: What am I supposed to do in case of internal class?
		if (className.contains("$")) {
			return null;
		} else if (className.equals(moduleDescriptionB)) {
			return null;
		} else if (DCLUtil.hasClassNameByDescription(className, moduleDescriptionB, modules, projectClasses, typeBindings)) {
			return null;
		}

		boolean found = false;
		for (Dependency d : dependencies) {
			if (dependencyClass.isAssignableFrom(d.getClass())) {
				if (DCLUtil.hasClassNameByDescription(d.getClassNameB(), moduleDescriptionB, modules, projectClasses, typeBindings)) {
					found = true;
					break;
				}
			}
		}
		if (!found) {
			architecturalDrifts.add(new AbsenceArchitecturalDrift(this, className, moduleDescriptionB, warning));
		}

		return architecturalDrifts;
	}

	@Override
	public String toString() {
		return (this.constraint.getConstraintType().equals(ConstraintType.ONLY_CAN) ? "only " : "") + this.moduleDescriptionA + " "
				+ this.constraint.getValue() + " " + this.moduleDescriptionB;
	}

	public int compareTo(DependencyConstraint o) {
		return this.toString().compareTo(o.toString());
	}

	public Constraint getConstraint() {
		return this.constraint;
	}

	public String getModuleDescriptionA() {
		return this.moduleDescriptionA;
	}

	public String getModuleDescriptionB() {
		return this.moduleDescriptionB;
	}

	/**
	 * DCL2 Class that stores the crucial informations about the architectural
	 * drift
	 */
	public static abstract class ArchitecturalDrift {
		public static final String DIVERGENCE = "DIVERGENCE";
		public static final String ABSENCE = "ABSENCE";

		protected final DependencyConstraint violatedConstraint;
		protected final boolean warning;

		protected ArchitecturalDrift(DependencyConstraint violatedConstraint, boolean warning) {
			super();
			this.violatedConstraint = violatedConstraint;
			this.warning=warning;
		}

		public final DependencyConstraint getViolatedConstraint() {
			return this.violatedConstraint;
		}

		public abstract boolean getIsWarning();
		
		public abstract String getDetailedMessage();
		
		public abstract String getInfoMessage();

		public abstract String getViolationType();

	}

	public static class DivergenceArchitecturalDrift extends ArchitecturalDrift {
		private final Dependency forbiddenDependency;
		private final boolean warning;

		public DivergenceArchitecturalDrift(DependencyConstraint violatedConstraint, Dependency forbiddenDependency, boolean warning) {
			super(violatedConstraint, warning);
			this.forbiddenDependency = forbiddenDependency;
			this.warning=warning;
		}

		public final Dependency getForbiddenDependency() {
			return this.forbiddenDependency;
		}
		
		@Override
		public boolean getIsWarning(){
			return this.warning;
		}

		@Override
		public String getDetailedMessage() {
			return this.forbiddenDependency.toString();
		}

		@Override
		public String getInfoMessage() {
			return this.forbiddenDependency.toShortString();
		}

		@Override
		public String getViolationType() {
			return DIVERGENCE;
		}
	}

	public static class AbsenceArchitecturalDrift extends ArchitecturalDrift {
		private final String classNameA;
		private final String moduleDescriptionB;
		private final boolean warning;

		public AbsenceArchitecturalDrift(DependencyConstraint violatedConstraint, String classNameA, String moduleDescriptionB, boolean warning) {
			super(violatedConstraint, warning);
			this.classNameA = classNameA;
			this.moduleDescriptionB = moduleDescriptionB;
			this.warning=warning;
		}
		
		@Override
		public boolean getIsWarning(){
			return this.warning;
		}

		public final String getClassNameA() {
			return this.classNameA;
		}

		public String getModuleNameB() {
			return this.moduleDescriptionB;
		}

		@Override
		public String getDetailedMessage() {
			return this.classNameA + " does not " + this.violatedConstraint.getConstraint().getDependencyType().getValue()
					+ " any type in " + this.violatedConstraint.getModuleDescriptionB();
		}

		@Override
		public String getInfoMessage() {
			switch (this.violatedConstraint.getConstraint().getDependencyType()) {

			case ACCESS:
				return "The access of fiels or methods of " + this.violatedConstraint.getModuleDescriptionB()
						+ " is required for this location w.r.t. the architecture";
			case DECLARE:
				return "The declaration of " + this.violatedConstraint.getModuleDescriptionB()
						+ " is required for this location w.r.t. the architecture";
			case HANDLE:
				return "The access or declaration (handling) of " + this.violatedConstraint.getModuleDescriptionB()
						+ " is required for this location w.r.t. the architecture";
			case CREATE:
				return "The creation of " + this.violatedConstraint.getModuleDescriptionB()
						+ " is required for this location w.r.t. the architecture";
			case THROW:
				return "The throwing of " + this.violatedConstraint.getModuleDescriptionB()
						+ " is required for this location w.r.t. the architecture";
			case DERIVE:
			case EXTEND:
			case IMPLEMENT:
				return "The inheritance of " + this.violatedConstraint.getModuleDescriptionB()
						+ " is required for this location w.r.t. the architecture";
			case USEANNOTATION:
				return "The annotation @" + this.violatedConstraint.getModuleDescriptionB()
						+ " is required for this location w.r.t. the architecture";
			default:
				return "The dependency with " + this.violatedConstraint.getModuleDescriptionB()
						+ " is required for this location w.r.t. the architecture";
			}
		}

		@Override
		public String getViolationType() {
			return ABSENCE;
		}
	}

}
