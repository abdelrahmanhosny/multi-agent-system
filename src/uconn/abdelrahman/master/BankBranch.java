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

import java.io.Serializable;
import java.util.ArrayList;
import org.json.JSONArray;

/**
 *
 * @author Abdelrahman
 */
public class BankBranch implements Serializable{
    private String branchID;
    private String branchSize;
    private ArrayList<String> services;
    private ArrayList<Desk> availableDesks;

    public BankBranch(String branchID, String branchSize, ArrayList<String> services, ArrayList<Desk> availableDesks) {
        this.branchID = branchID;
        this.branchSize = branchSize;
        this.services = services;
        this.availableDesks = availableDesks;
    }

    public String getBranchID() {
        return branchID;
    }

    public String getBranchSize() {
        return branchSize;
    }

    public ArrayList<String> getServices() {
        return services;
    }

    public ArrayList<Desk> getAvailableDesks() {
        return availableDesks;
    }
    
    public void addDesk(Desk d){
        availableDesks.add(d);
    }
    public void removeDesk(){
        availableDesks.remove(0);
    }

    @Override
    public String toString() {
        return "BankBranch{" + "branchID=" + branchID + ", branchSize=" + branchSize + ", services=" + services + ", availableDesks=" + availableDesks + '}';
    }
    
}
