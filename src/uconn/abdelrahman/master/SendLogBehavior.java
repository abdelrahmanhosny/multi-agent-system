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
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Abdelrahman
 */
public class SendLogBehavior extends OneShotBehaviour{
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
            Logger.getLogger(RegisterBranchBehavior.class.getName()).log(Level.SEVERE, null, ex);
        }
        myAgent.send(msg);
    }    
}
