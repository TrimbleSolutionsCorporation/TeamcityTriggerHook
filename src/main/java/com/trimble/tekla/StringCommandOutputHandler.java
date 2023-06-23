/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla;

import com.atlassian.bitbucket.scm.CommandOutputHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StringCommandOutputHandler implements CommandOutputHandler<String> {
    private StringBuilder output = new StringBuilder();

    @Override
    public String getOutput() {
        // Return the accumulated output
        return output.toString();
    }

    @Override
    public void setWatchdog(com.atlassian.bitbucket.scm.Watchdog wtchdg) {        
    }

    @Override
    public void process(InputStream in) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Process each line of the command output
                // Do something with the output line, such as storing it in a variable
                output.append(line).append("\n");
            }
        } catch (IOException e) {
            // Handle any exceptions that occur during reading
            e.printStackTrace();
        }
    }    
}
