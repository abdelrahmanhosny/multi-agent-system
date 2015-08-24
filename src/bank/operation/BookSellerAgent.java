/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bank.operation;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import java.util.Hashtable;

/**
 *
 * @author Abdelrahman
 */
public class BookSellerAgent extends Agent{
    private Hashtable catalogue;
    @Override
    protected void setup(){
        catalogue = new Hashtable();
        catalogue.put("Java", 10);
        catalogue.put("C", 15);
        catalogue.put("Data Communication", 23);
        catalogue.put("Python", 58);
        addBehaviour(new OfferRequestsServer());
        addBehaviour(new PurchaseOrdersServer());
    }

    private static class OfferRequestsServer extends Behaviour {

        @Override
        public void action() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean done() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class PurchaseOrdersServer extends Behaviour {

        @Override
        public void action() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean done() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }   
}
