package tech.lapsa.insurance.facade.producer.local;

import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import tech.lapsa.insurance.facade.EJBViaCDI;
import tech.lapsa.insurance.facade.NotificationFacade;
import tech.lapsa.insurance.facade.NotificationFacade.NotificationFacadeLocal;

@Dependent
public class NotificationFacadeProducer {

    @EJB
    private NotificationFacadeLocal ejb;

    @Produces
    @EJBViaCDI
    public NotificationFacade getEjb() {
	return ejb;
    }
}
