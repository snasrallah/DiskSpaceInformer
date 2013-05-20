package dsi;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class DiskSpaceInformer extends JPanel
        implements ActionListener {

    private static Logger log;
    private static JTextArea textArea;
    private final JButton stopButton;
    private JTree tree;
    private final JButton checkButton;
    private final JComboBox filterBox;
    private JScrollPane treeScrollPane;
    protected FindFileAndFolderSizes task;
    protected JProgressBar progressBar;

    private static String version = "Disk Space Informer v0.1r";
    static private final String newline = "\n";
    private final JComboBox drives;

    private String[] pathsToIgnore;
    private List<FindFileAndFolderSizes> tasks = new  ArrayList<FindFileAndFolderSizes>();

    public DiskSpaceInformer(File[] files, String path) {
        super(new BorderLayout());
        try {
            System.out.println("Starting Application from: " + new File(".").getCanonicalPath());
            LogManager.getLogManager().readConfiguration(DiskSpaceInformer.class.getResourceAsStream("logging.properties"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log = Logger.getLogger(DiskSpaceInformer.class.getName());
        log.log(Level.FINE, "Starting" + DiskSpaceInformer.class.getName());

        pathsToIgnore = new String[0];
        textArea = new JTextArea(30, 30);
        textArea.setName("textArea");
        textArea.setMargin(new Insets(5, 5, 5, 5));
        textArea.setEditable(true);

        try {
            pathsToIgnore = new Config("config.properties").getItems("folders.to.ignore");
            StringBuffer pathBuffer = new StringBuffer();
            for (String pathToIgnore : pathsToIgnore) {
                pathBuffer.append(String.format("path Ignored: %s\n", pathToIgnore));
            }
            textArea.setText(pathBuffer + "\n" + textArea.getText() + "\n");

        } catch (MissingResourceException e) {
            textArea.setText(new StringBuffer(String.format("Error: %s File: %s Key Missing: %s", e.getMessage(), e.getClassName(), e.getKey())) + "\n" + textArea.getText() + "\n");

        }

        textArea.append(new FindFileAndFolderSizes.Builder(new File("/")).build().checkSpaceAvailable() + "\n");
        JScrollPane logScrollPane = new JScrollPane(textArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        checkButton = new JButton("Check Space");
        checkButton.setName("Check Space");
        checkButton.addActionListener(this);
        stopButton = new JButton("Stop");
        stopButton.setName("Stop");
        stopButton.addActionListener(this);

        JComboBox<String> filter = new JComboBox<String>();
        String[] filters = { "Size", "Alpha"};
        filterBox = new JComboBox(filters);
        filterBox.setName("filterBox");
        filterBox.setSelectedIndex(0);
        filterBox.setToolTipText("Set filter");
        filterBox.addActionListener(this);

        drives = new JComboBox(files);
        if (path != "") drives.addItem(path);
        drives.setSelectedItem(path);
        drives.addActionListener(this);

        JPanel controlPanel = new JPanel();
        controlPanel.add(drives);
        controlPanel.add(checkButton);
        controlPanel.add(filterBox);
        controlPanel.add(stopButton);

        String root = drives.getSelectedItem().toString();
        tree = new JTree();
        tree.setName("tree");
        tree.setModel(new FileSystemTreeModel(root));
        tree.addMouseListener(new LeftClickMouseListener());

        treeScrollPane = new JScrollPane(tree);
        progressBar = new JProgressBar();
        Dimension prefSize = progressBar.getPreferredSize();
        prefSize.width = 30;
        progressBar.setPreferredSize(prefSize);
        JPanel progressPanel = new JPanel();
        progressPanel.add(progressBar);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                treeScrollPane, logScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(250);

        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(progressPanel, BorderLayout.SOUTH);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == drives) { //open
            tree = new JTree();
            tree.setModel(new FileSystemTreeModel(new TreeFile(drives.getSelectedItem().toString())));
            tree.addMouseListener(new LeftClickMouseListener());
            treeScrollPane.setViewportView(tree);
        } else if (e.getSource() == checkButton) {
            // check if tree is selected
            if (tree.isSelectionEmpty()){
                task = new FindFileAndFolderSizes.Builder(new TreeFile(drives.getSelectedItem().toString()))
                    .pathstoIgnore(pathsToIgnore)
                    .textArea(textArea)
                    .filter(filterBox.getSelectedItem().toString())
                    .progressBar(progressBar).build();
                tasks.add(task);
            }
            else{
                showSpaceUsedByFolder();
            }
            task.execute();
        } else if (e.getSource() == stopButton) {
            for (FindFileAndFolderSizes t : tasks){
                if(!t.isDone()){
                    t.cancel(true);
                }
            }
            progressBar.setString("Task Cancelled, showing partial information");
        }
    }


    private void showSpaceUsedByFolder() {
        TreePath[] selectionPaths = tree.getSelectionPaths();
        for (TreePath path : selectionPaths) {
            File lastPathComponent = (File) path.getLastPathComponent();
            task = new FindFileAndFolderSizes.Builder(lastPathComponent)
                    .pathstoIgnore(pathsToIgnore)
                    .filter(filterBox.getSelectedItem().toString())
                    .textArea(textArea)
                    .progressBar(progressBar).build();
            tasks.add(task);
            task.execute();
        }
    }

    protected class LeftClickMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (e.getClickCount() == 2) {
                    int rowLocation = tree.getRowForLocation(e.getX(), e.getY());
                    File lastPathComponent = (File) tree.getPathForRow(rowLocation).getLastPathComponent();
                    if (lastPathComponent.isFile()) {
                        task = new FindFileAndFolderSizes.Builder(lastPathComponent)
                                .pathstoIgnore(pathsToIgnore)
                                .textArea(textArea)
                                .progressBar(progressBar).build();
                        task.execute();
                    }
                } else if (e.getClickCount() == 1) {
                    //showSpaceUsedByFolder();
                }
            }
        }
    };

    private static void setupAndShowUI(File[] files, String path) {
        JFrame frame = new JFrame(version);
        frame.setName("DiskSpaceInformer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new DiskSpaceInformer(files, path));
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(final String[] args) throws IOException, InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                if (args.length == 0) {
                    setupAndShowUI(File.listRoots(), System.getProperty("user.home"));
                } else {
                    setupAndShowUI(new File[]{new File("")}, args[0]);
                }
            }
        });
    }

}