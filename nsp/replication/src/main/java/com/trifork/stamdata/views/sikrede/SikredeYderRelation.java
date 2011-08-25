// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy of
// the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS
// IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
//
// Contributor(s): Contributors are attributed in the source code
// where applicable.
//
// The Original Code is "Stamdata".
//
// The Initial Developer of the Original Code is Trifork Public A/S.
//
// Portions created for the Original Code are Copyright 2011,
// Lægemiddelstyrelsen. All Rights Reserved.
//
// Portions created for the FMKi Project are Copyright 2011,
// National Board of e-Health (NSI). All Rights Reserved.

package com.trifork.stamdata.views.sikrede;

import com.trifork.stamdata.views.View;
import com.trifork.stamdata.views.ViewPath;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.math.BigInteger;
import java.util.Date;

import static javax.persistence.TemporalType.DATE;
import static javax.persistence.TemporalType.TIMESTAMP;


@Entity
@XmlRootElement
@ViewPath("sikrede/sikredeyderrelation/v1")
@Table(name = "SikredeYderRelation")
public class SikredeYderRelation extends View
{
	@Id
	@GeneratedValue
	@XmlTransient
	@Column(name = "SikredeYderRelationPID")
	protected BigInteger recordID;

	@XmlElement(required = true)
	@Column(name = "CPR")
	protected String cpr;

	@XmlElement(required = true)
	protected String type;

	@XmlTransient
	@Temporal(TIMESTAMP)
	protected Date modifiedDate;

	@Temporal(TIMESTAMP)
	protected Date validFrom;

	@Temporal(TIMESTAMP)
	protected Date validTo;

	@XmlElement
	@Column(name = "ydernummer")
	protected int ydernummer;

	@XmlElement
	@Temporal(DATE)
	@Column(name = "ydernummerIkraftDato")
	protected Date ydernummerIkraftDato;

	@XmlElement
	@Column(name = "sikringsgruppeKode")
	protected String sikringsgruppeKode;

	@XmlElement
	@Temporal(DATE)
	@Column(name = "gruppeKodeIkraftDato")
	protected Date gruppeKodeIkraftDato;

	@XmlElement
	@Temporal(DATE)
	@Column(name = "gruppekodeRegistreringDato")
	protected Date gruppekodeRegistreringDato;

	@XmlElement
	@Temporal(DATE)
	@Column(name = "ydernummerRegistreringDato")
	protected Date ydernummerRegistreringDato;

	/*
	 * 
	 * Id VARCHAR(21) NOT NULL,
	 * 
	 * CreatedDate DATETIME NOT NULL,
	 */

	@Override
	public String getId()
	{
		return cpr + type;
	}

	@Override
	public BigInteger getRecordID()
	{
		return recordID; // To change body of implemented methods use File |
							// Settings | File Templates.
	}

	@Override
	public Date getUpdated()
	{
		return modifiedDate; // To change body of implemented methods use File |
								// Settings | File Templates.
	}
}
