import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Scanner;

public class SchedulingSimulator {
    private static enum Mode {
        INPUT, FILE, INVALID;

        public static Mode fromString (String value) {
            if (value.equalsIgnoreCase ("i") || value.equalsIgnoreCase ("input")) {
                return INPUT;
            }
            if (value.equalsIgnoreCase ("f") || value.equalsIgnoreCase ("file")) {
                return FILE;
            }
            return INVALID;
        }
    }

    private static final byte FCFS = 1, SPN = 2, SRT = 3, RR = 4, PRIO = 5, ALL = 6;
    private static final String[] algorithmNames = {
        "First-Come-First-Serve (FCFS)",
        "Shortest Process Next (SPN)",
        "Shortest Remaining Time (SRT)",
        "Round Robin (RR)",
        "Priority (Non-Preemptive)"
    };
    public static void main (String[] args) throws Exception {
        System.out.println ("CPU Scheduling Algorithm Simulator");

        boolean doAnotherSim = true;
        ArrayList<Process> processes = new ArrayList<> ();
        ArrayList<GanttChart> charts = new ArrayList<> ();
        Scanner fromKey = new Scanner (System.in);
        Mode mode;

        while (doAnotherSim) {
            for (int i = 0; i < 5; i++) {
                charts.add (null);
            }

            mode = Mode.INVALID;

            System.out.println ("Select mode - \"i\" for user input, \"f\" for file");

            while (mode == Mode.INVALID) {
                System.out.print ("Enter \"i\" or \"f\": ");
                mode = Mode.fromString (fromKey.nextLine());
            }

            if (mode == Mode.INPUT) {
                inputProcessesFromKey (processes, fromKey);
            } else {
                System.out.print ("Enter file name: ");
                String fileName = fromKey.nextLine ();
                inputProcessesFromFile (processes, new Scanner (new File (fileName)));
            }

            sortProcesses (processes, 0, processes.size () - 1);
            
            byte selection;

            while (doAnotherSim) {
                System.out.println ("\nSelect Scheduling Algorithm:");
                for (int i = 0; i < 5; i++) {
                    System.out.println ((i + 1) + ". " + algorithmNames[i]);
                }
                System.out.println ("6. All");

                System.out.print ("Enter your choice (1-6): ");
                selection = Byte.parseByte (fromKey.nextLine ());

                if (selection < 1 || selection > 6) {
                    throw new IllegalArgumentException ("Number provided not within range (1-6)");
                }

                executeAlgorithm (processes, charts, selection, fromKey);

                System.out.println();
                
                doAnotherSim = yesNoPrompt ("Run another simulation with the same processes?", fromKey);
            }

            if (mode == Mode.INPUT && yesNoPrompt ("Save inputted processes as a text file?", fromKey)) {
                PrintStream toFile = createFileWriter (fromKey);
                sortProcessesByID (processes, 0, processes.size () - 1);
                toFile.println (processes.size ());

                ListIterator<Process> it = processes.listIterator ();
                Process p;

                while (it.hasNext ()) {
                    p = it.next ();
                    toFile.print (p.getArrivalTime () + " ");
                    toFile.print (p.getCPUBurst () + " ");
                    toFile.print (p.getPriority ());
                    if (it.hasNext ()) {
                        toFile.println ();
                    }
                }
            }

            if (yesNoPrompt ("Save simulation results as a text file?", fromKey)) {
                PrintStream toFile = createFileWriter (fromKey);
                toFile.println ("Simulation Results");
                toFile.println ();
                for (int i = 0; i < 5; i++) {
                    if (charts.get (i) != null) {
                        toFile.print (algorithmNames[i]);
                        if (i == 3) {
                            toFile.printf (" (quantum = %d%s", charts.get (i).getQuantum (), ")");
                        }
                        toFile.println ();
                        printAnalytics (charts.get (i), processes, toFile);
                        toFile.println ();

                        if (i == 3) {
                            for (int j = 5; j < charts.size (); j++) {
                                toFile.printf ("Round Robin (RR) (quantum = %d%s", charts.get (j).getQuantum (), ")");
                                toFile.println ();
                                printAnalytics (charts.get (i), processes, toFile);
                                toFile.println ();
                            }
                        }
                    }
                }
            }

            doAnotherSim = yesNoPrompt ("Simulate another set of processes?", fromKey);
            processes.clear ();
            charts.clear ();
        }
        fromKey.close ();
    }

    private static void inputProcessesFromKey (ArrayList<Process> processes, Scanner fromKey) {
        System.out.print ("Enter number of processes: ");
        final int numProcesses = Integer.parseInt (fromKey.nextLine ());

        if (numProcesses <= 0) {
            throw new IllegalArgumentException ("Cannot have zero or negative processes");
        }

        System.out.println ("\nEnter the information for each process below:\n");
        int arrival, burst, priority;

        for (int i = 0; i < numProcesses; i++) {
            System.out.printf ("Process %d%s", i + 1, ":\n");
            System.out.print ("Arrival Time: ");
            arrival = Integer.parseInt (fromKey.nextLine ());
            if (arrival < 0) {
                throw new IllegalArgumentException ("Cannot have negative arrival time");
            }

            System.out.print ("Burst Time: ");
            burst = Integer.parseInt (fromKey.nextLine ());

            if (burst <= 0) {
                throw new IllegalArgumentException ("Cannot have zero or negative burst time");
            }

            System.out.print("Priority: ");
            priority = Integer.parseInt(fromKey.nextLine ());

            if (priority < 0) {
                throw new IllegalArgumentException ("Cannot have negative priority");
            }

            processes.add (new Process (i + 1, arrival, burst, priority));
            System.out.println ();
        }
    }

    private static void inputProcessesFromFile (ArrayList<Process> processes, Scanner fromFile) throws ParseException {
        System.out.print ("Enter number of processes: ");
        final int numProcesses = Integer.parseInt (fromFile.nextLine ());
        System.out.println (numProcesses);

        if (numProcesses <= 0) {
            throw new IllegalArgumentException ("Cannot have zero or negative processes");
        }

        System.out.println ("\nEnter the information for each process below:\n");

        String[] temp; // 
        int[] params = new int[3];

        for (int i = 0; i < numProcesses; i++) {
            System.out.printf ("Process %d%s", i + 1, ":\n");
            temp = fromFile.nextLine ().split (" ");

            if (temp.length != 3) {
                int errorOffset = 0, spaces = 3;
                
                for (String s: temp) {
                    if (--spaces >= 0) {
                        errorOffset += s.length () + 1;
                    }
                }

                throw new ParseException ("File must provide exactly three numbers per line", errorOffset);
            }

            for (int j = 0; j < 3; j++) {
                params[j] = Integer.parseInt (temp[j]);
            }

            System.out.println ("Arrival Time: " + params[0]);

            if (params[0] < 0) {
                throw new IllegalArgumentException ("Cannot have negative arrival time");
            }

            System.out.println ("Burst Time: " + params[1]);

            if (params[1] <= 0) {
                throw new IllegalArgumentException ("Cannot have zero or negative burst time");
            }

            System.out.println ("Priority: " + params[2]);

            if (params[2] < 0) {
                throw new IllegalArgumentException ("Cannot have negative priority");
            }

            processes.add (new Process (i + 1, params[0], params[1], params[2]));
            System.out.println ();
        }
    }

    private static PrintStream createFileWriter (Scanner fromKey) throws IOException {
        boolean makeFile = false;
        File file;
        PrintStream toFile = null;

        boolean cont = false;
        String yn;

        while (!makeFile) {
            System.out.print ("Enter file name: ");
            file = new File (fromKey.nextLine ());

            if (file.exists ()) {
                System.out.println ("File of name " + file.getName () + " already exists. Overwrite? (y/n)");
                
                while (!cont) {
                    System.out.print ("Enter \"y\" or \"n\": ");
                    yn = fromKey.nextLine ();

                    if (yn.equalsIgnoreCase ("y") || yn.equalsIgnoreCase ("yes")) {
                        cont = true;
                        makeFile = true;
                    } else if (yn.equalsIgnoreCase ("n") || yn.equalsIgnoreCase ("no")) {
                        cont = true;
                    }
                }
            } else {
                makeFile = true;
            }

            if (makeFile) {
                toFile = new PrintStream (file);
            }
        }

        return toFile;
    }

    private static boolean yesNoPrompt (String prompt, Scanner fromKey) {
        boolean cont = false;
        boolean selection = false;
        String yn;
        System.out.println (prompt + " (y/n)");

        while (!cont) {
            System.out.print ("Enter \"y\" or \"n\": ");
            yn = fromKey.nextLine ();

            if (yn.equalsIgnoreCase ("y") || yn.equalsIgnoreCase ("yes")) {
                cont = true;
                selection = true;
            } else if (yn.equalsIgnoreCase ("n") || yn.equalsIgnoreCase ("no")) {
                cont = true;
            }
        }
        
        return selection;
    }

    private static void sortProcesses (ArrayList<Process> processes, int low, int high) {
        if (low < high) {
            int pi = partition (processes, low, high);
            sortProcesses (processes, low, pi - 1);
            sortProcesses (processes, pi + 1, high);
        }
    }

    private static int partition (ArrayList<Process> processes, int low, int high) {
        Process pivot = processes.get (high);
        int i = low;
        Process temp;

        for (int j = low; j < high; j++) {
            temp = processes.get (j);
            if (temp.compareTo (pivot) < 1) {
                processes.set (j, processes.get(i));
                processes.set (i, temp);
                i++;
            }
        }

        processes.set (high, processes.get (i));
        processes.set (i, pivot);

        return i;
    }

    private static void sortProcessesByID (ArrayList<Process> processes, int low, int high) {
        if (low < high) {
            int pi = partitionByID (processes, low, high);
            sortProcesses (processes, low, pi - 1);
            sortProcesses (processes, pi + 1, high);
        }
    }

    private static int partitionByID (ArrayList<Process> processes, int low, int high) {
        Process pivot = processes.get (high);
        int i = low;
        Process temp;

        for (int j = low; j < high; j++) {
            temp = processes.get (j);
            if (temp.getID () < pivot.getID ()) {
                processes.set (j, processes.get(i));
                processes.set (i, temp);
                i++;
            }
        }

        processes.set (high, processes.get (i));
        processes.set (i, pivot);

        return i;
    }

    private static void executeAlgorithm (ArrayList<Process> processes, ArrayList<GanttChart> charts, byte algorithm, Scanner fromKey) {
        System.out.println();
        boolean runSim = false;
        GanttChart temp = null;

        if (algorithm > 0 && algorithm < ALL && (charts.get (algorithm - 1) == null || algorithm == RR) ) {
            runSim = true;

            switch (algorithm) {
                case FCFS -> charts.set (FCFS - 1, fcfs (queueProcesses (processes)));
                case SPN -> charts.set (SPN - 1, spn (queueProcesses (processes)));
                case SRT -> charts.set (SRT - 1, srt (queueProcesses (processes)));
                case RR -> {
                    temp = rr (queueProcesses (processes), fromKey); 
                    if (charts.get (RR - 1) == null) {
                        charts.set (RR - 1, temp);
                    } else if (temp.getQuantum () != charts.get (RR - 1).getQuantum ()) {
                        boolean placedChart = false;
                        int i = 5;
                        while (!placedChart) {
                            if (i == charts.size ()) {
                                charts.add (temp);
                                placedChart = true;
                            } else if (temp.getQuantum () != charts.get (i).getQuantum ()) {
                                placedChart = true;
                            }
                        }
                    }
                }
                case PRIO -> charts.set (PRIO - 1, priority (queueProcesses (processes)));
            }
        } else if (algorithm == ALL) {
            System.out.println ("Simulating algorithms in order...");

            for (byte i = 1; i <= 5; i++) {
                executeAlgorithm(processes, charts, i, fromKey);
            }
        }
        
        if (algorithm > 0 && algorithm < ALL) {
            if (!runSim) {
                System.out.print (algorithmNames[algorithm - 1]);
            }

            if (algorithm != RR) {
                printAnalytics (charts.get (algorithm - 1), processes, System.out);
            } else {
                printAnalytics (temp, processes, System.out);
            }
        }
    }

    private static Queue<Process> queueProcesses (ArrayList<Process> processes) {
        Queue<Process> processQueue = new LinkedList<> ();

        for (Process p: processes) {
            p.resetProcess ();
            processQueue.add (p);
        }

        return processQueue;
    }

    private static GanttChart fcfs (Queue<Process> processes) {
        System.out.println ("Simulating FCFS algorithm...");
        Process runningProcess = null;
        Queue<Process> processQueue = new LinkedList<> ();
        GanttChart chart = new GanttChart (0);

        int tick = 0;

        while (runningProcess != null || !processes.isEmpty () || !processQueue.isEmpty ()) {
            while (!processes.isEmpty () && processes.peek ().getArrivalTime () == tick) {
                processQueue.add (processes.poll ());
            }

            if (runningProcess == null && !processQueue.isEmpty ()) {
                runningProcess = processQueue.poll ();
                runningProcess.receiveResponse (tick);
                chart.addEntry (runningProcess.getID (), tick);
            }

            tick++;

            if (runningProcess != null) {
                runningProcess.executeTick(tick);

                if (runningProcess.isFinished()) {
                    runningProcess = null;

                    if (processes.isEmpty() && processQueue.isEmpty()) {
                        chart.addEntry (-1, tick);
                    }
                }
            }
        }

        return chart;
    }

    private static GanttChart spn (Queue<Process> processes) {
        System.out.println ("Simulating SPN algorithm...");
        Process runningProcess = null;
        LinkedList<Process> processQueue = new LinkedList<> ();
        GanttChart chart = new GanttChart (0);

        int tick = 0;

        int i = 0;
        Process temp;
        ListIterator<Process> it;

        while (runningProcess != null || !processes.isEmpty () || !processQueue.isEmpty ()) {
            while (!processes.isEmpty () && processes.peek ().getArrivalTime () == tick) {
                temp = processes.poll ();
                it = processQueue.listIterator ();

                while (it.hasNext () && temp.cpuBurst >= it.next ().cpuBurst) {
                    i++;
                }

                it = processQueue.listIterator ();

                while (i > 0) {
                    it.next ();
                    i--;
                }

                it.add (temp);
            }

            if (runningProcess == null && !processQueue.isEmpty ()) {
                runningProcess = processQueue.poll ();
                runningProcess.receiveResponse (tick);
                chart.addEntry (runningProcess.getID (), tick);
            }

            tick++;

            if (runningProcess != null) {
                runningProcess.executeTick(tick);

                if (runningProcess.isFinished()) {
                    runningProcess = null;

                    if (processes.isEmpty() && processQueue.isEmpty()) {
                        chart.addEntry (-1, tick);
                    }
                }
            }
        }

        return chart;
    }

    private static GanttChart srt (Queue<Process> processes) {
        System.out.println ("Simulating SRT algorithm...");
        Process runningProcess = null;
        LinkedList<Process> processQueue = new LinkedList<> ();
        GanttChart chart = new GanttChart (0);

        int tick = 0;

        int i = 0;
        Process temp;
        ListIterator<Process> it;

        while (runningProcess != null || !processes.isEmpty () || !processQueue.isEmpty ()) {
            while (!processes.isEmpty () && processes.peek ().getArrivalTime () == tick) {
                temp = processes.poll ();
                if (runningProcess != null && temp.getRemainingBurst () < runningProcess.getRemainingBurst ()) {
                    processQueue.add (0, runningProcess);
                    runningProcess = temp;
                    chart.addEntry (temp.getID (), tick);
                } else {
                    it = processQueue.listIterator ();

                    while (it.hasNext () && temp.getRemainingBurst () >= it.next ().getRemainingBurst ()) {
                        i++;
                    }

                    it = processQueue.listIterator ();

                    while (i > 0) {
                        it.next ();
                        i--;
                    }

                    it.add (temp);
                }
            }

            if (runningProcess == null && !processQueue.isEmpty ()) {
                runningProcess = processQueue.poll ();
                runningProcess.receiveResponse (tick);
                chart.addEntry (runningProcess.getID (), tick);
            }

            tick++;

            if (runningProcess != null) {
                runningProcess.executeTick(tick);

                if (runningProcess.isFinished()) {
                    runningProcess = null;

                    if (processes.isEmpty() && processQueue.isEmpty()) {
                        chart.addEntry (-1, tick);
                    }
                }
            }
        }

        return chart;
    }

    private static GanttChart rr (Queue<Process> processes, Scanner fromKey) {
        System.out.print ("Enter time quantum for RR algorithm: ");

        final int quantum = Integer.parseInt (fromKey.nextLine ());
        int q = 0;

        if (quantum < 1) {
            throw new IllegalArgumentException ("Time quantum cannot be zero or negative");
        }

        System.out.printf ("Simulating RR algorithm (quantum = %d%s", quantum, ")...");
        System.out.println ();
        Process runningProcess = null;
        Queue<Process> processQueue = new LinkedList<> ();
        GanttChart chart = new GanttChart (quantum);

        int tick = 0;

        while (runningProcess != null || !processes.isEmpty () || !processQueue.isEmpty ()) {
            while (!processes.isEmpty () && processes.peek ().getArrivalTime () == tick) {
                processQueue.add (processes.poll ());
            }

            if (q == quantum) {
                if (!processQueue.isEmpty ()) {
                    if (runningProcess != null) {
                        processQueue.add (runningProcess);
                    }

                    runningProcess = processQueue.poll ();
                    chart.addEntry (runningProcess.getID (), tick);
                    q = 0;
                }
            }

            if (runningProcess == null && !processQueue.isEmpty ()) {
                runningProcess = processQueue.poll ();
                runningProcess.receiveResponse (tick);
                chart.addEntry (runningProcess.getID (), tick);
                q = 0;
            }

            tick++;
            q++;

            if (runningProcess != null) {
                runningProcess.executeTick(tick);

                if (runningProcess.isFinished()) {
                    runningProcess = null;

                    if (processes.isEmpty() && processQueue.isEmpty()) {
                        chart.addEntry (-1, tick);
                    }
                }
            }
        }

        return chart;
    }

    private static GanttChart priority (Queue<Process> processes) {
        System.out.println ("Simulating priority (non-preemtive) algorithm...");
        Process runningProcess = null;
        LinkedList<Process> processQueue = new LinkedList<> ();
        GanttChart chart = new GanttChart (0);

        int tick = 0;

        int i = 0;
        Process temp;
        ListIterator<Process> it;

        while (runningProcess != null || !processes.isEmpty () || !processQueue.isEmpty ()) {
            while (!processes.isEmpty () && processes.peek ().getArrivalTime () == tick) {
                temp = processes.poll ();
                it = processQueue.listIterator ();

                while (it.hasNext () && temp.getPriority () >= it.next ().getPriority ()) {
                    i++;
                }

                it = processQueue.listIterator ();

                while (i > 0) {
                    it.next ();
                    i--;
                }

                it.add (temp);
            }

            if (runningProcess == null && !processQueue.isEmpty ()) {
                runningProcess = processQueue.poll ();
                runningProcess.receiveResponse (tick);
                chart.addEntry (runningProcess.getID (), tick);
            }

            tick++;

            if (runningProcess != null) {
                runningProcess.executeTick(tick);
                if (runningProcess.isFinished()) {
                    runningProcess = null;
                    if (processes.isEmpty() && processQueue.isEmpty()) {
                        chart.addEntry (-1, tick);
                    }
                }
            }
        }

        return chart;
    }

    private static void printAnalytics (GanttChart chart, ArrayList<Process> processes, PrintStream output) {
        output.println ("Gantt Chart Visualization:");
        chart.printChart (output);

        output.println ();

        int wait = 0;
        int turnaround = 0;
        int response = 0;
        float nTurnaround = 0;
        final float numProcesses = processes.size ();

        int tempTurnaround;

        for (Process p: processes) {
            wait += p.getWaitTime ();
            turnaround += p.getTurnaroundTime ();
            nTurnaround += p.getNormalizedTurnaroundTime ();
            response += p.getResponseTime ();
        }

        output.println ("Perfomance Metrics:");
        output.printf ("Average Waiting Time: %.3f%s", wait / numProcesses, "\n");
        output.printf ("Average Turnaround Time: %.3f%s", turnaround / numProcesses, "\n");
        output.printf ("Average Normalized Turnaround Time: %.3f%s", nTurnaround / numProcesses, "\n");
        output.printf ("Average Response Time: %.3f%s", response / numProcesses, "\n");
    }

    private static int numDigits (int num) {
        int digits = 1;

        while (num >= 10) {
            num /= 10;
            digits++;
        }

        return digits;
    }

    private static class Process implements Comparable<Process> {
        private final int pid, arrival, cpuBurst, priority;

        private int responded, finished, remainingBurst;

        public Process (int id, int arrival, int burst, int prio) {
            pid = id;
            this.arrival = arrival;
            cpuBurst = burst;
            priority = prio;
            resetProcess ();
        }

        public int getID () {
            return pid;
        }

        public int getArrivalTime () {
            return arrival;
        }

        public int getCPUBurst () {
            return cpuBurst;
        }

        public int getRemainingBurst () {
            return remainingBurst;
        }

        public boolean executeTick (int tick) {
            if (remainingBurst <= 0) {
                return false;
            }

            remainingBurst--;

            if (remainingBurst == 0) {
                finished = tick;
            }

            return true;
        }

        public int getPriority () {
            return priority;
        }

        public boolean isFinished () {
            return finished != -1;
        }

        public int getResponseTime () {
            if (responded < arrival) {
                return -1;
            }

            return responded - arrival;
        }

        public int getTurnaroundTime () {
            if (finished < arrival + cpuBurst) {
                return -1;
            }

            return finished - arrival;
        }

        public float getNormalizedTurnaroundTime () {
            if (finished < arrival + cpuBurst) {
                return -1;
            }

            return (finished - arrival) / (float) cpuBurst;
        }

        public boolean receiveResponse (int tick) {
            if (responded != -1 || tick < arrival) {
                return false;
            }

            responded = tick;
            return true;
        }

        public int getWaitTime () {
            int turnaround = getTurnaroundTime ();

            if (turnaround < cpuBurst) {
                return -1;
            }

            return turnaround - cpuBurst;
        }

        public void resetProcess () {
            responded = -1;
            finished = -1;
            remainingBurst = cpuBurst;
        }

        @Override
        public int compareTo (Process p) {
            if (arrival < p.arrival) {
                return -1;
            } else if (arrival > p.arrival || pid > p.pid) {
                return 1;
            } else if (pid < p.pid) {
                return -1;
            }

            return 0;
        }

        @Override
        public String toString () {
            return "P" + pid;
        }
    }
    
    private static class GanttChart {
        private LinkedList<Entry> entries;

        private StringBuilder processesFormatter, timeFormatter;
        private String[] processes;
        private Integer[] times;
        private boolean modified;

        private final int quantum;

        public GanttChart (int quantum) {
            entries = new LinkedList<> ();
            processesFormatter = new StringBuilder ();
            timeFormatter = new StringBuilder ();
            processes = new String[0];
            times = new Integer[0];
            modified = false;
            this.quantum = quantum;
        }

        public void addEntry (int id, int time) {
            if (!isFinished ()) {
                entries.add (new Entry (id, time));
            }
            modified = true;
        }

        public boolean isFinished () {
            if (entries.isEmpty ()) {
                return false;
            }
            return entries.peekLast ().pid == -1;
        }

        public boolean isRoundRobin () {
            return quantum > 0;
        }

        public int getQuantum () {
            return quantum;
        }

        public void printChart (PrintStream output) {
            if (modified) {
                int maxIDDigits = 2;
                int maxTimeDigits = 1;

                if (isFinished ()) {
                    processes = new String[(entries.size () * 2) - 1];
                } else {
                    processes = new String[entries.size () * 2];
                }

                times = new Integer[entries.size ()];

                processesFormatter.setLength (0);
                timeFormatter.setLength (0);

                for (Entry e: entries) {
                    maxIDDigits = Math.max (maxIDDigits, SchedulingSimulator.numDigits (e.pid) + 1);
                    maxTimeDigits = Math.max (maxTimeDigits, SchedulingSimulator.numDigits (e.time));
                }

                int i = 0;
                final int offset = Math.max (maxIDDigits + 3, maxTimeDigits + 2);
                int shortenedOffset = 0;
                int startingTimeDigits = 0;

                for (Entry e: entries) {
                    processes[i * 2] = "|";

                    if (e.pid != -1) {
                        processes[(i * 2) + 1] = "P" + e.pid;
                    }

                    times[i] = e.time;

                    if (i == 0) {
                        startingTimeDigits = SchedulingSimulator.numDigits (e.time) / 2;
                        processesFormatter.append ("%");

                        if (startingTimeDigits > 0) {
                            processesFormatter.append (startingTimeDigits);
                        }

                        processesFormatter.append ("s");
                        timeFormatter.append ("%d");

                        shortenedOffset = offset - 2 - ((maxIDDigits - processes[1].length ()) / 2);
                        processesFormatter.append ("%").append (shortenedOffset).append ("s");

                        if (startingTimeDigits % 2 == 0) {
                            startingTimeDigits--;
                        }
                    } else {
                        processesFormatter.append ("%").append (offset - shortenedOffset).append ("s");
                        timeFormatter.append ("%");
                        
                        int additionalOffset = 0;
                        int timeDigits = SchedulingSimulator.numDigits (times[i]);

                        while (startingTimeDigits < timeDigits - 2) {
                            additionalOffset++;
                            startingTimeDigits += 2;
                        }

                        timeFormatter.append (offset + additionalOffset);
                        timeFormatter.append ("d");

                        if (e.pid != -1) {
                            shortenedOffset = offset - 2 - ((maxIDDigits - processes[(i * 2) + 1].length ()) / 2);
                            processesFormatter.append ("%").append (shortenedOffset).append ("s");
                        }
                    }

                    i++;
                }
                modified = false;
            }

            output.printf (processesFormatter.toString (), (Object[]) processes);
            output.println ();
            output.printf (timeFormatter.toString (), (Object[]) times);
            output.println ();
        }

        private class Entry {
            private final int pid, time;

            public Entry (int id, int time) {
                pid = id;
                this.time = time;
            }
        }
    }
}