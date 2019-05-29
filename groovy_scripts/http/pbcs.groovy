/*  
* @author  Hyperion Jedi (Ahmed Hafez)
* @version 1.0 
* @27 Dec 2017
* @Purpose: Basic PBCS Groovy program to demonstrate how Groovy can be used to interact with the user for entry and execute commands on PBCS. For the sake of this example program we are limiting the actions for user to list files, delete files, upload and download files to and from PBCS and execute Jobs in PBCS using Groovy commands.
* @Note: Some of the Code has been taken from the Oracle Groovy Function Helper and adjusted or re-written to work
*/ 


import javax.swing.*
import java.awt.*
import java.awt.event.*;
import org.json.JSONObject
import groovy.json.JsonSlurper
import javax.swing.JDialog;

public class Pbcs {

   
    static void main(String[] args) {
    
        //Define User Input object
        UserInputAction newInputAction = new UserInputAction();
        
        //call the object and action based on return value
        def userInputResult = newInputAction.captureDetails()
        if (userInputResult.equals("Success")) {
            if (newInputAction.taskSelection.equals("listfiles")) {
                 System.out.println("The Force Is With You, Time to list those files")
                 newInputAction.listFiles()
            }else if (newInputAction.taskSelection.equals("uploadfile")) {
                 System.out.println("The Force Is With You, Time to upload those files")
                 newInputAction.uploadFile(newInputAction.taskParam);
            }else if (newInputAction.taskSelection.equals("downloadfile")) {
                 System.out.println("The Force Is With You, Time to download those files")
                 newInputAction.downloadFile(newInputAction.taskParam);
            }else if (newInputAction.taskSelection.equals("deletefile")) {
                 System.out.println("The Force Is With You, Time to delete those files")
                 newInputAction.deleteFile(newInputAction.taskParam);
            } else if (newInputAction.taskSelection.equals("job")) {
                 System.out.println("The Force Is With You, Time to run those jobs")
                 newInputAction.executeJob(newInputAction.jobTypeSelection, newInputAction.jobNameSelection, newInputAction.taskParam);
            }
        } 
        else {
            System.out.println("Thanks for using Hyperion Jedi Groovy Sample!!! Code will now exit")
        }
        System.out.println("Thanks for using the Hyperion Jedi's Groovy Sample! Goodluck on your Groovy Journey Ahead...")
                  
    }
       
}

public class UserInputAction {
    
    //Define Variables
    def userCredentials
    def serverUrl
    def username
    def password
    def apiVersion
    def appName
    def varStatus
    def basicAuth
    def taskSelection
    def jobTypeSelection
    def jobNameSelection
    def taskParam
    def response
            
                 
    def captureDetails (def Result){
                     
                //Key Notes: We will need to display the prompts in Java Swing as the Groovy Console doesn't allow user entry through Console.
                //           Not sure if this is a bug or expected behaviour. Swing is much better suited for this task in any case.
             
                //Defining the J-Swing Fields            
                JTextField UserField = new JTextField(10);
                JTextField PasswordField = new JPasswordField(10);
                JTextField UrlField = new JTextField("https://planning--.pbcs..oraclecloud.com");
                JTextField DomainField = new JTextField(10);
                JTextField AppField = new JTextField(10);
                JLabel StatusLabel = new JLabel("");
                JLabel FileLabel = new JLabel("File Name");
                JLabel JobTypeLabel = new JLabel("Job Type");
                JLabel JobNameLabel = new JLabel("Job Name");
                JLabel JobParamLabel = new JLabel("Job Parameter");
                JRadioButton ListFiles = new JRadioButton("List Files in PBCS");
                JRadioButton UploadFile = new JRadioButton("Upload File To PBCS");
                JRadioButton DownloadFile = new JRadioButton("Download File From PBCS");
                JRadioButton DeleteFile = new JRadioButton("Delete Files in PBCS");
                JRadioButton Job = new JRadioButton("Run PBCS Job");
                JTextField JobType = new JTextField(20);
                JTextField JobName = new JTextField(20);
                JTextField JobParameter = new JTextField(40);             
                                
                //Defining the J-Swing Panel to Display on Dialog box
                JPanel DisplayPanel = new JPanel();
                JPanel LoginPanel = new JPanel();
                JPanel TaskTypePanel = new JPanel();
                JPanel TaskSelectionPanel = new JPanel();
                JPanel TaskDetailPanel = new JPanel();
                DisplayPanel.setLayout(new BorderLayout());
                
                //Defining the J-Swing Radio Options
                ButtonGroup group = new ButtonGroup();
                group.add(ListFiles);
                group.add(UploadFile);
                group.add(DownloadFile);
                group.add(DeleteFile);
                group.add(Job);

                //Take actions to control display based on Radio Option Selected
                ListFiles.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {         
                        StatusLabel.setText("ListFiles Option:" + (e.getStateChange()==1?"checked":"unchecked"));
                        TaskDetailPanel.remove(FileLabel);
                        TaskDetailPanel.remove(JobTypeLabel);                       
                        TaskDetailPanel.remove(JobNameLabel); 
                        TaskDetailPanel.remove(JobParamLabel);                          
                        TaskDetailPanel.remove(JobName)
                        TaskDetailPanel.remove(JobType)
                        TaskDetailPanel.remove(JobParameter)
                        taskSelection = "listfiles" 
                    }           
                });
                UploadFile.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {             
                        StatusLabel.setText("UploadFile Option: " + (e.getStateChange()==1?"checked":"unchecked"));                       
                        TaskDetailPanel.remove(FileLabel);                        
                        TaskDetailPanel.remove(JobName)
                        TaskDetailPanel.remove(JobType)
                        TaskDetailPanel.add(FileLabel); 
                        TaskDetailPanel.add(JobParameter)
                        taskSelection = "uploadfile" 
                    }           
                });
                DownloadFile.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {             
                        StatusLabel.setText("DownloadFile Option: " + (e.getStateChange()==1?"checked":"unchecked"));                     
                        TaskDetailPanel.remove(FileLabel);  
                        TaskDetailPanel.remove(JobTypeLabel);                       
                        TaskDetailPanel.remove(JobNameLabel); 
                        TaskDetailPanel.remove(JobParamLabel);                           
                        TaskDetailPanel.remove(JobName)
                        TaskDetailPanel.remove(JobType)
                        TaskDetailPanel.add(FileLabel); 
                        TaskDetailPanel.add(JobParameter)
                        taskSelection = "downloadfile"
                    }           
                });
                DeleteFile.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {             
                        StatusLabel.setText("DeleteFile Option: " + (e.getStateChange()==1?"checked":"unchecked"));                     
                        TaskDetailPanel.remove(FileLabel);   
                        TaskDetailPanel.remove(JobTypeLabel);                       
                        TaskDetailPanel.remove(JobNameLabel); 
                        TaskDetailPanel.remove(JobParamLabel);                     
                        TaskDetailPanel.remove(JobName)
                        TaskDetailPanel.remove(JobType)
                        TaskDetailPanel.add(FileLabel); 
                        TaskDetailPanel.add(JobParameter)
                        taskSelection = "deletefile"
                    }           
                });
                Job.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {             
                        StatusLabel.setText("Job Option: " + (e.getStateChange()==1?"checked":"unchecked"));
                        TaskDetailPanel.remove(FileLabel);
                        TaskDetailPanel.add(JobTypeLabel);                       
                        TaskDetailPanel.add(JobType)
                        TaskDetailPanel.add(JobNameLabel); 
                        TaskDetailPanel.add(JobName)
                        TaskDetailPanel.add(JobParamLabel);
                        TaskDetailPanel.add(JobParameter)
                        taskSelection = "job"
                    }           
                });
 
                
                //Adding the Fields and Labels to the Display Panel
                LoginPanel.add(new JLabel("User:"));
                LoginPanel.add(UserField);
                LoginPanel.add(new JLabel("Password:"));
                LoginPanel.add(PasswordField);
                LoginPanel.add(new JLabel("URL:"));
                LoginPanel.add(UrlField);
                LoginPanel.add(new JLabel("Domain:"));
                LoginPanel.add(DomainField);
                LoginPanel.add(new JLabel("PBCS Application Name:"));
                LoginPanel.add(AppField);
                TaskTypePanel.add(new JLabel("Select Action Type:"));
                TaskTypePanel.add(ListFiles)
                TaskTypePanel.add(UploadFile)
                TaskTypePanel.add(DownloadFile)
                TaskTypePanel.add(DeleteFile)
                TaskTypePanel.add(Job)
                TaskSelectionPanel.add(StatusLabel);
                
                //Controlling the size of the Display Panel
                DisplayPanel.setPreferredSize(new Dimension(1200, 100));
                
                //Displaying to contents on the Panel
                DisplayPanel.add(LoginPanel,"North");
                DisplayPanel.add(TaskTypePanel,"Center");
                DisplayPanel.add(TaskSelectionPanel,"West"); 
                DisplayPanel.add(TaskDetailPanel,"South"); 
                                             
                //Assign default variable value to blank to ensure correct user entry
                varStatus=""             
            
                while(varStatus!="Success") {
                    //Display the OptionPane with above Panel (fields and labels) for user entry of values and confirmation.
                    def result = JOptionPane.showConfirmDialog(null, DisplayPanel,"Hyperion Jedi PBCS Groovy Example", JOptionPane.OK_CANCEL_OPTION);
    
                    //OutPut the Results to be used to login                
                    if (result == JOptionPane.OK_OPTION && UserField.getText()!="") {
                        //Output User Entry
                        System.out.println("You Entered the Following PBCS Login Details")
                        System.out.println("User value: " + UserField.getText());
                        System.out.println("Password value: " + PasswordField.getText());
                        System.out.println("Url value: " + UrlField.getText());
                        System.out.println("Domain value: " + DomainField.getText());
                        System.out.println("PBCS Application: " + AppField.getText());
                        System.out.println("Proceeding to Process request to execute: " + taskSelection + " command on PBCS!!!")
                
                        //Assign variables from user input
                        serverUrl = UrlField.getText()
                        username = DomainField.getText() + "." + UserField.getText()
                        password = PasswordField.getText()
                        appName = AppField.getText()
                        jobTypeSelection = JobType.getText()
                        jobNameSelection = JobName.getText()
                        taskParam = JobParameter.getText()
                        
                        //Set the Credentials and the Authentication
                        userCredentials = username + ":" + password
                        basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userCredentials.getBytes())              
                                              
                        varStatus = "Success"
                                                   
                    } 
                    else if(result == JOptionPane.CANCEL_OPTION){
                        return "Cancelled"
                    } 
                    else {
                        //Output Message when not all required fields have been entered. This is basic error handling.
                        System.out.println("You have not entered input in all the required fields")
                        varStatus = "fail"
                    }
                }
                
                return varStatus
          }

    def fetchResponse(is) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line+"\n");
            }
            br.close();
            return sb.toString();
    }

    def fetchPingUrlFromResponse(response, relValue) {
            def object = new JsonSlurper().parseText(response)
            def pingUrlStr
            if (object.status == -1) {
                println "Started executing successfully"
                def links = object.links
                links.each{
                        if (it.rel.equals(relValue)) {
                                pingUrlStr=it.href
                        }
                }
            } else {
                println "Error details: " + object.detail
                //System.exit(0);
            }
            return pingUrlStr
    }

    def fetchJobStatusFromResponse(response) {
            def object = new JsonSlurper().parseText(response)
            def status = object.status
            if (status == -1)
                return "Processing"
            else if (status == 0)
                return "Completed"
            else
                return object.detail
    }

    def executeRequest(url, requestType, payload, contentType) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setRequestMethod(requestType);
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("Authorization", basicAuth);
            if (payload != null) {
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(payload);
                writer.flush();
            }

            int statusCode
            try {
                statusCode = connection.responseCode;
            } catch (all) {
                println "Error connecting to the URL"
                //System.exit(0);
            }

            def response
            if (statusCode == 200 || statusCode == 201) {
                if (connection.getContentType() != null && !connection.getContentType().startsWith("application/json")) {
                        println "Error occurred in server"
                        //System.exit(0)
                }
                InputStream is = connection.getInputStream();
                if (is != null)
                        response = fetchResponse(is)
            } else {
                println "Error occurred while executing request"
                println "Response error code : " + statusCode
                InputStream is = connection.getErrorStream();
                if (is != null && connection.getContentType() != null && connection.getContentType().startsWith("application/json")){
                        println fetchJobStatusFromResponse(fetchResponse(is))
                //System.exit(0);
                }
            }
            connection.disconnect();
            return response;
    }

    def getJobStatus(pingUrlString, methodType) {
        
            def pingUrl = new URL(pingUrlString);
            def completed = false;
            def pingResponse
            def status
            while (!completed) {
                pingResponse = executeRequest(pingUrl, methodType, null, "application/x-www-form-urlencoded");
                status = fetchJobStatusFromResponse(pingResponse);
                if (status == "Processing") {
                    try {
                        println "Please wait..."
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        completed = true
                    }
                } else {
                        println status
                        completed = true
                }
            }
    }

    def executeJob(jobType, jobName, parameters) {
            apiVersion="v3"
            def response
            def url = new URL(serverUrl + "/HyperionPlanning/rest/" + apiVersion + "/applications/" + appName + "/jobs");
            JSONObject payload = new JSONObject();
            try {
                if (parameters != null && !parameters.equals("")) {
                    JSONObject params = new JSONObject();
                    def args = parameters.split(';');
                    for (int i = 0; i < args.length; i++) {
                        if (args[i].indexOf("=") != -1) {
                            String[] param = args[i].split("=");
                            if (param[0].equalsIgnoreCase("clearData")) {
                                params.put("clearData",Boolean.valueOf(param[1]));
                            } else {
                                params.put(param[0],param[1]);
                            }
                        }
                    }
                    payload.put("jobName",jobName);
                    payload.put("jobType",jobType);
                    payload.put("parameters",params);
                }
                else {
                    payload.put("jobName",jobName);
                    payload.put("jobType",jobType);
                }
            } catch (MalformedURLException e) {
                println "Malformed URL. Please pass valid URL"
                //System.exit(0);
            }
            response = executeRequest(url, "POST", payload.toString(), "application/json");
            if (response != null) {
                getJobStatus(fetchPingUrlFromResponse(response, "self"), "GET");
            }
    }

    def listFiles() {
          
            def url;
            apiVersion="11.1.2.3.600"
            try {
                url = new URL(serverUrl + "/interop/rest/" + apiVersion + "/applicationsnapshots")
            } catch (MalformedURLException e) {
                println "Malformed URL. Please pass valid URL"
                ////System.exit(0);
            }
            response = executeRequest(url, "GET", null, "application/x-www-form-urlencoded");
            def object = new JsonSlurper().parseText(response)
            def status = object.status
            if (status == 0 ) {
                def items = object.items
                if (items == null) {
                        println "No files found"
                } else {
                    println "List of files :"
                    items.each{
                        println it.name
                    }
                }
            } else {
                println "Error occurred while listing files"
                if (object.detail != null){
                                println "Error details: " + object.detail
                }
            }
            
    }

    def deleteFile(filename) {
            def url;
            apiVersion="11.1.2.3.600"
            try {
                String encodedFileName = URLEncoder.encode(filename, "UTF-8");
                url = new URL(serverUrl + "/interop/rest/" + apiVersion + "/applicationsnapshots/" + encodedFileName)
            } catch (MalformedURLException e) {
                println "Malformed URL. Please pass valid URL"
                //System.exit(0);
            }
            response = executeRequest(url, "DELETE", null, "application/x-www-form-urlencoded");
            def object = new JsonSlurper().parseText(response)
            def status = object.status
            if (status == 0 ){
                println "File deleted successfully"
            } else {
                println "Error occurred while deleting file"
                if (object.detail != null) {
                   println "Error details: " + object.detail
                }
            }
    }

    def downloadFile(filename) {
        def url;
        apiVersion="11.1.2.3.600"
        try {
                String encodedFileName = URLEncoder.encode(filename, "UTF-8");
                url = new URL(serverUrl + "/interop/rest/" + apiVersion + "/applicationsnapshots/" + encodedFileName + "/contents");
        } catch (MalformedURLException e) {
                println "Malformed URL. Please pass valid URL"
                //System.exit(0);
        }
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", basicAuth);

        int statusCode
        def fileExt
        def saveFilePath
        try {
                statusCode = connection.responseCode;
        } catch (all) {
                println "Error connecting to the URL"
                //System.exit(0);
        }

        if (statusCode == 200) {
                InputStream is;
                if (connection.getContentType() != null && connection.getContentType().contains("application/json")) {
                        is = connection.getInputStream();
                        if (is != null) {
                                response = fetchResponse(is)
                                def object = new JsonSlurper().parseText(response)
                                println "Error occurred while downloading file"
                                if (object.details != null)
                                        println "Error details: " + object.details
                        }
                } else {
                        final int BUFFER_SIZE = 5 * 1024 * 1024;
                        fileExt = connection.getHeaderField("fileExtension");
                        //saveFilePath = "/u01/" + filename + "." + fileExt;
                        saveFilePath = filename;
                        File f = new File(saveFilePath);
                        is = connection.getInputStream();
                        FileOutputStream outputStream = new FileOutputStream(f);
                        int bytesRead = -1;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        while ((bytesRead = is.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                        }
                        println "Downloaded " + filename + " successfully";
                }
        } else {
                println "Error occurred while executing request"
                println "Response error code : " + statusCode
                InputStream is = connection.getErrorStream();
                if (is != null && connection.getContentType() != null && connection.getContentType().startsWith("application/json"))
                        println fetchJobStatusFromResponse(fetchResponse(is))
                //System.exit(0);
        }
        connection.disconnect();
    }
    
    def sendRequestToRest(isFirst, isLast, lastChunk,fileName) throws Exception {
            JSONObject params = new JSONObject();
            params.put("chunkSize", lastChunk.length);
            params.put("isFirst", isFirst);
            params.put("isLast", isLast);

            def url;
            try {
                url = new URL(serverUrl + "/interop/rest/" + apiVersion + "/applicationsnapshots/" + fileName + "/contents?q=" + params.toString());
            } catch (MalformedURLException e) {
                println "Malformed URL. Please pass valid URL"
                //System.exit(0);
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Authorization", basicAuth);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(lastChunk);
            wr.flush();

            boolean status = false
            int statusCode
            try {
                statusCode = connection.getResponseCode();
                if (statusCode == 200) {
                    InputStream is;
                    if (connection.getContentType() != null && connection.getContentType().contains("application/json")) {
                        is = connection.getInputStream();
                        if (is != null) {
                            response = fetchResponse(is)
                            def object = new JsonSlurper().parseText(response)
                            if (object.status == 0){
                                status = true;
                            } else if(object.status == -1 && isLast == true){
                                getJobStatus(fetchPingUrlFromResponse(response, "Job Status"), "GET");
                            } else {
                                println "Error occurred while uploading file"
                                if (object.detail != null){
                                    println "Error details: " + object.detail
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                println "Exception occurred while uploading file";
                //System.exit(0);
            } finally {
                if (connection != null) {
                        connection.disconnect();
                }
            }
            return status;
    }

    def uploadFile(fileName) {
            apiVersion="11.1.2.3.600"
            final int DEFAULT_CHUNK_SIZE = 50 * 1024 * 1024;
            int packetNo = 1;
            boolean status = true;
            byte[] lastChunk = null;
            println("Attempting to Upload File: "+ fileName)
            File f = new File(fileName);
            BufferedInputStream fis = null;
            long totalFileSize = f.length();
            boolean isLast = false;
            boolean isFirst = true;
            int lastPacketNo = (int) (Math.ceil(totalFileSize/ (double) DEFAULT_CHUNK_SIZE));
            long totalbytesRead = 0;
            try {
                fis = new BufferedInputStream(new FileInputStream(fileName));
                while (totalbytesRead < totalFileSize && status) {
                    int nextChunkSize = (int) Math.min(DEFAULT_CHUNK_SIZE, totalFileSize - totalbytesRead);
                    if (lastChunk == null) {
                        lastChunk = new byte[nextChunkSize];
                        int bytesRead = fis.read(lastChunk);
                        totalbytesRead += bytesRead;
                        if (packetNo == lastPacketNo) {
                            isLast = true;
                        }
                        status = sendRequestToRest(isFirst, isLast,lastChunk,fileName);
                        isFirst=false;
                        if (status) {
                            println "\r" + ((100 * totalbytesRead)/ totalFileSize) + "% completed";
                        } else {
                            break;
                        }
                        packetNo = packetNo + 1;
                        lastChunk = null;
                    }
                }
            } catch (Exception e) {
                println "Exception occurred while uploading file";
                //System.exit(0);
            } finally {
                if (null != fis) {
                        try {
                                fis.close();
                        } catch (IOException e) {
                            println "Error while closing input stream";
                            //System.exit(0);
                        }
                }
            }
    }
    
}