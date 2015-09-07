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

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Abdelrahman
 */
enum ServiceType {DEPOSIT, WITHDRAW, BALANCE, CHECK, OPEN, TRANSFER, ATM, WIRE, LOAN};
class LogRecord {
    public int customerID;
    public ServiceType customerRequiredService;
    public Timestamp customerArrivalTime;
}
public class GenerateLogData {
    private static final Calendar calendar = Calendar.getInstance();
    private static int customerID;
    private static ServiceType customerRequiredService;
    private static Timestamp customerArrivalTime;
    
    //Delimiter used in CSV file
    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";
    private static final String FILE_HEADER = "CUSTOMER_ID,SERVICE_TYPE,ARRIVAL_TIME";
    
    private static LogRecord[] records;
    
    
    private static void writeToCSVFile(String fileName){
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(fileName);
            fileWriter.append(FILE_HEADER);
            fileWriter.append(NEW_LINE_SEPARATOR);
            
            for(int i = 0; i<records.length; i++){
                fileWriter.append(Integer.toString(records[i].customerID));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(records[i].customerRequiredService.toString());
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(records[i].customerArrivalTime.toString());
                fileWriter.append(NEW_LINE_SEPARATOR);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(GenerateLogData.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(GenerateLogData.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static ServiceType getRandomService(){
        Random randomGenerator = new Random();
        int randomService = randomGenerator.nextInt(9);
        switch(randomService){
            case 0: return ServiceType.ATM;
            case 1: return ServiceType.BALANCE;
            case 2: return ServiceType.CHECK;
            case 3: return ServiceType.DEPOSIT;
            case 4: return ServiceType.LOAN;
            case 5: return ServiceType.OPEN;
            case 6: return ServiceType.TRANSFER;
            case 7: return ServiceType.WIRE;
            case 8: return ServiceType.WITHDRAW;
        }
        return ServiceType.ATM;
    }
    public static void main(String[] args){
        calendar.set(2015, 5, 1, 9, 0, 0);
        customerArrivalTime = new Timestamp(calendar.getTimeInMillis());
        
        records = new LogRecord[800];
        
        for(customerID = 0; customerID < 800; customerID ++){
            LogRecord temp = new LogRecord();
            temp.customerID = customerID + 1;
            temp.customerRequiredService = getRandomService();
            temp.customerArrivalTime = customerArrivalTime;
            records[customerID] = temp;
            
            calendar.add(Calendar.SECOND, 36);
            customerArrivalTime = new Timestamp(calendar.getTimeInMillis());
        }
        writeToCSVFile("log-headquarter.csv");
    }
}
