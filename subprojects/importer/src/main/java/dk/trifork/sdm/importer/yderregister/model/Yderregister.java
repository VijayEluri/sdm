package dk.trifork.sdm.importer.yderregister.model;

import dk.trifork.sdm.model.AbstractStamdataEntity;
import dk.trifork.sdm.model.Id;
import dk.trifork.sdm.model.Output;
import dk.trifork.sdm.model.StamdataEntity;
import dk.trifork.sdm.util.DateUtils;

import java.util.Calendar;
import java.util.Date;

@Output
public class Yderregister extends AbstractStamdataEntity implements StamdataEntity{
	private String nummer;
	private String telefon;
	private String navn;
	private String vejnavn;
	private String postnummer;
	private String bynavn;
	private int amtNummer;
	private String email;
	private String www;
	private String hovedSpecialeKode;
	private String hovedSpecialeTekst;
    private String histID;
	private Date tilgangDato;
    private Date afgangDato;	

    
	@Id
    @Output	
	public String getNummer() {
		return nummer;
	}

	public void setNummer(String nummer) {
		this.nummer = nummer;
	}

    @Output	
	public String getTelefon() {
		return telefon;
	}

	public void setTelefon(String telefon) {
		this.telefon = telefon;
	}

    @Output
    public String getNavn() {
		return navn;
	}

	public void setNavn(String navn) {
		this.navn = navn;
	}

    @Output
    public String getVejnavn() {
		return vejnavn;
	}

	public void setVejnavn(String vejnavn) {
		this.vejnavn = vejnavn;
	}

    @Output
    public String getPostnummer() {
		return postnummer;
	}

	public void setPostnummer(String postnummer) {
		this.postnummer = postnummer;
	}

    @Output
    public String getBynavn() {
		return bynavn;
	}

	public void setBynavn(String bynavn) {
		this.bynavn = bynavn;
	}

    @Output
    public int getAmtNummer() {
		return amtNummer;
	}

	public void setAmtNummer(int amtNummer) {
		this.amtNummer = amtNummer;
	}

    @Output
    public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

    @Output
    public String getWww() {
		return www;
	}

	public void setWww(String www) {
		this.www = www;
	}

    @Output
    public String getHovedSpecialeKode() {
		return hovedSpecialeKode;
	}

	public void setHovedSpecialeKode(String hovedSpecialeKode) {
		this.hovedSpecialeKode = hovedSpecialeKode;
	}

    @Output
    public String getHovedSpecialeTekst() {
		return hovedSpecialeTekst;
	}

	public void setHovedSpecialeTekst(String hovedSpecialeTekst) {
		this.hovedSpecialeTekst = hovedSpecialeTekst;
	}

	@Output
    public String getHistID() {
		return histID;
	}

	public void setHistID(String histID) {
		this.histID = histID;
	}
	
    public Date getTilgangDato() {
		return tilgangDato;
	}

	public void setTilgangDato(Date tilgangDato) {
		this.tilgangDato = tilgangDato;
	}

	public Date getAfgangDato() {
		return afgangDato;
	}

	public void setAfgangDato(Date afgangDato) {
		this.afgangDato = afgangDato;
	}
	
	@Override
	public Calendar getValidFrom() {
		return DateUtils.toCalendar(tilgangDato);
	}

	@Override
	public Calendar getValidTo() {
		if (afgangDato != null)
			return DateUtils.toCalendar(afgangDato);
		return FUTURE;
	}

}
