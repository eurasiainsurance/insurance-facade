package com.lapsa.eurasia36.facade.beans;

import java.util.List;
import java.util.stream.Stream;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.lapsa.commons.function.MyCollectors;
import com.lapsa.commons.function.MyObjects;
import com.lapsa.commons.function.MyOptionals;
import com.lapsa.eurasia36.facade.CompanyPointOfSaleFacade;
import com.lapsa.insurance.dao.CompanyPointOfSaleDAO;
import com.lapsa.insurance.domain.CompanyPointOfSale;
import com.lapsa.insurance.domain.PostAddress;
import com.lapsa.kz.country.KZCity;

@Stateless
public class CompanyPointOfSaleFacadeBean implements CompanyPointOfSaleFacade {

    @Inject
    private CompanyPointOfSaleDAO companyPointOfSaleDAO;

    @Override
    public List<CompanyPointOfSale> pointOfSalesForPickup() {
	return allAvailable() //
		.filter(CompanyPointOfSale::isPickupAvailable) //
		.collect(MyCollectors.unmodifiableList());
    }

    @Override
    public List<CompanyPointOfSale> pointOfSalesForPickup(final KZCity city) {
	MyObjects.requireNonNull(city, "city");
	return allAvailable() //
		.filter(CompanyPointOfSale::isPickupAvailable)
		.filter(x -> MyObjects.nonNull(x.getAddress()))
		.filter(x -> city.equals(x.getAddress().getCity()))
		.collect(MyCollectors.unmodifiableList());
    }

    @Override
    public List<CompanyPointOfSale> pointOfSalesForDelivery() {
	return allAvailable() //
		.filter(CompanyPointOfSale::isDeliveryServicesAvailable) //
		.collect(MyCollectors.unmodifiableList());
    }

    @Override
    public List<CompanyPointOfSale> pointOfSalesForDelivery(final KZCity city) {
	MyObjects.requireNonNull(city, "city");
	return allAvailable() //
		.filter(CompanyPointOfSale::isDeliveryServicesAvailable) //
		.filter(x -> MyObjects.nonNull(x.getAddress())) //
		.filter(x -> city.equals(x.getAddress().getCity())) //
		.collect(MyCollectors.unmodifiableList());
    }

    @Override
    public List<KZCity> getCitiesForPickup() {
	return allAvailable() //
		.filter(CompanyPointOfSale::isPickupAvailable) //
		.map(CompanyPointOfSale::getAddress) //
		.map(PostAddress::getCity) //
		.distinct() //
		.collect(MyCollectors.unmodifiableList());
    }

    @Override
    public List<CompanyPointOfSale> findAllOwnOffices() {
	return allAvailable() //
		.filter(CompanyPointOfSale::isCompanyOwnOffice)
		.collect(MyCollectors.unmodifiableList());
    }

    // PRIVATE

    private Stream<CompanyPointOfSale> allAvailable() {
	List<CompanyPointOfSale> poses = companyPointOfSaleDAO.findAll();
	return MyOptionals.streamOf(poses) //
		.orElseGet(Stream::empty) //
		.filter(CompanyPointOfSale::isAvailable);
    }
}
