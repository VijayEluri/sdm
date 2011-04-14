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

package com.trifork.stamdata.replication.replication.views.dkma;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.trifork.stamdata.replication.replication.views.View;
import com.trifork.stamdata.replication.replication.views.ViewPath;


@Entity
@XmlRootElement
@ViewPath("dkma/pakning/v1")
public class Pakning extends View {

	@Id
	@GeneratedValue
	@Column(name = "PakningPID")
	@XmlTransient
	private BigInteger recordID;

	@Column(name = "Varenummer")
	protected BigInteger varenummer;

	@Column(name = "VarenummerDelpakning")
	protected BigInteger varenummerDelpakning;

	@Column(name = "DrugID")
	protected String drugId;

	@Column(name = "PakningsstoerrelseNumerisk")
	protected Double pakningsstoerrelseNumerisk;

	@Column(name = "Pakningsstoerrelsesenhed")
	protected String pakningsstoerrelseEnhed;

	@Column(name = "PakningsstoerrelseTekst")
	protected String pakningsstoerrelseTekst;

	@Column(name = "EmballageTypeKode")
	protected String emballageTypeKode;

	@Column(name = "Dosisdispenserbar")
	protected Boolean dosisdispenserbar;

	@Column(name = "MedicintilskudsKode")
	protected String medicintilskudsKode;

	@Column(name = "KlausuleringsKode")
	protected String klausuleringsKode;

	@Column(name = "AlfabetSekvensnr")
	protected BigInteger alfabetSekvensNummer;

	@Column(name = "AntalDelpakninger")
	protected BigInteger antalDelpakninger;

	@Column(name = "Udleveringsbestemmelse")
	protected String udleveringsbestemmelse;

	@Column(name = "UdleveringSpeciale")
	protected String udleveringSpeciale;

	@Column(name = "AntalDDDPrPakning")
	protected Double antalDDDPrPakning;

	@Column(name = "OpbevaringstidNumerisk")
	protected BigInteger opbevaringstidNumerisk;

	@Column(name = "Opbevaringstid")
	protected BigInteger opbevaringstid;

	@Column(name = "Opbevaringsbetingelser")
	protected String opbevaringsbetingelser; // TODO: VARCHAR(1) why?

	@Column(name = "Oprettelsesdato")
	protected String oprettelsesdato; // TODO: VARCHAR why?

	@Column(name = "DatoForSenestePrisaendring")
	protected String datoForSenestePrisaendring; // TODO: VARCHAR why?

	@Column(name = "UdgaaetDato")
	protected String udgaaetDato;

	@Column(name = "BeregningskodeAIRegpris")
	protected String BeregningskodeAIRegpris;

	@Column(name = "PakningOptagetITilskudsgruppe")
	protected Boolean pakningOptagetITilskudsgruppe;

	@Column(name = "Faerdigfremstillingsgebyr")
	protected Boolean faerdigfremstillingsgebyr;

	@Column(name = "Pakningsdistributoer")
	protected BigInteger pakningsdistributoer;

	// Metadata

	@XmlTransient
	@Column(name = "ModifiedDate")
	private Date modifiedDate;

	@Column(name = "ValidFrom")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date validFrom;

	@Column(name = "ValidTo")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date validTo;

	@Override
	public String getId() {

		return varenummer.toString();
	}

	@Override
	public Date getUpdated() {

		return modifiedDate;
	}

	@Override
	public BigInteger getRecordID() {

		return recordID;
	}
}