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

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author Abdelrahman
 */
public class ConfirmSendLogBehavior extends CyclicBehaviour{

    @Override
    public void action() {
        ACLMessage recommenderReply = myAgent.receive();
        if(recommenderReply != null){
            if(recommenderReply.getPerformative() == ACLMessage.CONFIRM){
                System.out.println(myAgent.getName() + ":branch -> successfully sent log");
                System.out.println(myAgent.getName() + ":branch -> recommender says: " + recommenderReply.getContent());
            }else {
                System.err.println(myAgent.getName() + ":branch -> recommender rejected the log sent!");
                System.err.println(myAgent.getName() + ":branch -> recommender says: " + recommenderReply.getContent());
            }
            myAgent.removeBehaviour(this);
        } else {
            block();
        }
    }
    
}
