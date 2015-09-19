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
import jade.core.Agent;
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
    private class Desk {
        private final Calendar calendar = Calendar.getInstance();
        private String deskID;
        private float cost; // in $
        private int averageProcessingTime; // in minutes
        private Timestamp lastFinishTime;

        public Desk(String deskID, float cost, int averageProcessingTime) {
            this.deskID = deskID;
            this.cost = cost;
            this.averageProcessingTime = averageProcessingTime;
            this.lastFinishTime = null;
        }

        @Override
        public String toString() {
            return "Desk{" + "deskID=" + deskID + ", cost=" + cost + ", averageProcessingTime=" + averageProcessingTime + '}';
        }

        public float getCost() {
            return cost;
        }
        
        // to check if this desk is currently free or not.
        public boolean isFreeNow(Timestamp nowTime){
            if(lastFinishTime == null) {
                // desk never called.
                return true;
            } else if (nowTime.after(lastFinishTime)){
                // client arrived after the desk finished the previous one
                return true;
            }
            return false;
        }
        
        // if the desk is not free, return when it will finish handling the current client
        public Timestamp getLastFinishTime(){
            return lastFinishTime;
        }
        
        // assign a client to this desk and get the finish time
        public Timestamp assign(Timestamp nowTime){
            lastFinishTime = new Timestamp(nowTime.getTime() + averageProcessingTime*60*1000);
            return lastFinishTime;
        }
    }
    private String manifestFile;
    private String logFile;
    
    private final Calendar calendar = Calendar.getInstance();
    private final String COMMA_DELIMITER = ",";
    private final String NEW_LINE_SEPARATOR = "\n";
    
    private String branchID;
    private String branchSize;
    private JSONArray services;
    private ArrayList<Desk> availableDesks;
    private Timestamp[] getServiceInsights(Timestamp arrivalTime){
        Timestamp[] arr = new Timestamp[2]; // to represent the service start and finish times.
        for(int i = 0;i<availableDesks.size(); i++){
            // if the desk is free, assign it and get service finish time
            if(availableDesks.get(i).isFreeNow(arrivalTime)){
                arr[0] = arrivalTime;
                arr[1] = availableDesks.get(i).assign(arrivalTime);
                return arr;
            }
        }
        // if reached here, that means no desks were free
        // get all finish times and select the nearest one and assign
        Timestamp[] finishTimes = new Timestamp[availableDesks.size()];
        for(int i = 0; i< availableDesks.size();i++){
            finishTimes[i] = availableDesks.get(i).getLastFinishTime();
        }
        int nearestAvailableDesk = nearestAvailableDesk(finishTimes);
        arr[0] = availableDesks.get(nearestAvailableDesk).getLastFinishTime();
        arr[1] = availableDesks.get(nearestAvailableDesk).assign(arr[0]);
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
            branchID = manifest.getString("branchID");
            branchSize = manifest.getString("branchSize");
            services = manifest.getJSONArray("services");
            
            JSONArray desks = manifest.getJSONArray("desks");
            availableDesks = new ArrayList<>();
            for(int i = 0;i<desks.length(); i++){
                JSONObject deskType = (JSONObject) desks.get(i);
                int numberOfDesks = Integer.parseInt(deskType.getString("numderOfDesks"));
                for(int j = 0; j< numberOfDesks; j++){
                    Desk temp = new Desk(deskType.getString("deskID"), Float.parseFloat(deskType.getString("costPerDesk")) , Integer.parseInt(deskType.getString("averageProcessingTime")));
                    availableDesks.add(temp);
                }
            }
            
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
            // save the sb to a new file
            FileWriter fr = new FileWriter( branchID + " processed.csv");
            fr.write(sb.toString());
            fr.flush();
            fr.close();
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
            manifestFile = "C:\\Users\\Abdelrahman\\Downloads\\Bank Operation\\b1.json";
            logFile = "C:\\Users\\Abdelrahman\\Downloads\\Bank Operation\\log-headquarter.csv";
            // manifestFile = (String) args[0];
            // logFile = (String) args[1];
            if(loadManifest()){
                System.out.println("Branch " + branchID + " is ready.");
                if(loadLog()){
                    System.out.println("Branch " + branchID + " has processed all clients ..");
                }
            }else {
                System.err.println("Manifest file error!");
            }
        } else {
            System.err.println("Missing manifest or log file path!");
            doDelete();
        }
        
        // register to the recommender agent
        
        // send log file
    }
    @Override
    protected void takeDown(){
        if(branchID != null){
            System.out.println("Branch " + branchID + " is terminating.");
        }
    }
}
