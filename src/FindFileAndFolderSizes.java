import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class FindFileAndFolderSizes extends SwingWorker<Void, Void> {

    private File file;
    private JTextArea log;
    private JProgressBar progressBar;
    private ProgressMonitor progressMonitor;
    private boolean summary = false;

    FindFileAndFolderSizes(File file, JTextArea log, JProgressBar progressBar, ProgressMonitor progressMonitor, boolean summary) {
        this(file, log, progressBar, progressMonitor);
        this.summary = summary;
    }

    FindFileAndFolderSizes(File file, JTextArea log, JProgressBar progressBar, ProgressMonitor progressMonitor) {
        this.file = file;
        this.log = log;
        this.progressBar = progressBar;
        this.progressMonitor = progressMonitor;
    }

    @Override
    public Void doInBackground() {
        if (file.isFile()) {
            Utils.prettyPrint(file, log);
            return null;
        }

        Map<String, Long> listing = new HashMap<String, Long>();
        progressBar.setString("Determining files to scan");
        progressBar.setStringPainted(true);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        int count = file.list().length;
        float increment = 0.0f;
        if (count != 0) {
            increment = 100.0f / count;
        }
        float progress = 0.0f;
        setProgress(0);
        long totalSize = 0L;

        for (File file : this.file.listFiles(new IgnoreFilter())) {
            if (file.isFile()) {
                progress += increment;
                //log.append("progress: " + progress);
                setProgress(Math.min((int) Math.round(progress), 100));
                long size = file.length();
                totalSize += size;
                listing.put(file.getName(), size);
            } else {
                progress += increment;
                //log.append("progress: " + progress);
                setProgress(Math.min((int) Math.round(progress), 100));
                CalculateFileSizeVisitor visitor = new CalculateFileSizeVisitor();
                Path root = Paths.get(String.valueOf(file));
                try {
                    Files.walkFileTree(root, visitor);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                totalSize += visitor.getSizeSum();
                listing.put(file.getName(), visitor.getSizeSum());
            }
        }

        progressBar.setString("Sorting Listing...");
        ValueComparator vc = new ValueComparator(listing);
        Map<String, Long> sortedMap = new TreeMap<String, Long>(vc);
        sortedMap.putAll(listing);
        progressBar.setIndeterminate(false);
        if (summary) {
            int lastDoc = log.getDocument().getLength();
            Utils.prettyPrint(file, totalSize, log);
            log.setCaretPosition(lastDoc);
        } else {
            int lastDoc = log.getDocument().getLength();
            Utils.prettyPrint(file, totalSize, sortedMap, log);
            log.setCaretPosition(lastDoc);
        }
        return null;
    }

    @Override
    public void done() {
        Toolkit.getDefaultToolkit().beep();
        progressMonitor.close();
    }

}
