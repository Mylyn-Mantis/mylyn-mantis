/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - Initial implementation for Mantis
 *******************************************************************************/
package com.itsolut.mantis.core;

import java.math.BigInteger;
import java.net.Proxy;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.rpc.ServiceException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.itsolut.mantis.binding.AccountData;
import com.itsolut.mantis.binding.AttachmentData;
import com.itsolut.mantis.binding.FilterData;
import com.itsolut.mantis.binding.IssueData;
import com.itsolut.mantis.binding.IssueHeaderData;
import com.itsolut.mantis.binding.IssueNoteData;
import com.itsolut.mantis.binding.MantisConnectLocator;
import com.itsolut.mantis.binding.MantisConnectPortType;
import com.itsolut.mantis.binding.ObjectRef;
import com.itsolut.mantis.binding.ProjectData;
import com.itsolut.mantis.core.exception.InvalidTicketException;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.exception.MantisRemoteException;
import com.itsolut.mantis.core.model.MantisAttachment;
import com.itsolut.mantis.core.model.MantisComment;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisProjection;
import com.itsolut.mantis.core.model.MantisReproducibility;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSearchFilter;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicketAttribute;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisViewState;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * Represents a Mantis repository that is accessed through the MantisConnect SOAP Interface.
 * 
 * @author Chris Hane
 */
public class MantisAxis1SOAPClient extends AbstractMantisClient {
	private transient MantisConnectPortType soap;

	public static final String REQUIRED_REVISION = "1.0a5";
	
	//how to use proxy?
	public MantisAxis1SOAPClient(URL url, Version version, String username, String password, Proxy proxy) {
		super(url, version, username, password, proxy);
	}

	protected MantisConnectPortType getSOAP() throws MantisException {
		if(soap!=null){
			return soap;
		}

		try {
			soap = new MantisConnectLocator().getMantisConnectPort(repositoryUrl);
		} catch (ServiceException e) {
			throw new MantisRemoteException(e);
		}
		
		return soap;
	}
	
	public void validate() throws MantisException {
		
		String version;
		try {
			version = getSOAP().mc_version();
		} catch (RemoteException e) {
			throw new MantisRemoteException(e);
		}
		
		if("0.0.5".equals(version)){
			return;
		}

		throw new MantisException("Required API calls are missing, please update your MantisConnect to revision "
				+ REQUIRED_REVISION + " or later");
	}

	public MantisTicket getTicket(int id) throws MantisException {
		IssueData issue;
		try {
			issue = getSOAP().mc_issue_get(username, password, BigInteger.valueOf(id));
		} catch (RemoteException e) {
			throw new MantisRemoteException(e);
		}
		MantisTicket ticket = parseTicket(issue);
//
//		String[] actions = getActions(id);
//		ticket.setActions(actions);
//
//		ticket.setResolutions(getDefaultTicketResolutions());

		return ticket;
	}
	
	//local cache
	private MantisProject[] projects = null;
	
	@Override
	public MantisProject[] getProjects() throws MantisException {
		if(projects==null){
			ProjectData[] pds;
			try {
				pds = getSOAP().mc_projects_get_user_accessible(username, password);
			} catch (RemoteException e) {
				throw new MantisRemoteException(e);
			}
			
			projects = new MantisProject[countProjects(pds)];
			addProjects(0, pds, 0);
		}
		
		return projects;	
	}
	
	private int addProjects(int offset, ProjectData[] pds, int level) {
		StringBuilder buf = new StringBuilder(level);
//		if(level>0){
//			for(int x=level; x>0; x--){
//				buf.append(" ");
//			}
//			buf.append(" -> ");
//		}
		
		for(int x=0; x<pds.length; x++){
			projects[offset++] = new MantisProject(buf.toString()+pds[x].getName(), pds[x].getId().intValue());
			offset = addProjects(offset, pds[x].getSubprojects(), level+1); //add sub projects if there are any...
		}
		return offset;
	}

	private int countProjects(ProjectData[] pds) {
		int cnt = 0;
		
		for(int x=0; x<pds.length; x++){
			cnt++;
			cnt += countProjects(pds[x].getSubprojects());
		}
		
		return cnt;
	}

	private ObjectRef getProject(String name) throws MantisException{
		for(MantisProject mp : this.getProjects()){
			if(mp.getName().equals(name)){
				return new ObjectRef(BigInteger.valueOf(mp.getValue()), mp.getName());
			}
		}
		return null;
	}
	
	private Map<String, MantisProjectCategory[]> categories = new HashMap<String, MantisProjectCategory[]>(3);
	
	public MantisProjectCategory[] getProjectCategories(String projectName) throws MantisException{
		
		if(categories.containsKey(projectName)){
			return categories.get(projectName);
		}
		
		ObjectRef project = getProject(projectName);
		String[] list;
		try {
			list = getSOAP().mc_project_get_categories(username, password, project.getId());
		} catch (Exception e) {
			return new MantisProjectCategory[0];
		}
		MantisProjectCategory[] data = new MantisProjectCategory[list.length];
		for(int x=0; x<list.length; x++){
			data[x] = new MantisProjectCategory(list[x], x);
		}
		this.categories.put(projectName, data);
		return data;
	}
	
	private Map<String, MantisProjectFilter[]> filters = new HashMap<String, MantisProjectFilter[]>(3);
	
	public MantisProjectFilter[] getProjectFilters(String projectName) throws MantisException{
		if(filters.containsKey(projectName)){
			return filters.get(projectName);
		}
		
		ObjectRef project = getProject(projectName);
		FilterData[] list;
		try {
			list = getSOAP().mc_filter_get(username, password, project.getId());
		} catch (RemoteException e) {
			return new MantisProjectFilter[0];
		}
		MantisProjectFilter[] data = new MantisProjectFilter[list.length];
		for(int x=0; x<list.length; x++){
			data[x] = new MantisProjectFilter(list[x].getName(), list[x].getId().intValue());
		}
		this.filters.put(projectName, data);
		return data;
	}
	
	private FilterData getFilter(String projectName, String filterName) throws MantisException {
		for(MantisProjectFilter filter : getProjectFilters(projectName)){
			if(filter.getName().equals(filterName)){
				FilterData fd = new FilterData();
				fd.setId(BigInteger.valueOf(filter.getValue()));
				return fd;
			}
		}
		return null;
	}
	
//	private MantisAttachment parseAttachment(Object[] entry) {
//		MantisAttachment attachment = new MantisAttachment((String) entry[0]);
//		attachment.setDescription((String) entry[1]);
//		attachment.setSize((Integer) entry[2]);
//		attachment.setCreated(MantisUtils.parseDate((Integer) entry[3]));
//		attachment.setAuthor((String) entry[4]);
//		return attachment;
//	}

	@SuppressWarnings("unchecked")
	public void search(MantisSearch query, List<MantisTicket> tickets) throws MantisException {
		try {
			
			String projectName=null;
			String filterName=null;
			for(MantisSearchFilter filter : query.getFilters()){
				if("project".equals(filter.getFieldName())){
					projectName = filter.getValues().get(0);
					
				} else if("filter".equals(filter.getFieldName())){
					filterName = filter.getValues().get(0);
				}
			}
			
			IssueHeaderData[] ihds = getSOAP().mc_filter_get_issue_headers(username, password, 
					                                                       getProject(projectName).getId(),   //project
					                                                       getFilter(projectName, filterName).getId(),  //filter
					                                                       BigInteger.valueOf(1),   //start page
					                                                       BigInteger.valueOf(1000)); //# per page
			
			for(IssueHeaderData ihd : ihds){
				MantisTicket ticket = new MantisTicket(ihd.getId().intValue());
				ticket.putBuiltinValue(Key.SUMMARY, ihd.getSummary()+" project["+ihd.getProject()+"]");
				ticket.putBuiltinValue(Key.ID, ihd.getId().toString());
				ticket.putBuiltinValue(Key.PRIORITY, ihd.getPriority().toString());
				
				tickets.add(ticket);
			}
			
		} catch (RemoteException e) {
			throw new MantisRemoteException(e);
		}
	}

	private MantisTicket parseTicket(IssueData issue) throws InvalidTicketException {
		MantisTicket ticket = new MantisTicket(issue.getId().intValue());
		ticket.setCreated(issue.getDate_submitted().getTime());
		ticket.setLastChanged(issue.getLast_updated().getTime());

		ticket.putBuiltinValue(Key.PROJECT, issue.getProject().getName());

		ticket.putBuiltinValue(Key.SUMMARY, issue.getSummary());
		ticket.putBuiltinValue(Key.DESCRIPTION, issue.getDescription());
		ticket.putBuiltinValue(Key.CATEOGRY, issue.getCategory());
		
		ticket.putBuiltinValue(Key.SEVERITY, issue.getSeverity().getName());
		ticket.putBuiltinValue(Key.PRIORITY, issue.getPriority().getName());
		ticket.putBuiltinValue(Key.REPRODUCIBILITY, issue.getReproducibility().getName());
		ticket.putBuiltinValue(Key.PROJECTION, issue.getProjection().getName());
		ticket.putBuiltinValue(Key.ETA, issue.getEta().getName());
		ticket.putBuiltinValue(Key.VIEW_STATE, issue.getView_state().getName());
		ticket.putBuiltinValue(Key.STATUS, issue.getStatus().getName());
		
		ticket.putBuiltinValue(Key.ADDITIONAL_INFO, issue.getAdditional_information());
		ticket.putBuiltinValue(Key.STEPS_TO_REPRODUCE, issue.getSteps_to_reproduce());

		ticket.putBuiltinValue(Key.REPORTER, issue.getReporter().getName());
		if(issue.getHandler()!=null){
			ticket.putBuiltinValue(Key.ASSIGNED_TO, issue.getHandler().getName());
		}
		
		for(IssueNoteData ind : issue.getNotes()){
			parseNote(ticket, ind);
		}
		
		for(AttachmentData ad : issue.getAttachments()){
			parseAttachment(ticket, ad);
		}
		
		
		
		return ticket;
	}

	private void parseAttachment(MantisTicket ticket, AttachmentData ad) {
		MantisAttachment ma = new MantisAttachment();
		ma.setContentType(ad.getContent_type());
		ma.setCreated(MantisUtils.transform(ad.getDate_submitted()));
		ma.setDownloadURL(ad.getDownload_url().getPath());
		ma.setFilename(ad.getFilename());
		ma.setSize(ad.getSize().intValue());
		ma.setId(ad.getId().intValue());
		ticket.addAttachment(ma);
	}

	private void parseNote(MantisTicket ticket, IssueNoteData ind) {
		MantisComment comment = new MantisComment();
		comment.setId(ind.getId().intValue());
		comment.setReporter(ind.getReporter().getName());
		comment.setText(ind.getText());
		comment.setDateSubmitted(MantisUtils.transform(ind.getDate_submitted()));
		comment.setLastModified(MantisUtils.transform(ind.getLast_modified()));
		ticket.addComment(comment);
	}

	@Override
	public synchronized void updateAttributes(IProgressMonitor monitor) throws MantisException {
		monitor.beginTask("Updating attributes", 8);
		
		try {
			ObjectRef[] result = getSOAP().mc_enum_priorities(username, password);
			data.priorities = new ArrayList<MantisPriority>(result.length);
			for (ObjectRef item : result) {
				data.priorities.add(parsePriority(item));
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			result = getSOAP().mc_enum_status(username, password);
			data.statuses = new ArrayList<MantisTicketStatus>(result.length);
			for (ObjectRef item : result) {
				data.statuses.add(parseTicketStatus(item));
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			result = getSOAP().mc_enum_severities(username, password);
			data.severities = new ArrayList<MantisSeverity>(result.length);
			for (ObjectRef item : result) {
				data.severities.add(parseSeverity(item));
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			result = getSOAP().mc_enum_resolutions(username, password);
			data.resolutions = new ArrayList<MantisResolution>(result.length);
			for (ObjectRef item : result) {
				data.resolutions.add(parseResolution(item));
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			result = getSOAP().mc_enum_reproducibilities(username, password);
			data.reproducibilities = new ArrayList<MantisReproducibility>(result.length);
			for (ObjectRef item : result) {
				data.reproducibilities.add(parseReproducibility(item));
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			result = getSOAP().mc_enum_projections(username, password);
			data.projections = new ArrayList<MantisProjection>(result.length);
			for (ObjectRef item : result) {
				data.projections.add(parseProjection(item));
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			result = getSOAP().mc_enum_etas(username, password);
			data.etas = new ArrayList<MantisETA>(result.length);
			for (ObjectRef item : result) {
				data.etas.add(parseETA(item));
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			result = getSOAP().mc_enum_view_states(username, password);
			data.viewStates = new ArrayList<MantisViewState>(result.length);
			for (ObjectRef item : result) {
				data.viewStates.add(parseViewState(item));
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
		
		
		
		} catch (RemoteException e) {
			throw new MantisRemoteException(e);
		}
	}

	private MantisViewState parseViewState(ObjectRef or) {
		MantisViewState item = new MantisViewState(or.getName(), or.getId().intValue());
		return item;
	}

	private MantisETA parseETA(ObjectRef or) {
		MantisETA item = new MantisETA(or.getName(), or.getId().intValue());
		return item;
	}

	private MantisProjection parseProjection(ObjectRef or) {
		MantisProjection item = new MantisProjection(or.getName(), or.getId().intValue());
		return item;
	}

	private MantisReproducibility parseReproducibility(ObjectRef or) {
		MantisReproducibility item = new MantisReproducibility(or.getName(), or.getId().intValue());
		return item;
	}

	private MantisResolution parseResolution(ObjectRef or) {
		MantisResolution item = new MantisResolution(or.getName(), or.getId().intValue());
		return item;
	}

	private MantisSeverity parseSeverity(ObjectRef or) {
		MantisSeverity item = new MantisSeverity(or.getName(), or.getId().intValue());
		return item;
	}

	private MantisTicketStatus parseTicketStatus(ObjectRef or) {
		MantisTicketStatus item = new MantisTicketStatus(or.getName(), or.getId().intValue());
		return item;
	}

	private MantisPriority parsePriority(ObjectRef or) {
		MantisPriority item = new MantisPriority(or.getName(), or.getId().intValue());
		return item;
	}

//	private MantisVersion parseVersion(Map<?, ?> result) {
//		MantisVersion version = new MantisVersion((String) result.get("name"));
//		version.setTime(MantisUtils.parseDate((Integer) result.get("time")));
//		version.setDescription((String) result.get("description"));
//		return version;
//	}

	public byte[] getAttachmentData(int attachmentID) throws MantisException {
		try {
			return getSOAP().mc_issue_attachment_get(username, password, BigInteger.valueOf(attachmentID));
		} catch (RemoteException e) {
			throw new MantisRemoteException(e);
		}
	}

	public void putAttachmentData(int ticketID, String filename, byte[] data) throws MantisException {
		try {
			getSOAP().mc_issue_attachment_add(username, password, BigInteger.valueOf(ticketID), filename, "bug", data);
		} catch (RemoteException e) {
			throw new MantisRemoteException(e);
		}
	}

	public int createTicket(MantisTicket ticket) throws MantisException {
		
		IssueData issue = createSOAPIssue(ticket);
		
		BigInteger id;
		try {
			id = getSOAP().mc_issue_add(username, password, issue);
		} catch (RemoteException e) {
			throw new MantisRemoteException(e);
		}
		
		ticket.setId(id.intValue());
		
		return ticket.getId();
	}

	private IssueData createSOAPIssue(MantisTicket ticket) throws MantisException {
		IssueData issue = new IssueData();
		issue.setSummary(ticket.getValue(Key.SUMMARY));
		issue.setDescription(ticket.getValue(Key.DESCRIPTION));
//		issue.setDate_submitted(ticket.getValue(Key.DATE_SUBMITTED));
		issue.setSeverity(newRef(data.severities, Key.SEVERITY, ticket));
//		issue.setResolution(newRef(data.resolutions, Key.RESOLUTION, ticket));
		issue.setPriority(newRef(data.priorities, Key.PRIORITY, ticket));
		issue.setReproducibility(newRef(data.reproducibilities, Key.REPRODUCIBILITY, ticket));
		issue.setProjection(newRef(data.projections, Key.PROJECTION, ticket));
		issue.setEta(newRef(data.etas, Key.ETA, ticket));
		issue.setView_state(newRef(data.viewStates, Key.VIEW_STATE, ticket));

		issue.setProject(getProject(ticket.getValue(Key.PROJECT)));
		issue.setCategory(ticket.getValue(Key.CATEOGRY));
		
		issue.setSteps_to_reproduce(ticket.getValue(Key.STEPS_TO_REPRODUCE));
		issue.setAdditional_information(ticket.getValue(Key.ADDITIONAL_INFO));
		
		issue.setStatus(newRef(data.statuses, Key.STATUS, ticket));
		
		if(MantisUtils.isEmpty(ticket.getValue(Key.REPORTER))){
			issue.setReporter(createReport(username));
		} else {
			issue.setReporter(createReport(ticket.getValue(Key.REPORTER)));
		}
		issue.setHandler(createReport(ticket.getValue(Key.ASSIGNED_TO)));
		issue.setLast_updated(MantisUtils.transform(new Date()));
		return issue;
	}

	private AccountData createReport(String name) {
		AccountData data = new AccountData();
		data.setName(name);
		return data;
	}

	private ObjectRef newRef(List<? extends MantisTicketAttribute> atttributes, Key key, MantisTicket ticket) throws MantisException {
		ObjectRef ref = new ObjectRef();
		
		ref.setName(ticket.getValue(key));
		
		for(MantisTicketAttribute attribute : atttributes){
			if(attribute.getName().equals(ref.getName())){
				ref.setId(BigInteger.valueOf(attribute.getValue()));
				return ref;
			}
		}
		
//		throw new MantisException("Could not find id for value["+ref.getName()+"] in key["+key.getKey()+"]");
		return null;
		
	}

	public void updateTicket(MantisTicket ticket, String comment) throws MantisException {
		
		IssueData issue = createSOAPIssue(ticket);
		issue.setId(BigInteger.valueOf(ticket.getId()));
		
		//add comment...
		IssueNoteData ind = new IssueNoteData();
		ind.setDate_submitted(MantisUtils.transform(new Date()));
		ind.setLast_modified(MantisUtils.transform(new Date()));
		ind.setReporter(createReport(username));
		ind.setText(comment);
		try {
			getSOAP().mc_issue_update(username, password, issue.getId(), issue);
			if(!MantisUtils.isEmpty(comment)){
				BigInteger id = getSOAP().mc_issue_note_add(username, password, issue.getId(), ind);
				ind.setId(id);
				parseNote(ticket, ind);
			}
			
			if(false){
				throw new RemoteException("TEST");
			}
		} catch (RemoteException e) {
			throw new MantisRemoteException(e);
		}
	}

	public Set<Integer> getChangedTickets(Date since) throws MantisException {
//		Object[] ids;
//		ids = (Object[]) call("ticket.getRecentChanges", since);
//		Set<Integer> result = new HashSet<Integer>();
//		for (Object id : ids) {
//			result.add((Integer) id);
//		}
//		return result;
		return new HashSet();
	}

	public String[] getActions(int id) throws MantisException {
//		Object[] actions = (Object[]) call("ticket.getAvailableActions", id);
//		String[] result = new String[actions.length];
//		for (int i = 0; i < result.length; i++) {
//			result[i] = (String) actions[i];
//		}
//		return result;
		return null;
	}
	//////////////////
}