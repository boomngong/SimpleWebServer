/* CS-361, Rogelio Zamudio Jr. */

import java.io.*;
import java.net.*;
import java.util.*;

public class SimpleWebServer {
  /* Run the HTTP server on this TCP port. */
  private static final int PORT = 8080;

  /* The socket used to process incoming connections
     from web clients */
  private static ServerSocket dServerSocket;

  /* Declare new file name for log file, to ensure
     writing to a different file */
  private String logFile = "logFile.txt";
  private String errorLog = "errorLog.txt";

  /* Declare size limit in byte size to compare against
     incoming files, sizes are in bytes */
  private final long SIZE_LIMIT = 1000;
  private final long MAX_DOWNLOAD_LIMIT = 5000;

  /* Hardcoded values for username and password,
     which would be easy to guess */
  private String username = "admin";
  private String password = "password";

  public SimpleWebServer() throws Exception {
    dServerSocket = new ServerSocket (PORT);
  }

  public void run() throws Exception {
    while(true) {
      /* wait for a connection from a client */
      Socket s = dServerSocket.accept();

      /* then process the client's request */
      processRequest(s);
    }
  }

  /* Reads the HTTP request from the client, and
     responds with the file the user requested or
     a HTTP error code. */
  public void processRequest(Socket s) throws Exception {
      /* used to read the data from the client */
      BufferedReader br =
        new BufferedReader(new InputStreamReader(s.getInputStream()));

      /* used to write data to the client */
      OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());

      /* read the HTTP request from the client */
      String request = br.readLine();

      String command = null;
      String pathname = null;
      String filename = null;
      /* parse the HTTP request */
      StringTokenizer st = new StringTokenizer(request, " ");

      command = st.nextToken();
      pathname = st.nextToken();

      if (command.equals("GET")) {
          /* if the request is a GET
             try to respond with the file
             the user is requesting */
           serveFile(osw, pathname);
           osw.write("\r\n");
           logEntry(logFile, command + " " + pathname);
           osw.close();
      } else if (command.equals("PUT")) {
         /* if the request is a PUT
            try to store the file
            and log the entry */

        storeFile(br, osw, pathname);
        /* place the log in log file */
        osw.write("\r\n");
        logEntry(logFile, command + " " + pathname);
        osw.close();
      } else {
        /* if the request is NOT a GET or a PUT
           return an error saying this server
           does not implement the requested command */
        /* Update this section with variables, turn
           HTTP errors into List of Strings */
         osw.write("HTTP/1.0 501 Not Implemented\n\n");
         logEntry(logFile, "HTTP/1.0 501 Not Implemented\n\n");
         osw.close();
      }


      /* close the connection to the client */
      osw.close();
    }

    public void serveFile(OutputStreamWriter osw, String pathname)
    throws Exception {
      FileReader fr = null;
      int c = -1;
      int sentBytes = 0;
      StringBuffer sb = new StringBuffer();

      /* remove the initial slash at the beginning
         of the pathname in the request */
      if (pathname.charAt(0) == '/')
         pathname = pathname.substring(1);

      /* if there was no filename specified by the
         client, serve the "index.html" file */
      if (pathname.equals(""))
         pathname = "index.html";

      /* check the file size, if it doesn't contrain
         to limits , log and serve HTTP error */
      if (!checkFileSize(pathname)) {
        osw.write("HTTP/1.0 403 Forbidden\n\n");
        logEntry(errorLog, "File requested too large to serve");
        return;
      }
      /* try to open the file specified by pathname */
      try {
        fr = new FileReader(checkPath(pathname));
        c = fr.read();
      } catch (Exception e) {
        /* if the file not found, return the
           appropriate HTTP response code */
           osw.write("HTTP/1.0 404 Not Found\n\n");
           return;
      }

      /* if the requested file can be successfully opened
         and read, then return an OK response code and
         send the contents of the file */
      osw.write("HTTP/1.0 200 OK\n\n");
      while ((c != -1) && (sentBytes < MAX_DOWNLOAD_LIMIT)) {
          osw.write(c);
          sentBytes++;
          c = fr.read();
      }
      osw.write(sb.toString());
    }

    public void storeFile(BufferedReader br, OutputStreamWriter osw,
                          String pathname) throws Exception {
      FileWriter fw = null;
      try {
        fw = new FileWriter(pathname);
        String s = br.readLine();
        while (s != null) {
          fw.write(s);
          s = br.readLine();
        }
        fw.close();
        osw.write("HTTP/1.0 201 Created");
      } catch(Exception e) {
        osw.write("HTTP/1.0 500 Internal Server Error");
      }
    }

    public void logEntry(String filename, String record) throws Exception {
      try (FileWriter fw = new FileWriter(filename, true)) {
          fw.write(getTimeStamp() + " " + record);
      }
    }

    public String getTimeStamp() {
      return (new Date().toString());
    }

    private String checkPath(String pathname) throws Exception {
      File target = new File(pathname);
      File cwd = new File(System.getProperty("user.dir"));
      File absoluteTarget = new File(pathname);
      String abslstStr = absoluteTarget.getAbsolutePath();
      String targetStr = target.getCanonicalPath();
      String cwdStr = cwd.getCanonicalPath();
      logEntry(logFile, abslstStr);
      logEntry(logFile, targetStr);
      if (!targetStr.startsWith(cwdStr))
        throw new Exception("File Not Found");
      else
        return targetStr;
    }

    /* This method is called to compare the size of the
       requested file, return true if the file is smaller
       than the limit, false otherwise. */
    private boolean checkFileSize(String pathname) throws IOException {
      File file = new File(pathname);
      return file.length() <= SIZE_LIMIT;
    }
    
    /* This method is called when the program is run from
       the command line. */
    public static void main(String args[]) throws Exception {

        SimpleWebServer sws = new SimpleWebServer();
        sws.run();
    }
}
