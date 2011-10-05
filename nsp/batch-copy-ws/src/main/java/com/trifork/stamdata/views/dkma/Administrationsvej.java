/**
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Contributor(s): Contributors are attributed in the source code
 * where applicable.
 *
 * The Original Code is "Stamdata".
 *
 * The Initial Developer of the Original Code is Trifork Public A/S.
 *
 * Portions created for the Original Code are Copyright 2011,
 * Lægemiddelstyrelsen. All Rights Reserved.
 *
 * Portions created for the FMKi Project are Copyright 2011,
 * National Board of e-Health (NSI). All Rights Reserved.
 */


package com.trifork.stamdata.views.dkma;

import static javax.persistence.TemporalType.TIMESTAMP;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.trifork.stamdata.views.View;
import com.trifork.stamdata.views.ViewPath;

@Entity
@XmlRootElement
@ViewPath("dkma/administrationsvej/v1")
public class Administrationsvej extends View
{
	@Id
	@GeneratedValue
	@Column(name = "AdministrationsvejPID")
	@XmlTransient
	private BigInteger recordID;

	@Column(name = "AdministrationsvejKode")
	private String id;

	@Column(name = "AdministrationsvejTekst")
	protected String tekst;

	@Column(name = "AdministrationsvejKortTekst")
	protected String kortTekst;

	@XmlTransient
	private Date modifiedDate;

	@Temporal(TIMESTAMP)
	protected Date validFrom;

	@Temporal(TIMESTAMP)
	protected Date validTo;

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public Date getUpdated()
	{
		return modifiedDate;
	}

	@Override
	public BigInteger getRecordID()
	{
		return recordID;
	}
}
