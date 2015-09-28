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
import jade.lang.acl.ACLMessage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
public class BankBranchAgent extends Agent{
    
    private String manifestFile;
    private String logFile;
    
    private final Calendar calendar = Calendar.getInstance();
    private final String COMMA_DELIMITER = ",";
    private final String NEW_LINE_SEPARATOR = "\n";
    
    private BankBranch myBranch;
    private StringBuilder completeLog;

    public BankBranch getMyBranch() {
        return myBranch;
    }


    private Timestamp[] getServiceInsights(Timestamp arrivalTime){
        Timestamp[] arr = new Timestamp[2]; // to represent the service start and finish times.
        for(int i = 0;i<myBranch.getAvailableDesks().size(); i++){
            // if the desk is free, assign it and get service finish time
            if(myBranch.getAvailableDesks().get(i).isFreeNow(arrivalTime)){
                arr[0] = arrivalTime;
                arr[1] = myBranch.getAvailableDesks().get(i).assign(arrivalTime);
                return arr;
            }
        }
        // if reached here, that means no desks were free
        // get all finish times and select the nearest one and assign
        Timestamp[] finishTimes = new Timestamp[myBranch.getAvailableDesks().size()];
        for(int i = 0; i< myBranch.getAvailableDesks().size();i++){
            finishTimes[i] = myBranch.getAvailableDesks().get(i).getLastFinishTime();
        }
        int nearestAvailableDesk = nearestAvailableDesk(finishTimes);
        arr[0] = myBranch.getAvailableDesks().get(nearestAvailableDesk).getLastFinishTime();
        arr[1] = myBranch.getAvailableDesks().get(nearestAvailableDesk).assign(arr[0]);
        return arr;
    }
    private int nearestAvailableDesk(Timestamp[] arr){
        int index = 0;
        Timestamp min = arr[0];
        for(int i = 0; i<arr.length;i++){
            if(arr[i].before(min)){
                index = i;
                min = arr[i];
            }
        }
        return index;
    }
    private boolean loadManifest(){
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(manifestFile));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while(line != null){
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
            for(int i = 0;i<arr.length();i++){
                services.add(arr.getString(i));
            }
            
            JSONArray desks = manifest.getJSONArray("desks");
            ArrayList<Desk> availableDesks = new ArrayList<>();
            for(int i = 0;i<desks.length(); i++){
                JSONObject deskType = (JSONObject) desks.get(i);
                int numberOfDesks = Integer.parseInt(deskType.getString("numderOfDesks"));
                for(int j = 0; j< numberOfDesks; j++){
                    Desk temp = new Desk(deskType.getString("deskID"), Float.parseFloat(deskType.getString("costPerDesk")) , Integer.parseInt(deskType.getString("averageProcessingTime")));
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
    private boolean loadLog(){
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(logFile));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while(line != null){
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
    
    @Override
    protected void setup(){
        // receive manifest file path through command line arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String[] arguments = ((String) args[0]).split(" ");
            manifestFile = arguments[0];
            logFile = arguments[1];
            if(loadManifest()){
                System.out.println("Branch " + myBranch.getBranchID() + " is ready.");
                if(loadLog()){
                    System.out.println("Branch " + myBranch.getBranchID() + " has processed all clients ..");
                }
            }else {
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
    protected void takeDown(){
        if(myBranch.getBranchID() != null){
            System.out.println("Branch " + myBranch.getBranchID() + " is terminating.");
        }
    }
}
