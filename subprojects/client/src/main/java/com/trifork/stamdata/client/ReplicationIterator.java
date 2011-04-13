// Stamdata - Copyright (C) 2011 National Board of e-Health (NSI)
// 
// All source code and information supplied as part of Stamdata is
// copyright to National Board of e-Health.
// 
// The source code has been released under a dual license - meaning you can
// use either licensed version of the library with your code.
// 
// It is released under the Common Public License 1.0, a copy of which can
// be found at the link below.
// http://www.opensource.org/licenses/cpl1.0.php
// 
// It is released under the LGPL (GNU Lesser General Public License), either
// version 2.1 of the License, or (at your option) any later version. A copy
// of which can be found at the link below.
// http://www.gnu.org/copyleft/lesser.html

package com.trifork.stamdata.client;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

class ReplicationIterator<T> implements Iterator<EntityRevision<T>> {
	private XMLEventReader reader;
	private XMLEventReader filteredReader;
	private Unmarshaller unmarshaller;
	private final Class<T> entityType;
	private final ReplicationReader replicationReader;

	ReplicationIterator(Class<T> entityType, ReplicationReader replicationReader) throws XMLStreamException, IOException, JAXBException {
		this.entityType = entityType;
		this.replicationReader = replicationReader;

		// XML TO POJO UNMARSHALLING

		JAXBContext ctx = JAXBContext.newInstance(entityType);
		System.out.println("Entity type: " + entityType);
		unmarshaller = ctx.createUnmarshaller();
		System.out.println("Unmarshaller type: " + unmarshaller.getClass());
	}

	protected boolean hasMoreInCurrentPage() throws XMLStreamException {
		return filteredReader != null && filteredReader.peek() != null;
	}

	@Override
	public boolean hasNext() {

		try {
			boolean moreInPage = hasMoreInCurrentPage();

			// IF THERE ARE NO MORE ENTRIES IN THE PAGE
			//
			// This is true if the page is completely traversed.
			// Both readers have to be released.

			if (!moreInPage) {

				if (filteredReader != null) {
					filteredReader.close();
					reader.close();
				}

				// CHECK IF WE NEED TO FETCH THE NEXT PAGE

				if (!replicationReader.isUpdateCompleted()) {
					fetchNextPage();
					moreInPage = hasMoreInCurrentPage();
				}
			}

			return moreInPage;
		}
		catch (Exception e) {
			throw new RecordStreamException(e);
		}
	}

	private void fetchNextPage() {
		replicationReader.fetchNextPage();

		try {
			// READ THE RESPONSE
			//
			// To optimize the parsing we are only interested in 'start element'
			// events that start with the prefix 'sd'.

			XMLInputFactory readerFactory = XMLInputFactory.newInstance();
			reader = readerFactory.createXMLEventReader(replicationReader.getInputStream(), "UTF-8");
			EventFilter filter = new EventFilter() {
				@Override
				public boolean accept(XMLEvent event) {
					return event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("entry");
				}
			};
			filteredReader = readerFactory.createFilteredReader(reader, filter);
		} catch (XMLStreamException e) {
			throw new IllegalStateException("Could not fetch next page", e);
		}
	}

	@Override
	public EntityRevision<T> next() {
		if (!hasNext()) throw new NoSuchElementException();

		try {
			filteredReader.next();
			reader.nextTag();
			String id = reader.getElementText();
			skipTag("title");
			skipTag("updated");
			skipToEntity();
			T entity = unmarshaller.unmarshal(reader, entityType).getValue();

			return new EntityRevision<T>(id, entity);
		}
		catch (JAXBException e) {
			throw new RecordStreamException(e);
		}
		catch (XMLStreamException e) {
			throw new RecordStreamException(e);
		}
	}

	private void skipTag(String tagName) throws XMLStreamException {
		while ((reader.peek().isStartElement() && reader.peek().asStartElement().getName().getLocalPart().equals(tagName))
			|| reader.peek().isEndElement()
			|| reader.peek().isCharacters()) {
			reader.nextEvent();
		}
	}
	
	private void skipToEntity() throws XMLStreamException {
		while (!reader.peek().isStartElement() || !reader.peek().asStartElement().getName().getPrefix().equals("sd")) {
			reader.nextEvent();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
