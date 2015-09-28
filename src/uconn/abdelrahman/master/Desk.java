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
import java.sql.Timestamp;
import java.util.Calendar;

/**
 *
 * @author Abdelrahman
 */
public class Desk implements Serializable{
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
