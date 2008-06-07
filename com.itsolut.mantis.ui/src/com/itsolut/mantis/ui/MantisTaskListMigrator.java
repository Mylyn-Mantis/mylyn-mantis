package com.itsolut.mantis.ui;

import java.util.Set;

import org.eclipse.mylyn.tasks.core.AbstractTaskListMigrator;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.w3c.dom.Element;

public class MantisTaskListMigrator extends AbstractTaskListMigrator {

	public MantisTaskListMigrator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getConnectorKind() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getQueryElementNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTaskElementName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void migrateQuery(IRepositoryQuery query, Element element) {
		// TODO Auto-generated method stub

	}

	@Override
	public void migrateTask(ITask task, Element element) {
		// TODO Auto-generated method stub

	}

}
