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
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.*;

/**
 *
 * @author Abdelrahman
 */
public class BankBranchAgent extends Agent {

    private String manifestFile;
    private String logFile;

    private final Calendar calendar = Calendar.getInstance();
    private final String COMMA_DELIMITER = ",";
    private final String NEW_LINE_SEPARATOR = "\n";

    private BankBranch myBranch;
    private StringBuilder completeLog;

    private void resetDisks() {
        for (int i = 0; i < myBranch.getAvailableDesks().size(); i++) {
            myBranch.getAvailableDesks().get(i).reset();
        }
    }

    private Timestamp[] getServiceInsights(Timestamp arrivalTime) {
        Timestamp[] arr = new Timestamp[2]; // to represent the service start and finish times.
        for (int i = 0; i < myBranch.getAvailableDesks().size(); i++) {
            // if the desk is free, assign it and get service finish time
            if (myBranch.getAvailableDesks().get(i).isFreeNow(arrivalTime)) {
                arr[0] = arrivalTime;
                arr[1] = myBranch.getAvailableDesks().get(i).assign(arrivalTime);
                return arr;
            }
        }
        // if reached here, that means no desks were free
        // get all finish times and select the nearest one and assign
        Timestamp[] finishTimes = new Timestamp[myBranch.getAvailableDesks().size()];
        for (int i = 0; i < myBranch.getAvailableDesks().size(); i++) {
            finishTimes[i] = myBranch.getAvailableDesks().get(i).getLastFinishTime();
        }
        int nearestAvailableDesk = nearestAvailableDesk(finishTimes);
        arr[0] = myBranch.getAvailableDesks().get(nearestAvailableDesk).getLastFinishTime();
        arr[1] = myBranch.getAvailableDesks().get(nearestAvailableDesk).assign(arr[0]);
        return arr;
    }

    private int nearestAvailableDesk(Timestamp[] arr) {
        int index = 0;
        Timestamp min = arr[0];
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].before(min)) {
                index = i;
                min = arr[i];
            }
        }
        return index;
    }

    private boolean loadManifest() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(manifestFile));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String jsonString = sb.toString();
            JSONObject manifest = new JSONObject(jsonString);
            String branchID = manifest.getString("branchID");
            String branchSize = manifest.getString("branchSize");
            JSONArray arr = manifest.getJSONArray("services");
            ArrayList<String> services = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                services.add(arr.getString(i));
            }

            JSONArray desks = manifest.getJSONArray("desks");
            ArrayList<Desk> availableDesks = new ArrayList<>();
            for (int i = 0; i < desks.length(); i++) {
                JSONObject deskType = (JSONObject) desks.get(i);
                int numberOfDesks = Integer.parseInt(deskType.getString("numderOfDesks"));
                for (int j = 0; j < numberOfDesks; j++) {
                    Desk temp = new Desk(deskType.getString("deskID"), Float.parseFloat(deskType.getString("costPerDesk")), Integer.parseInt(deskType.getString("averageProcessingTime")));
                    availableDesks.add(temp);
                }
            }
            myBranch = new BankBranch(branchID, branchSize, services, availableDesks);

        } catch (Exception ex) {
            return false;
        } finally {
            try {
                br.close();
                return true;
            } catch (IOException ex) {
                return false;
            }
        }
    }

    private boolean loadLog() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(logFile));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                // split the line
                String[] tokens = line.split(",");
                // get ARRIVAL_TIME
                Timestamp arrivalTime = Timestamp.valueOf(tokens[2]);
                // compute SERVICE_START_TIME and SERVICE_FINISH_TIME
                Timestamp[] serviceTimes = getServiceInsights(arrivalTime);
                // append the new record to the sb
                sb.append(line);
                sb.append(COMMA_DELIMITER);
                sb.append(serviceTimes[0]);
                sb.append(COMMA_DELIMITER);
                sb.append(serviceTimes[1]);
                sb.append(NEW_LINE_SEPARATOR);
                line = br.readLine();
            }
            // save the sb to the string builder
            completeLog = new StringBuilder(sb);

            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                br.close();
                return true;
            } catch (IOException ex) {
                return false;
            }
        }
    }

    private class RegisterBranchBehavior extends OneShotBehaviour {

        private String recommenderAgentName = "recommender";
        private BankBranch branch;

        public RegisterBranchBehavior(BankBranch b) {
            this.branch = b;
        }

        @Override
        public void action() {
            AID recommenderAgent = new AID(recommenderAgentName, AID.ISLOCALNAME);
            ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);
            msg.addReceiver(recommenderAgent);
            try {
                msg.setContentObject(branch);
            } catch (IOException ex) {
                Logger.getLogger(RegisterBranchBehavior.class.getName()).log(Level.SEVERE, null, ex);
            }
            myAgent.send(msg);

            myAgent.addBehaviour(new ConfirmRegisterBranchBehavior());
        }
    }

    private class ConfirmRegisterBranchBehavior extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage recommenderReply = myAgent.receive();
            if (recommenderReply != null) {
                if (recommenderReply.getPerformative() == ACLMessage.CONFIRM) {
                    System.out.println(myAgent.getLocalName() + ":branch -> successfully submitted manifest");
                    System.out.println(myAgent.getLocalName() + ":branch -> recommender says: " + recommenderReply.getContent());
                } else {
                    System.err.println(myAgent.getLocalName() + ":branch -> recommender rejected the manifest submitted!");
                    System.err.println(myAgent.getLocalName() + ":branch -> recommender says: " + recommenderReply.getContent());
                }
                myAgent.removeBehaviour(this);
            } else {
                block();
            }
        }
    }

    private class SendLogBehavior extends OneShotBehaviour {

        private String recommenderAgentName = "recommender";
        private StringBuilder log;

        public SendLogBehavior(StringBuilder log) {
            this.log = log;
        }

        @Override
        public void action() {
            AID recommenderAgent = new AID(recommenderAgentName, AID.ISLOCALNAME);
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);
            msg.addReceiver(recommenderAgent);
            try {
                msg.setContentObject(log);
            } catch (IOException ex) {
                Logger.getLogger(SendLogBehavior.class.getName()).log(Level.SEVERE, null, ex);
            }
            myAgent.addBehaviour(new ConversationBehavior());
            myAgent.send(msg);
        }
    }
    
    private class ConversationBehavior extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage recommendation = myAgent.receive();
            if(recommendation != null){
                if(recommendation.getPerformative() == ACLMessage.CONFIRM){
                    // it is optimized
                    System.out.println(myAgent.getLocalName() + ":branch -> manifest is now optimized as follows:\n" + myBranch);
                } else if (recommendation.getPerformative() == ACLMessage.PROPOSE){
                    try {
                        // get the new manifest, recompute the log and send back again.
                        myBranch = (BankBranch)recommendation.getContentObject();
                        resetDisks();
                        if(loadLog()){
                            // myAgent.removeBehaviour(this);
                            myAgent.addBehaviour(new SendLogBehavior(completeLog));
                            System.out.println(myAgent.getLocalName() + ":branch -> sent updated log based on recommender proposal ..");
                        } else {
                            System.out.println(myAgent.getLocalName() + ":branch -> cannot re-compute the log based on recommender proposal !!");
                        }
                    } catch (UnreadableException ex) {
                        Logger.getLogger(BankBranchAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    // some failure!
                    System.out.println(myAgent.getLocalName() + ":branch -> recommender failed to propose !!");
                }
            }else{
                block();
            }
        }
    }

    @Override
    protected void setup() {
        // receive manifest file path through command line arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String[] arguments = ((String) args[0]).split(" ");
            manifestFile = arguments[0];
            logFile = arguments[1];
            if (loadManifest()) {
                System.out.println("Branch " + myBranch.getBranchID() + " is ready.");
                if (loadLog()) {
                    System.out.println("Branch " + myBranch.getBranchID() + " has processed all clients ..");
                }
            } else {
                System.err.println("Manifest file error!");
            }
        } else {
            System.err.println("Missing manifest or log file path!");
            doDelete();
        }

        // register to the recommender agent
        addBehaviour(new RegisterBranchBehavior(myBranch));

        // send log file
        addBehaviour(new SendLogBehavior(completeLog));
    }

    @Override
    protected void takeDown() {
        if (myBranch.getBranchID() != null) {
            System.out.println("Branch " + myBranch.getBranchID() + " is terminating.");
        }
    }
}
