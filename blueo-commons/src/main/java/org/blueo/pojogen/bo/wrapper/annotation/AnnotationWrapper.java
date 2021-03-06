package org.blueo.pojogen.bo.wrapper.annotation;

import java.lang.annotation.Annotation;
import java.util.List;

import org.blueo.pojogen.bo.wrapper.clazz.ClassWrapper;

import com.google.common.base.Function;

public class AnnotationWrapper<T> {
	protected final ClassWrapper classWrapper;
	// should be BiFunction in JDK 8
	protected final Function<T, String> function;
	
	// -----------------------------
	// ----- Constructors
	// -----------------------------
	public AnnotationWrapper(Class<? extends Annotation> annotationClass) {
		this(annotationClass, (Function<T, String>)null);
	}
	
	public AnnotationWrapper(Class<? extends Annotation> annotationClass, final String displayValue) {
		this(annotationClass, new Function<T, String>(){
			@Override
			public String apply(T input) {
				return displayValue;
			}
		});
	}

	public AnnotationWrapper(Class<? extends Annotation> annotationClass, Function<T, String> function) {
		this(ClassWrapper.of(annotationClass), function);
	}

	public AnnotationWrapper(ClassWrapper classWrapper) {
		this(classWrapper, null);
	}

	public AnnotationWrapper(ClassWrapper classWrapper, Function<T, String> function) {
		super();
		this.classWrapper = classWrapper;
		this.function = function;
	}

	// -----------------------------
	// ----- Others
	// -----------------------------
	public String getDisplayString(T t) {
		if (function == null) {
			return getAnnotationClassPart();
		} else {
			return String.format("@%s%s", this.classWrapper.getName(), function.apply(t));
		}
	}

	protected String getAnnotationClassPart() {
		return String.format("@%s", classWrapper.getName());
	}

	public ClassWrapper getClassWrapper() {
		return classWrapper;
	}

	public List<ClassWrapper> getImports() {
		return classWrapper.getImports();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AnnotationWrapper [annotationClass=");
		builder.append(classWrapper);
		builder.append("]");
		return builder.toString();
	}

}
