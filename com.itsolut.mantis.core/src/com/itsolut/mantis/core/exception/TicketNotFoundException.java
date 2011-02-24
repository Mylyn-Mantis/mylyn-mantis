package com.itsolut.mantis.core.exception;

/**
 * @author Robert Munteanu
 *
 */
public class TicketNotFoundException extends MantisException {

	private static final long serialVersionUID = 1L;

	public TicketNotFoundException(int ticketId) {
		
		super("No ticket found with id " + ticketId + " .");
	}
	
}
