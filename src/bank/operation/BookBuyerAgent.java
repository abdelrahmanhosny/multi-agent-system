/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bank.operation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
/**
 *
 * @author Abdelrahman
 */
public class BookBuyerAgent extends Agent{
    private AID[] sellerAgents = {new AID("seller1", AID.ISLOCALNAME), new AID("seller2", AID.ISLOCALNAME)};

    @Override
    protected void setup(){
        System.out.println("Hello! Buyer-Agent " + getAID().getName() + " is ready.");
        addBehaviour(new TickerBehaviour(this, 60000) {
            @Override
            protected void onTick() {
                myAgent.addBehaviour(new RequestPerformer());
            }
        });
    }
    @Override
    protected void takeDown(){
        System.out.println("Hello! Buyer-Agent " + getAID().getName() + " terminating.");
    }

    private static class RequestPerformer extends Behaviour{

        public RequestPerformer() {
        }

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
