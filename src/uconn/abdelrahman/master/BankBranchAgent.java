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
import java.io.IOException;
import org.json.*;
/**
 *
 * @author Abdelrahman
 */
public class BankBranchAgent extends Agent{
    private String manifestFile;
    private JSONObject manifest;
    
    private String branchID;
    private String branchSize;
    private JSONArray services;
    private JSONArray desks;
    
    private boolean loadManifest(){
        try {
            BufferedReader br = new BufferedReader(new FileReader(manifestFile));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while(line != null){
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String jsonString = sb.toString();
            manifest = new JSONObject(jsonString);
            branchID = manifest.getString("branchID");
            branchSize = manifest.getString("branchSize");
            services = manifest.getJSONArray("services");
            desks = manifest.getJSONArray("desks");
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
    
    @Override
    protected void setup(){
        // receive manifest file path through command line arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            manifestFile = (String) args[0];
            if(loadManifest()){
                System.out.println("Hello! Branch " + branchID + " is ready.");
            }else {
                System.err.println("Manifest file error!");
            }
        } else {
            System.err.println("Bank branch manifest path is not passed!");
            doDelete();
        }
        
        // register to the recommender agent
        
        // send log files
    }
    @Override
    protected void takeDown(){
        System.out.println("Hello! Branch " + branchID + " is terminating.");
    }
}
