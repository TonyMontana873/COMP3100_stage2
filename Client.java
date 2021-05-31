import java.net.*;
import java.util.ArrayList;
import java.io.*;

public class Client {
    // global variables initialised for sending messages according to user guide
    private static String HELO = "HELO";
    private static String AUTH = "AUTH";
    private static String REDY = "REDY";
    private static String NONE = "NONE";
    private static String GETS = "GETS Capable";
    private static String OK = "OK";
    private static String SCHD = "SCHD";
    private static String QUIT = "QUIT";
    private static String JOBN = "JOBN";
    private static String JCPL = "JCPL";
    private static final String dot = ".";


    // global variables initialised for holding important shceduling data
    private static int smallestCS = 0;
    private static int smallestSID = 0;
    private static String smallestST = "";
    private static int jCore;
    private static int jMemory;
    private static int jDisk;
    private static int jbId;

    // global variable to hold messages sent from server
    private static String str = "";

    public static void main(String[] args) throws IOException, SocketException {
        run();
    }

    // function which initialises the socket so that a connection can be established
    // with client/server and messages can be sent between.
    // Calls core functions of stage 1 like the handshake as well contains main loop
    // for listening for jobs or other messages
    public static void run() throws IOException, SocketException {
        Socket s = new Socket("127.0.0.1", 50000);
        InputStreamReader in = new InputStreamReader(s.getInputStream());
        BufferedReader bf = new BufferedReader(in);
        PrintWriter pw = new PrintWriter(s.getOutputStream());

        handShake(pw, bf);

        getServer(s, pw, bf);

        while (!str.equals(NONE)) {

            readLineCatchUp(s, bf);

            jobStatus(pw, bf);

            nextJob(pw, bf);

            getsCapable(pw, bf);

            schedJob(pw, bf);

            if (str.equals(NONE)) {
                break;
            }
        }

        quit(s, pw, bf);

    }

    // this method is responsible for taking the ArrayList parameter looping through
    // it and splitting each String of the list into an array so that the indexes of
    // that array can be assigned to the attributes of the ServerInfo object.
    // the object is added to a seperate ArrayList where the smallest server is checked
    // on the coreCount of each ServerInfo object
    public static void createServerInfo(ArrayList<String> SLI) {
        ArrayList<ServerInfo> serverHold = new ArrayList<>();
        String[] SLIHold;
        for (int i = 0; i < SLI.size(); i++) {
            ServerInfo si = new ServerInfo();
            SLIHold = SLI.get(i).split("\\s+");
            si.type = SLIHold[0];
            si.id = Integer.parseInt(SLIHold[1]);
            
            si.coreCount = Integer.parseInt(SLIHold[4]);
            si.memory = Integer.parseInt(SLIHold[5]);
            si.disk = Integer.parseInt(SLIHold[6]);
            serverHold.add(si);
        }
        smallestCS = serverHold.get(serverHold.size()-1).coreCount;
        smallestSID = serverHold.get(serverHold.size()-1).id;
        smallestST = serverHold.get(serverHold.size()-1).type;
        for (int i = serverHold.size()-1 ; i > 0; i--) {
            if (serverHold.get(i).coreCount < smallestCS) {
                smallestCS = serverHold.get(i).coreCount;
                smallestSID = serverHold.get(i).id;
                smallestST = serverHold.get(i).type;
            }
        }

    }

    // this function is responsible for sending the initial GETS Capable message to get
    // server information and add it into an ArrayList
    public static void getServer(Socket s, PrintWriter pw, BufferedReader bf) {
        String [] Data;
        String reply = "";
        ArrayList<String> SLI = new ArrayList<>();
        
        try {
            if(str.contains(JOBN)){
                String[] hold = str.split("\\s+");
                jCore = Integer.parseInt(hold[4]);
                jMemory = Integer.parseInt(hold[5]);
                jDisk = Integer.parseInt(hold[6]);
                
            }
            pw.println(GETS + " " + jCore + " " + jMemory + " " +jDisk);
            pw.flush();
            reply = bf.readLine();
            Data = reply.split("\\s+");
            pw.println(OK);
            pw.flush();
            Integer Server = Integer.parseInt(Data[1]);
            for(int i = 0; i < Server; i++){
                reply = bf.readLine();
                
                if (!reply.equals(dot)) {
                    SLI.add(reply);
                }
            }
            pw.println(OK);
            pw.flush();
            createServerInfo(SLI);
            
        } catch (Exception e) {
            System.out.println("Error: ArrayList invalid");
            e.printStackTrace();
        }
    }

    // a delay on the readLine created from GETS is fixed through this function
    // cathcing up readLine to the current message
    public static void readLineCatchUp(Socket s, BufferedReader bf) {
        try {
            while (str.equals(dot) || str.equals("")) {
                str = bf.readLine();
            }
        } catch (IOException e) {
            System.out.println("Error: readLineCatchUp invalid");
            e.printStackTrace();
        }

    }

    // intial handshake between client-server where client is authenticated before
    // proceeding to job scheduling
    public static void handShake(PrintWriter pw, BufferedReader bf) {
        String userName = System.getProperty("user.name");

        try {
            pw.println((HELO));
            pw.flush();
            str = bf.readLine();
            pw.println(AUTH + " " + userName);
            pw.flush();
            str = bf.readLine();
            pw.println(REDY);
            pw.flush();

            str = bf.readLine();
        } catch (IOException e) {
            System.out.println("Error: handshake invalid");
            e.printStackTrace();
        }

    }

    // JCPL messages will be sent to client which highlight the status of the jobs
    // that have been scheduled and need a REDY reponse from the client
    public static void jobStatus(PrintWriter pw, BufferedReader bf) {
        try {
            if (str.contains(JCPL)) {
                pw.println(REDY);
                pw.flush();
                str = bf.readLine();
            }
        } catch (IOException e) {
            System.out.println("Error: jobStatus invalid");
            e.printStackTrace();
        }
    }

    // the scheduling of jobs by splitting the JOBN string and taking the job ID to
    // use in the schedule message along with biggestST and biggestSID
    public static void schedJob(PrintWriter pw, BufferedReader bf) {
        
        try {
            if (str.contains(JOBN) && !str.contains(".")) {
                String[] hold = str.split("\\s+");
                jbId = Integer.parseInt(hold[2]);
                pw.println(SCHD + " " + jbId + " " + smallestST + " " + smallestSID);
                pw.flush();
                str = bf.readLine();
            }
        } catch (IOException e) {
            System.out.println("Error: schedJob invalid");
            e.printStackTrace();
        }

    }

    // when all job information has been sent a NONE message from the server will be
    // sent indicating its time to end the simulation and close the socket
    public static void quit(Socket s, PrintWriter pw, BufferedReader bf) {
        try {
            if (str.equals(NONE)) {
                pw.println(QUIT);
                pw.flush();
                str = bf.readLine();
                System.out.println("server : " + str);
                if (str.equals(QUIT)) {
                    s.close();
                }
            }
        } catch (IOException e) {
            System.out.println("Error: quit invalid");
            e.printStackTrace();
        }
    }

    // gets the next job from the server
    public static void nextJob(PrintWriter pw, BufferedReader bf) {
        try {
            if (str.equals(OK)) {
                pw.println(REDY);
                pw.flush();
                str = bf.readLine();
                
                String[] hold = str.split("\\s+");
                jbId = Integer.parseInt(hold[2]);
                
            }
        } catch (IOException e) {
            System.out.println("Error: nextJob invalid");
            e.printStackTrace();
        }
    }

    // essentially the same as getServer, however this executes in the main
    // while loop to get servers for every job
    public static void getsCapable(PrintWriter pw, BufferedReader bf){    
        String[] Data;
        String reply = "";
        ArrayList<String> SLI = new ArrayList<>();
        try {
            if(str.contains(JOBN) && jbId != 0){
                
                String[] hold = str.split("\\s+");
                jCore = Integer.parseInt(hold[4]);
                jMemory = Integer.parseInt(hold[5]);
                jDisk = Integer.parseInt(hold[6]);
            
            pw.println(GETS + " " + jCore + " " + jMemory + " " +jDisk);
            pw.flush();
            reply = bf.readLine();
            Data = reply.split("\\s+");
            Integer Servers = Integer.parseInt(Data[1]);
            pw.println(OK);
            pw.flush();
            for(int i = 0; i < Servers; i++){
                reply = bf.readLine();
                if (!reply.equals(dot)) {
                    SLI.add(reply);
                }
            }
            pw.println(OK);
            pw.flush();
            createServerInfo(SLI);
        }
        } catch (Exception e) {
            System.out.println("Error: ArrayList invalid");
            e.printStackTrace();
        }
    }

}
