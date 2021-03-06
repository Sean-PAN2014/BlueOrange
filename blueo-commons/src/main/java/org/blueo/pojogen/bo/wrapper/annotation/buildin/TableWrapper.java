package org.blueo.pojogen.bo.wrapper.annotation.buildin;

import javax.persistence.Table;

import org.blueo.pojogen.bo.PojoClass;
import org.blueo.pojogen.bo.wrapper.annotation.ClassAnnotationWrapper;
import org.springframework.util.Assert;

public class TableWrapper extends ClassAnnotationWrapper {

	public TableWrapper() {
		super(Table.class);
	}

	@Override
	public String getDisplayString(PojoClass pojoClass) {
		Object tableName = pojoClass.getValueMap().get("tableName");
		Assert.notNull(tableName);
		return String.format("@%s(name = \"%s\")", classWrapper.getName(), tableName);
	}

}
