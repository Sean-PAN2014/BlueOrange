package org.blueo.commons.persistent.core.dao;

import java.util.List;

import org.blueo.commons.persistent.core.dao.po.HasId;
import org.blueo.commons.persistent.core.dao.po.traceable.DelFlagType;
import org.blueo.commons.persistent.core.dao.po.traceable.TraceablePo;
import org.blueo.commons.persistent.core.dao.po.traceable.TraceablePoOverwriter;

public class TraceableDao<T extends HasId<K> & TraceablePo<U>, K, U> extends AbstractDao<T, K> {
	private TraceablePoOverwriter<T, U> TraceablePoOverwriter;
	private Dao<T, K> dao;

	// -----------------------------
	// ----- CRUD
	// -----------------------------

	@Override
	public T getById(K id) {
		return this.getById(id, DelFlagType.Active);
	}

	@Override
	public K save(T t) {
		TraceablePoOverwriter.saveOverwrite(t);
		return dao.save(t);
	}

	@Override
	public void update(T t) {
		TraceablePoOverwriter.updateOverwrite(t);
		dao.update(t);
	}

	@Override
	public void delete(T t) {
		TraceablePoOverwriter.deleteOverwrite(t);
		this.update(t);
	}

	@Override
	public void deleteById(K id) {
		T t = this.getById(id);
		if (t != null) {
			this.delete(t);
		}
	}

	public T getById(K id, DelFlagType type) {
		T t = dao.getById(id);
		return TraceablePoOverwriter.getOverwrite(t, type);
	}

	// -----------------------------
	// ----- CRUD BATCH
	// -----------------------------

	@Override
	public void saveAll(List<T> list) {
		TraceablePoOverwriter.saveAllOverwrite(list);
		dao.saveAll(list);
	}

	@Override
	public void updateAll(List<T> list) {
		TraceablePoOverwriter.updateAllOverwrite(list);
		dao.updateAll(list);
	}

	@Override
	public void deleteAll(List<T> list) {
		TraceablePoOverwriter.deleteAllOverwrite(list);
		this.updateAll(list);
	}

	// -----------------------------
	// ----- Search
	// -----------------------------

	@Override
	public List<T> findByExample(T t) {
		TraceablePoOverwriter.findByExampleOverwrite(t);
		return dao.findByExample(t);
	}

	// -----------------------------
	// ----- DI
	// -----------------------------
	
	public TraceablePoOverwriter<T, U> getTraceablePoOverwriter() {
		return TraceablePoOverwriter;
	}

	public void setTraceablePoOverwriter(TraceablePoOverwriter<T, U> traceablePoOverwriter) {
		TraceablePoOverwriter = traceablePoOverwriter;
	}

	public Dao<T, K> getDao() {
		return dao;
	}

	public void setDao(Dao<T, K> dao) {
		this.dao = dao;
	}

}