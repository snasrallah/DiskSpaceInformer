/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.text.DecimalFormat;
import java.util.*;

/*
 * DiskSpaceInformer.java
 */
public class DiskSpaceInformer extends JPanel
        implements ActionListener, PropertyChangeListener {
    static private final String newline = "\n";
    private static String version;
    JButton openButton, clearButton;
    JTextArea log;
    JFileChooser fc;
    private FindFileAndFolderSizes task;

    private ProgressMonitor progressMonitor;
    private long filesFolderCount = 0;
    private static float increment = 0.0f;
    private static float progress = 0.0f;

    class FindFileAndFolderSizes extends SwingWorker<Void, Void> {

        private File dir;

        FindFileAndFolderSizes(File dir) {
            this.dir = dir;
        }

        @Override
        public Void doInBackground() {

            File[] files = null;
            Map<String, Long> dirListing = new HashMap<String, Long>();
            try {
                files = dir.listFiles();
            } catch (SecurityException se) {
                throw new SecurityException("Security problem: " + se);
            }
            if (null == files) {
                log.append("Unable to retrieve folder information check permissions" + newline);
                dirListing = new HashMap<String, Long>();
            }

            for (File file : files) {
                //System.out.println("Processing: " + file);
                boolean onlyCount = true;
                DiskUsage diskUsage = new DiskUsage(onlyCount);
                diskUsage.accept(file);
                filesFolderCount += diskUsage.getCount();
            }
            //System.out.println("count: " + filesFolderCount);

            if (filesFolderCount != 0) {
                increment = 100.0f / filesFolderCount;
            }

            setProgress(0);
            //System.out.println("increment will be: " + increment);
            for (File file : files) {
                //System.out.println("Processing: " + file);
                DiskUsage diskUsage = new DiskUsage();
                diskUsage.accept(file);
                long size = diskUsage.getSize();
                dirListing.put(file.getName(), size);
            }
            ValueComparator bvc = new ValueComparator(dirListing);
            Map<String, Long> sortedMap = new TreeMap<String, Long>(bvc);
            sortedMap.putAll(dirListing);
            PrettyPrint(dir, sortedMap);
            return null;
        }

        @Override
        public void done() {
            Toolkit.getDefaultToolkit().beep();
            progressMonitor.close();
        }

        class DiskUsage implements FileFilter {

            public DiskUsage() {
            }

            ;

            public DiskUsage(boolean count) {
                this.count = count;
            }

            ;
            private long fileCount = 0;
            private long size = 0;
            private boolean count = false;

            public boolean accept(File file) {
                if (file.isFile()) {
                    if (count) {
                        fileCount++;
                    } else {
                        size += file.length();
                        progress += increment;
                        //System.out.println("progress: " + progress);
                        setProgress(Math.min((int) Math.round(progress), 100));
                    }
                } else if (file.isDirectory()) {
                    file.listFiles(this);
                }
                return false;
            }

            public long getSize() {
                return size;
            }

            public long getCount() {
                return fileCount;
            }
        }

    }


    public DiskSpaceInformer() {
        super(new BorderLayout());

        JFrame f = new JFrame();
        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(30, 30);
        log.setMargin(new Insets(5, 5, 5, 5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        //Create a file chooser
        fc = new JFileChooser();

        //Uncomment one of the following lines to try a different
        //file selection mode.  The first allows just directories
        //to be selected (and, at least in the Java look and feel,
        //shown).  The second allows both files and directories
        //to be selected.  If you leave these lines commented out,
        //then the default mode (FILES_ONLY) will be used.
        //
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        //Create the open button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        //  createImageIcon("images/Open16.gif")
        openButton = new JButton("Choose Folder...");
        openButton.addActionListener(this);

        //Create the save button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        clearButton = new JButton("Clear Log...");

        clearButton.addActionListener(this);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButton);
        buttonPanel.add(clearButton);

        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {

        //Handle open button action.
        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(DiskSpaceInformer.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File dir = fc.getSelectedFile();

                progressMonitor = new ProgressMonitor(DiskSpaceInformer.this,
                        "Getting Folder sizes",
                        "", 0, 100);
                progressMonitor.setProgress(0);
                task = new FindFileAndFolderSizes(dir);
                task.addPropertyChangeListener(this);
                //openButton.setEnabled(false);
                task.execute();
            } else {
                log.append("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());

            //Handle clear button action.
        } else if (e.getSource() == clearButton) {
            log.setText("");  //reset
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            progressMonitor.setProgress(progress);
            String message =
                    String.format("Completed %d%%.\n", progress);
            progressMonitor.setNote(message);
            //log.append(message); // see it print increments
            if (progressMonitor.isCanceled() || task.isDone()) {
                Toolkit.getDefaultToolkit().beep();
                if (progressMonitor.isCanceled()) {
                    log.append("Task canceled.\n");
                    task.cancel(true);
                } else {
                    log.append("Task completed.\n");
                }
            }
        }
    }

    private void PrettyPrint(File file, Map<String, Long> sortedFileFolderSizes) {
        Long total = 0L;
        for (Long value : sortedFileFolderSizes.values()) {
            total = total + value; // Can also be done by total += value;
        }
        String title = file.getName() + ": [" + readableFileSize(total) + "]";
        String underline = String.format(String.format("%%0%dd", title.length()), 0).replace("0", "=");
        log.append(underline + newline);
        log.append(title + newline);
        log.append(underline + newline);
        for (Map.Entry<String, Long> entry : sortedFileFolderSizes.entrySet()) {
            log.append("[ " + readableFileSize(entry.getValue()) + " ]");
            log.append(" --> " + entry.getKey() + "\n");
        }
        log.append(newline + newline);
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = DiskSpaceInformer.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        version = "Directory Sizer v0.1b";
        JFrame frame = new JFrame(version);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        frame.add(new DiskSpaceInformer());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }
}


class ValueComparator implements Comparator<String> {

    Map<String, Long> base;

    public ValueComparator(Map<String, Long> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }

}