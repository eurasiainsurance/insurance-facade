package tech.lapsa.insurance.facade.beans;

import static tech.lapsa.java.commons.function.MyExceptions.*;

import java.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.lapsa.insurance.domain.ContactData;
import com.lapsa.insurance.domain.IdentityCardData;
import com.lapsa.insurance.domain.OriginData;
import com.lapsa.insurance.domain.PersonalData;
import com.lapsa.insurance.domain.ResidenceData;
import com.lapsa.insurance.domain.policy.PolicyDriver;
import com.lapsa.insurance.elements.InsuranceClassType;
import com.lapsa.insurance.elements.InsuredAgeClass;
import com.lapsa.insurance.elements.Sex;

import tech.lapsa.insurance.esbd.NotFound;
import tech.lapsa.insurance.esbd.elements.InsuranceClassTypeService.InsuranceClassTypeServiceLocal;
import tech.lapsa.insurance.esbd.entities.SubjectPersonEntity;
import tech.lapsa.insurance.esbd.entities.SubjectPersonEntityService.SubjectPersonEntityServiceLocal;
import tech.lapsa.insurance.facade.PolicyDriverFacade;
import tech.lapsa.java.commons.function.MyExceptions.IllegalArgument;
import tech.lapsa.java.commons.function.MyExceptions.IllegalState;
import tech.lapsa.java.commons.function.MyOptionals;
import tech.lapsa.java.commons.time.MyTemporals;
import tech.lapsa.kz.taxpayer.TaxpayerNumber;

@Stateless
public class PolicyDriverFacadeBean implements PolicyDriverFacade {

    // READERS

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public InsuranceClassType getDefaultInsuranceClass() throws IllegalArgument, IllegalState {
	return reThrowAsChecked(() -> _getDefaultInsuranceClass());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public PolicyDriver getByTaxpayerNumber(final TaxpayerNumber idNumber) throws IllegalArgument, IllegalState {
	return reThrowAsChecked(() -> _getByTaxpayerNumber(idNumber));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public PolicyDriver getByTaxpayerNumberOrDefault(final TaxpayerNumber taxpayerNumber)
	    throws IllegalArgument, IllegalState {
	return reThrowAsChecked(() -> _getByTaxpayerNumberOrDefault(taxpayerNumber));
    }

    @Deprecated
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void fetch(final PolicyDriver driver) throws IllegalArgument, IllegalState {
	reThrowAsChecked(() -> _fetch(driver));
    }

    @Deprecated
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void clearFetched(final PolicyDriver driver) throws IllegalArgument, IllegalState {
	reThrowAsChecked(() -> _clearFetched(driver));
    }

    // MODIFIERS

    // PRIVATE

    @EJB
    private SubjectPersonEntityServiceLocal subjectPersonService;

    @EJB
    private InsuranceClassTypeServiceLocal insuranceClassTypeService;

    private InsuranceClassType _getDefaultInsuranceClass() {
	return insuranceClassTypeService.getDefault();
    }

    private PolicyDriver _getByTaxpayerNumberOrDefault(final TaxpayerNumber taxpayerNumber)
	    throws IllegalArgument, IllegalState {
	return MyOptionals.of(_getByTaxpayerNumber(taxpayerNumber)) //
		.orElseGet(() -> fillFromTaxpayerNumber(new PolicyDriver(), taxpayerNumber));
    }

    private PolicyDriver _getByTaxpayerNumber(final TaxpayerNumber idNumber) {
	return MyOptionals.of(idNumber) //
		.flatMap(number -> MyOptionals.ifAnyException(() -> subjectPersonService.getByIIN(number))) //
		.map(this::fetchFromESBDEntity) //
		.map(x -> fillFromTaxpayerNumber(x, idNumber)) //
		.orElse(null);
    }

    @Deprecated
    private void _fetch(final PolicyDriver driver) {
	_clearFetched(driver);
	final PolicyDriver fetched = _getByTaxpayerNumber(driver.getIdNumber());
	if (fetched == null)
	    return;

	driver.setFetched(fetched.isFetched());

	driver.setInsuranceClassType(fetched.getInsuranceClassType());
	driver.setAgeClass(fetched.getAgeClass());

	driver.setPersonalData(fetched.getPersonalData());
	driver.setResidenceData(fetched.getResidenceData());
	driver.setOriginData(fetched.getOriginData());
	driver.setIdentityCardData(fetched.getIdentityCardData());
	driver.setTaxPayerNumber(fetched.getTaxPayerNumber());
	driver.setContactData(fetched.getContactData());
    }

    @Deprecated
    private void _clearFetched(final PolicyDriver driver) {
	driver.setFetched(false);

	driver.setInsuranceClassType(_getDefaultInsuranceClass());
	driver.setAgeClass(null);

	driver.setPersonalData(new PersonalData());
	driver.setResidenceData(new ResidenceData());
	driver.setOriginData(new OriginData());
	driver.setIdentityCardData(new IdentityCardData());
	driver.setTaxPayerNumber(null);
	driver.setContactData(new ContactData());
    }

    private PolicyDriver fetchFromESBDEntity(final SubjectPersonEntity esbdEntity) {

	final PolicyDriver driver = new PolicyDriver();

	if (esbdEntity != null) {

	    final TaxpayerNumber idNumber = TaxpayerNumber.of(esbdEntity.getIdNumber());

	    if (idNumber != null)
		driver.setIdNumber(idNumber);

	    InsuranceClassType insuranceClassTypeLocal = null;
	    {
		insuranceClassTypeLocal = insuranceClassTypeService.getDefault();
		try {
		    insuranceClassTypeLocal = insuranceClassTypeService.getForSubject(esbdEntity);
		} catch (final NotFound e) {
		}
	    }

	    LocalDate dobLocal = null;
	    {
		if (esbdEntity != null && esbdEntity.getPersonal() != null
			&& esbdEntity.getPersonal().getDayOfBirth() != null)
		    dobLocal = esbdEntity.getPersonal().getDayOfBirth();
	    }

	    InsuredAgeClass insuredAgeClassLocal = null;
	    {
		if (dobLocal != null)
		    insuredAgeClassLocal = obtainInsuredAgeClass(dobLocal);
	    }

	    Sex sexLocal = null;
	    {
		if (esbdEntity != null && esbdEntity.getPersonal() != null
			&& esbdEntity.getPersonal().getSex() != null)
		    sexLocal = esbdEntity.getPersonal().getSex();
	    }

	    driver.setIdNumber(idNumber);

	    driver.setInsuranceClassType(insuranceClassTypeLocal);
	    driver.setAgeClass(insuredAgeClassLocal);

	    driver.getPersonalData().setDayOfBirth(dobLocal);
	    driver.getPersonalData().setSex(sexLocal);

	    if (esbdEntity != null) {
		driver.setFetched(true);

		if (esbdEntity.getPersonal() != null) {
		    driver.getPersonalData().setName(esbdEntity.getPersonal().getName());
		    driver.getPersonalData().setSurename(esbdEntity.getPersonal().getSurename());
		    driver.getPersonalData().setPatronymic(esbdEntity.getPersonal().getPatronymic());
		}

		if (esbdEntity.getOrigin() != null) {
		    driver.getResidenceData().setResident(esbdEntity.getOrigin().isResident());
		    driver.getOriginData().setCountry(esbdEntity.getOrigin().getCountry());
		}

		if (esbdEntity.getContact() != null)
		    driver.getResidenceData().setAddress(esbdEntity.getContact().getHomeAdress());

		if (esbdEntity.getOrigin().getCity() != null)
		    driver.getResidenceData().setCity(esbdEntity.getOrigin().getCity());

		if (esbdEntity.getIdentityCard() != null) {
		    driver.getIdentityCardData().setNumber(esbdEntity.getIdentityCard().getNumber());
		    driver.getIdentityCardData()
			    .setDateOfIssue(
				    MyTemporals.calendar().toLocalDate(esbdEntity.getIdentityCard().getDateOfIssue()));
		    driver.getIdentityCardData().setType(esbdEntity.getIdentityCard().getIdentityCardType());
		    driver.getIdentityCardData()
			    .setIssuingAuthority(esbdEntity.getIdentityCard().getIssuingAuthority());
		}

		if (esbdEntity.getContact() != null) {
		    driver.getContactData().setEmail(esbdEntity.getContact().getEmail());
		    driver.getContactData().setPhone(esbdEntity.getContact().getPhone());
		    driver.getContactData().setSiteUrl(esbdEntity.getContact().getSiteUrl());
		}

		driver.setTaxPayerNumber(esbdEntity.getTaxPayerNumber());
	    }
	}

	return driver;
    }

    // PRIVATE STATIC

    private PolicyDriver fillFromTaxpayerNumber(final PolicyDriver driver, final TaxpayerNumber taxpayerNumber) {

	if (driver.getIdNumber() == null)
	    driver.setIdNumber(taxpayerNumber);

	if (driver.getInsuranceClassType() == null)
	    driver.setInsuranceClassType(_getDefaultInsuranceClass());

	if (driver.getPersonalData().getDayOfBirth() == null)
	    taxpayerNumber.optionalDateOfBirth() //
		    .ifPresent(driver.getPersonalData()::setDayOfBirth);

	if (driver.getAgeClass() == null)
	    taxpayerNumber.optionalDateOfBirth() //
		    .map(this::obtainInsuredAgeClass)
		    .ifPresent(driver::setAgeClass);

	if (driver.getPersonalData().getSex() == null)
	    taxpayerNumber.optionalGender()
		    .map(this::convertKZLibSex)
		    .ifPresent(driver.getPersonalData()::setSex);

	return driver;
    }

    private Sex convertKZLibSex(final tech.lapsa.kz.taxpayer.Gender kzLibSex) {
	if (kzLibSex == null)
	    return null;
	switch (kzLibSex) {
	case FEMALE:
	    return Sex.FEMALE;
	case MALE:
	    return Sex.MALE;
	}
	return null;
    }

    private InsuredAgeClass obtainInsuredAgeClass(final LocalDate dayOfBirth) {
	if (dayOfBirth == null)
	    return null;
	final int years = calculateAgeByDOB(dayOfBirth);
	return _obtainInsuredAgeClass(years);
    }

    private static int calculateAgeByDOB(final LocalDate dob) {
	if (dob == null)
	    throw new NullPointerException();
	return dob.until(LocalDate.now()).getYears();
    }

    private static InsuredAgeClass _obtainInsuredAgeClass(final int years) {
	return years < 25 ? InsuredAgeClass.UNDER25 : InsuredAgeClass.OVER25;
    }
}
