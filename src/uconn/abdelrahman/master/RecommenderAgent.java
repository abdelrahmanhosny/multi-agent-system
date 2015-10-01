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
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Abdelrahman
 */
public class RecommenderAgent extends Agent {

    private final int ACCEPTABLE_AVERAGE_WAIT_TIME = 5; // in minutes

    private enum LastRecommendationStatus {

        NEVER, INCREASE, DECREASE
    };
    private HashMap<String, BankBranch> manifestMap;
    private HashMap<String, LastRecommendationStatus> lastRecommendationMap;
    private HashMap<String, Integer> desksCountMap;
    private HashMap<String, Double> utilizedWaitTimeMap;

    private class ManifestServer extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg;
            msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE));
            if (msg != null) {
                // process the message
                System.out.println(myAgent.getLocalName() + ":recommender -> received manifest ..");
                try {
                    System.out.println(myAgent.getLocalName() + ":recommender -> " + msg.getContentObject());
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
                System.out.println(myAgent.getLocalName() + ":recommender -> received log ..");
                StringBuilder log = null;
                try {
                    // System.out.println(myAgent.getName() + ":recommender -> content: " + msg.getContentObject());
                    log = (StringBuilder) msg.getContentObject();
                } catch (UnreadableException ex) {
                    Logger.getLogger(ManifestServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                // get sender name
                AID sender = msg.getSender();
                String branchName = sender.getLocalName();

                // analyze this log and suggest a new modified log
                if (log != null) {
                    BankBranch newManifest = getNewRecommendedManifest(branchName, log);

                    if (newManifest != null) {
                        // send new manifest to the branch agent
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.PROPOSE);
                        try {
                            reply.setContentObject(newManifest);
                        } catch (IOException ex) {
                            Logger.getLogger(RecommenderAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        myAgent.send(reply);
                    } else {
                        // send success status to the branch agent to indicate that it's all utilized.
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.CONFIRM);
                        try {
                            reply.setContentObject(newManifest);
                        } catch (IOException ex) {
                            Logger.getLogger(RecommenderAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        myAgent.send(reply);
                    }

                } else {
                    // send fail status to the branch agent
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("cannot parse log correctly!");
                    myAgent.send(reply);
                }

            } else {
                block();
            }
        }
    }

    private double[] getAverageWaitingTime(StringBuilder log) {
        double averageWaitTime = 0;
        int numberOfTransactions = 0;
        String fullLog = log.toString();
        String[] lines = fullLog.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String[] tokens = lines[i].split(",");
            Timestamp arrivalTS = Timestamp.valueOf(tokens[2]);
            Timestamp startTS = Timestamp.valueOf(tokens[3]);
            long diff = startTS.getTime() - arrivalTS.getTime();

            averageWaitTime += (diff / (1000.0 * 60));
            numberOfTransactions++;
        }
        averageWaitTime /= numberOfTransactions;
        double[] result = new double[2];
        result[0] = averageWaitTime;
        result[1] = numberOfTransactions;
        return result;
    }

    private Desk getARecommendedDesk(double avgWait) {
        if (avgWait > 3 * ACCEPTABLE_AVERAGE_WAIT_TIME) {
            // recommed the most expensive desk
            return new Desk("rec1", (float) 200.0, 3);
        } else if (avgWait > 2 * ACCEPTABLE_AVERAGE_WAIT_TIME) {
            // recommed a less expensive desk
            return new Desk("rec2", (float) 100.0, 8);
        } else {
            // recommed the least expensive desk
            return new Desk("rec3", (float) 50.0, 12);
        }
    }

    private BankBranch getNewRecommendedManifest(String branchName, StringBuilder log) {
        // return a new manifest if there is modifications. null if the analysis is ok.
        // analyze average waiting time
        double[] res = getAverageWaitingTime(log);
        double averageWaitTime = res[0];
        int numberOfClients = (int) res[1];
        System.out.println("recommender - > average waiting time for " + branchName + " is " + averageWaitTime + " minutes");

        if (averageWaitTime > ACCEPTABLE_AVERAGE_WAIT_TIME) {
            // if waiting time > ACCEPTABLE_AVERAGE_WAIT_TIME minutes, recommend increase
            // get increase desk type
            Desk toBeAdded = getARecommendedDesk(averageWaitTime);
            // update its corresponding manifest
            BankBranch newManifest = manifestMap.get(branchName);
            newManifest.addDesk(toBeAdded);
            manifestMap.put(branchName, newManifest);
            lastRecommendationMap.put(branchName, LastRecommendationStatus.INCREASE);
            Integer desksCount = desksCountMap.get(branchName);
            desksCount++;
            desksCountMap.put(branchName, desksCount);
            System.out.println("recommender - > sending increase recommendation for " + branchName + " with new manifest ->\n" + newManifest);
            // return the new manifest
            return manifestMap.get(branchName);
        } else {
            // if waiting time < ACCEPTABLE_AVERAGE_WAIT_TIME minutes, recommend decrease
            if (lastRecommendationMap.get(branchName) == LastRecommendationStatus.INCREASE) {
                // if last time we recommended increase, that means the average waiting time is just in the acceptable range
                // it is the steady state
                System.out.println("recommender - > reached steady state for " + branchName + " with a utilized waiting time: " + averageWaitTime);

                utilizedWaitTimeMap.put(branchName, averageWaitTime);

                // if we have more than one branch in the system, try to provide global recoomendation
                if (desksCountMap.size() > 1) {
                    printGlobalRecommendation();
                }
                return null;
            } else {
                // if the last time we recommended decrease, recommend another decrease.
                // pick a desk from the manifest to remove
                // update its correspoding manifest
                BankBranch newManifest = manifestMap.get(branchName);
                newManifest.removeDesk();
                manifestMap.put(branchName, newManifest);
                lastRecommendationMap.put(branchName, LastRecommendationStatus.DECREASE);
                Integer desksCount = desksCountMap.get(branchName);
                desksCount--;
                desksCountMap.put(branchName, desksCount);
                System.out.print(":recommender - > sending decrease recommendation for " + branchName + "\nNew manifest -> " + newManifest);
                return manifestMap.get(branchName);
            }
        }
    }

    private void addBranch(BankBranch b) {
        manifestMap.put(b.getBranchID(), b);
        lastRecommendationMap.put(b.getBranchID(), LastRecommendationStatus.NEVER);
        desksCountMap.put(b.getBranchID(), 0);
        utilizedWaitTimeMap.put(b.getBranchID(), -1.0);
    }

    private void printGlobalRecommendation() {
        System.out.println(":recommender -> global recommender started analysis on " + desksCountMap.size() + " branches ..");
        // do we have a branch that added a desk?
        ArrayList<String> branchesAddedADesk = new ArrayList<>();
        ArrayList<String> branchesRemovedADesk = new ArrayList<>();

        Set keys = desksCountMap.keySet();
        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            String branchName = (String) iter.next();
            if (desksCountMap.get(branchName) > 0) {
                branchesAddedADesk.add(branchName);
            } else if (desksCountMap.get(branchName) < 0) {
                branchesRemovedADesk.add(branchName);
            }
        }

        // transfer recommendation
        for (int i = 0; i < branchesAddedADesk.size(); i++) {
            for (int j = 0; j < branchesRemovedADesk.size(); j++) {
                System.out.println(":global recommender -> transfer from " + branchesRemovedADesk.get(j) + " to " + branchesAddedADesk.get(i));
            }
        }

        // swap recommendation
        iter = keys.iterator();
        ArrayList<String> allBranches = new ArrayList<String>();
        while (iter.hasNext()) {
            String branchName = (String) iter.next();
            allBranches.add(branchName);
        }
        for (int i = 0; i < allBranches.size(); i++) {
            // if branch has a waiting time of zero
            if(utilizedWaitTimeMap.get(allBranches.get(i)) < 0.001){
                for (int j = 0; j < allBranches.size(); j++) {
                    if(utilizedWaitTimeMap.get(allBranches.get(j)) >= 0.001){
                        System.out.println(":global recommender -> swap between " + allBranches.get(i) + " and " + allBranches.get(j));
                    }
                }
            }
        }
    }

    @Override
    protected void setup() {
        manifestMap = new HashMap<String, BankBranch>();
        lastRecommendationMap = new HashMap<String, LastRecommendationStatus>();
        desksCountMap = new HashMap<String, Integer>();
        utilizedWaitTimeMap = new HashMap<String, Double>();
        System.out.println(getLocalName() + ":recommender agent -> started ..");
        addBehaviour(new ManifestServer());
        addBehaviour(new LogServer());
    }

    @Override
    protected void takeDown() {
        System.out.println(getLocalName() + ":recommender agent -> shut down ..");
    }
}
