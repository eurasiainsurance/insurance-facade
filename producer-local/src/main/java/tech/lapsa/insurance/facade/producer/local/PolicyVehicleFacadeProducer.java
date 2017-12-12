package tech.lapsa.insurance.facade.producer.local;

import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import tech.lapsa.insurance.facade.EJBViaCDI;
import tech.lapsa.insurance.facade.PolicyVehicleFacade;
import tech.lapsa.insurance.facade.PolicyVehicleFacade.PolicyVehicleFacadeLocal;

@Dependent
public class PolicyVehicleFacadeProducer {

    @EJB
    private PolicyVehicleFacadeLocal ejb;

    @Produces
    @EJBViaCDI
    public PolicyVehicleFacade getEjb() {
	return ejb;
    }
}
