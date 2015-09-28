/*
 * Copyright (C) 2015 Abdelrahman
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package uconn.abdelrahman.master;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Abdelrahman
 */
public class RecommenderAgent extends Agent {

    private ArrayList<BankBranch> branches;

    private class ManifestServer extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg;
            msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE));
            if (msg != null) {
                // process the message
                System.out.println(myAgent.getName() + ":recommender -> received message ..");
                try {
                    System.out.println(myAgent.getName() + ":recommender -> content: " + msg.getContentObject());
                    // add branch to the branches list
                    addBranch((BankBranch) msg.getContentObject());
                } catch (UnreadableException ex) {
                    Logger.getLogger(ManifestServer.class.getName()).log(Level.SEVERE, null, ex);
                }

                // send success status to the branch agent
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("received manifest successfully");
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class LogServer extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg;
            msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
            if (msg != null) {
                // process the message
                System.out.println(myAgent.getName() + ":recommender -> received message ..");
                try {
                    System.out.println(myAgent.getName() + ":recommender -> content: " + msg.getContentObject());
                } catch (UnreadableException ex) {
                    Logger.getLogger(ManifestServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                // get sender name
                AID sender = msg.getSender();
                String branchName = sender.getName();

                // analyze this log and suggest a new modified log
                getNewRecommendedManifest();

                // send success status to the branch agent
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("received log successfully");
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private void getNewRecommendedManifest() {
        
    }

    private void addBranch(BankBranch b) {
        branches.add(b);
    }

    @Override
    protected void setup() {
        branches = new ArrayList<>();
        System.out.println(getName() + ":recommender agent -> started ..");
        addBehaviour(new ManifestServer());
        addBehaviour(new LogServer());
    }

    @Override
    protected void takeDown() {
        System.out.println(getName() + ":recommender agent -> shut down ..");
    }
}
