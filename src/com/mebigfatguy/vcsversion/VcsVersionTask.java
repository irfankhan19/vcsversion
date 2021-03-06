/*
 * VcsVersion - an ant task for fetching revision, branch and dates of current view
 * Copyright 2013-2014 MeBigFatGuy.com
 * Copyright 2013-2014 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.vcsversion;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class VcsVersionTask extends Task {

    private enum VcsType {SVN, GIT, HG};
    
    private String vcs;
    private String revisionProp;
    private String branchProp;
    private String dateProp;
    
    public void setVcs(String versionControlSystem) {
        vcs = versionControlSystem;
    }
    
    public void setRevisionProperty(String revisionProperty) {
        revisionProp = revisionProperty;
    }
    
    public void setBranchProperty(String branchProperty) {
        branchProp = branchProperty;
    }
    
    public void setDateProperty(String dateProperty) {
        dateProp = dateProperty;
    }
    
    
    public void execute() {
        if (vcs == null) {
            throw new BuildException("Failed to provide ant property 'vcs'");
        }
        
        VcsType type = VcsType.valueOf(vcs.toUpperCase());
        
        switch (type) {
                
            case SVN:
                getSVNInfo();
                break;
                
            case GIT:
                getGITInfo();
                break;
                
            case HG:
                getHGInfo();
                break;
                
            default:
                throw new BuildException("Unknown vcs type: " + vcs);
        }        
    }
    
    private void getSVNInfo() {

        Pattern commitPattern = Pattern.compile("([^\\s]*)\\s+\\|.*", Pattern.CASE_INSENSITIVE);
        Pattern datePattern = Pattern.compile("[^\\|]+\\|[^\\|]+\\|\\s*([\\|]*).*", Pattern.CASE_INSENSITIVE);
        Pattern branchPattern = Pattern.compile("url:?.*/(.*)", Pattern.CASE_INSENSITIVE);

        try {
            Map<Pattern, String> vcsProps = new HashMap<Pattern, String>();
            if (revisionProp != null)
                vcsProps.put(commitPattern, revisionProp);
            if (dateProp != null)
                vcsProps.put(datePattern, dateProp);
            
            fetchInfo(vcsProps, "svn", "log", "-l", "1");
            
            vcsProps.clear();
            vcsProps.put(branchPattern, branchProp);
            
            fetchInfo(vcsProps, "svn", "info");
            
        } catch (Exception e) {
            throw new BuildException("Failed getting svn log info", e);
        }
    }
    
    private void getGITInfo() {

        Pattern commitPattern = Pattern.compile("commit:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Pattern datePattern = Pattern.compile("date:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Pattern branchPattern = Pattern.compile("\\*\\s*(.*)", Pattern.CASE_INSENSITIVE);

        try {
            Map<Pattern, String> vcsProps = new HashMap<Pattern, String>();
            if (revisionProp != null)
                vcsProps.put(commitPattern, revisionProp);
            if (dateProp != null)
                vcsProps.put(datePattern, dateProp);
            
            fetchInfo(vcsProps, "git", "log", "-n", "1");
            
            vcsProps.clear();
            vcsProps.put(branchPattern, branchProp);
            
            fetchInfo(vcsProps, "git", "branch");
            
        } catch (Exception e) {
            throw new BuildException("Failed getting git log info", e);
        }
    }

    private void getHGInfo() {
        
        Pattern changesetPattern = Pattern.compile("changeset:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Pattern datePattern = Pattern.compile("date:?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Pattern branchPattern = Pattern.compile("(.*)", Pattern.CASE_INSENSITIVE);

        try {
            Map<Pattern, String> vcsProps = new HashMap<Pattern, String>();
            if (revisionProp != null)
                vcsProps.put(changesetPattern, revisionProp);
            if (dateProp != null)
                vcsProps.put(datePattern, dateProp);
            
            fetchInfo(vcsProps, "hg", "log", "-l", "1");
            
            vcsProps.clear();
            vcsProps.put(branchPattern, branchProp);
            
            fetchInfo(vcsProps, "hg", "branch");
            
        } catch (Exception e) {
            throw new BuildException("Failed getting hg log info", e);
        }
    }
    
    private void fetchInfo(Map<Pattern, String> vcsProps, String... commandLine) throws IOException, InterruptedException {
        
        ProcessBuilder builder = new ProcessBuilder(commandLine);
        builder.directory(getProject().getBaseDir());
        Process p = builder.start();
        p.waitFor();
        
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            String line = br.readLine();
            while (line != null) {
                for (Map.Entry<Pattern, String> entry : vcsProps.entrySet()) {
                    Matcher m = entry.getKey().matcher(line);
                    if (m.matches()) {
                        getProject().setProperty(entry.getValue(), m.group(1).trim());
                    }
                }

                line = br.readLine();
            }
        } finally {
            closeQuietly(br);
        }
    }
    
    private void closeQuietly(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) { 
        }
    }
}
