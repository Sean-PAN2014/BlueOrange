package org.blueo.commons.jdbc;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.blueo.commons.BlueoUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jdbc.core.ArgumentTypePreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.Lists;

public class EntityWrapper<T> {
	private static final int BATCH_SIZE = 1000;
	private static final String SEPARATOR = ", ";
	//
	private final Class<T> clazz;
	//
	private String insertSql;
	private String updateSql;
	private ParameterizedPreparedStatementSetter<T> insertPss;
	private ParameterizedPreparedStatementSetter<T> updatePss;
	
	public static <T> EntityWrapper<T> of(Class<T> clazz) {
		return new EntityWrapper<>(clazz);
	}
	
	public EntityWrapper(Class<T> clazz) {
		this.clazz = clazz;
		this.init();
	}

	private void init() {
		// Is a entity
		Assert.notNull(AnnotationUtils.findAnnotation(clazz, Entity.class));
		// Is a table
		Table table = AnnotationUtils.findAnnotation(clazz, Table.class);
		Assert.notNull(table);
		// prepare variables for building
		String tableName = table.name();
		PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(clazz);
		final List<PropertyDescriptor> noneGenValueCols = Lists.newArrayList();
		final List<PropertyDescriptor> noneIdCols = Lists.newArrayList();
		PropertyDescriptor idCol = null;
		for (PropertyDescriptor pd : pds) {
			// method has Column, but not GeneratedValue.
			if (EntityField.isColumn(pd)) {
				if (EntityField.isId(pd)) {
					idCol = pd;
				} else {
					noneIdCols.add(pd);
				}
				if (!EntityField.isGeneratedValue(pd)) {
					noneGenValueCols.add(pd);
				} 
			}
		}
		Assert.notEmpty(noneGenValueCols);
		// real build
		// insert
		insertPss = this.buildInsertPss(noneGenValueCols);
		insertSql = this.buildInsertSql(tableName, noneGenValueCols);
		// update
		updatePss = this.buildUpdatePss(noneIdCols, idCol);
		updateSql = this.buildUpdateSql(tableName, noneIdCols, idCol);
	}

	private String buildInsertSql(String tableName, List<PropertyDescriptor> columnPds) {
		List<String> columnNames = EntityField.getColumnNames(columnPds);
		String columnPart = StringUtils.join(columnNames, SEPARATOR);
		String valuePart = StringUtils.repeat("?", SEPARATOR, columnNames.size());
		return String.format("INSERT INTO %s(%s) VALUES(%s)", tableName, columnPart, valuePart);
	}

	private String buildUpdateSql(String tableName, final List<PropertyDescriptor> noneIds, final PropertyDescriptor id) {
		List<String> setPiece = Lists.newArrayList();
		for (String columnName : EntityField.getColumnNames(noneIds)) {
			setPiece.add(String.format("%s=?", columnName));
		}
		String setPart = StringUtils.join(setPiece, SEPARATOR);
		return String.format("UPDATE %s SET %s WHERE %s=?", tableName, setPart, EntityField.getColumnName(id));
	}

	private Object getValue(Object value, Method method) {
		Enumerated enumerated = AnnotationUtils.findAnnotation(method, Enumerated.class);
		if (!(value instanceof Enum<?>)) {
			return value;
		}
		Enum<?> enumValue = (Enum<?>) value;
		if (enumerated == null || enumerated.value() == null || enumerated.value() == EnumType.STRING) {
			return enumValue.toString();
		} else if (enumerated.value() == EnumType.ORDINAL) {
			return enumValue.ordinal();
		} else {
			throw BlueoUtils.illegalArgument("never go here.");
		}
	}

	private ParameterizedPreparedStatementSetter<T> buildInsertPss(final List<PropertyDescriptor> columnPds) {
		return new ParameterizedPreparedStatementSetter<T>() {

			@Override
			public void setValues(PreparedStatement ps, T t) throws SQLException {
				Object[] args = new Object[columnPds.size()];
				int[] argTypes = new int[columnPds.size()];

				for (int i = 0; i < columnPds.size(); i++) {
					// ArgumentTypePreparedStatementSetter
					PropertyDescriptor pd = columnPds.get(i);
					// 
					argTypes[i] = StatementCreatorUtils.javaTypeToSqlParameterType(pd.getPropertyType());
					// 
					Method readMethod = pd.getReadMethod();
					Object value = ReflectionUtils.invokeMethod(readMethod, t);
					args[i] = getValue(value, readMethod);
				}
				//
				ArgumentTypePreparedStatementSetter stmtSetter = new ArgumentTypePreparedStatementSetter(args, argTypes);
				stmtSetter.setValues(ps);
			}

			@Override
			public String toString() {
				return String.valueOf(columnPds);
			}
			
		};
	}

	private ParameterizedPreparedStatementSetter<T> buildUpdatePss(final List<PropertyDescriptor> noneIds, final PropertyDescriptor id) {
		return new ParameterizedPreparedStatementSetter<T>() {

			@Override
			public void setValues(PreparedStatement ps, T t) throws SQLException {
				List<PropertyDescriptor> allColumns = Lists.newArrayList();
				allColumns.addAll(noneIds);
				allColumns.add(id);
				//
				Object[] args = new Object[allColumns.size() + 1];
				int[] argTypes = new int[allColumns.size() + 1];
				for (int i = 0; i < allColumns.size(); i++) {
					// ArgumentTypePreparedStatementSetter
					PropertyDescriptor pd = allColumns.get(i);
					// 
					argTypes[i] = StatementCreatorUtils.javaTypeToSqlParameterType(pd.getPropertyType());
					// 
					Method readMethod = pd.getReadMethod();
					Object value = ReflectionUtils.invokeMethod(readMethod, t);
					args[i] = getValue(value, readMethod);
				}
				//
				ArgumentTypePreparedStatementSetter stmtSetter = new ArgumentTypePreparedStatementSetter(args, argTypes);
				stmtSetter.setValues(ps);
			}

			@Override
			public String toString() {
				return String.valueOf(noneIds);
			}
			
		};
	}

	public void saveAll(JdbcTemplate jdbcTemplate, List<T> entities) {
		jdbcTemplate.batchUpdate(insertSql, entities, BATCH_SIZE, insertPss);
	}

	public void updateAll(JdbcTemplate jdbcTemplate, List<T> entities) {
		jdbcTemplate.batchUpdate(updateSql, entities, BATCH_SIZE, updatePss);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EntityForJdbc [clazz=");
		builder.append(clazz);
		builder.append(", insertSql=");
		builder.append(insertSql);
		builder.append(", pss=");
		builder.append(insertPss);
		builder.append("]");
		return builder.toString();
	}

}
