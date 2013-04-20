import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private static String version = "Directory Sizer v0.1c";
    private final JButton checkButton;
    JButton openButton, clearButton;
    JTextArea log;
    JFileChooser fileChooser;
    JProgressBar jProgressBar;
    JTree tree;

    private FindFileAndFolderSizes task;
    private ProgressMonitor progressMonitor;
    //private static float progress = 0.0f;

    private long folderSize = 0;


    class FindFileAndFolderSizes extends SwingWorker<Void, Void> {

        private File dir;

        FindFileAndFolderSizes(File dir) {
            this.dir = dir;

        }

        @Override
        public Void doInBackground() {
            Map<String, Long> listing = new HashMap<String, Long>();
            jProgressBar.setString("Determining files to scan");
            jProgressBar.setStringPainted(true);
            jProgressBar.setVisible(true);
            jProgressBar.setIndeterminate(true);

            int count = dir.list().length;
            float increment = 0.0f;
            if (count != 0) {
                increment = 100.0f / count;
            }
            float progress = 0.0f;
            setProgress(0);
            long totalSize = 0L;

            for (File file : dir.listFiles(new IgnoreFilter())) {
                if (file.isFile()) {
                    progress += increment;
                    //log.append("progress: " + progress);
                    setProgress(Math.min((int) Math.round(progress), 100));

                    long size = file.length();
                    totalSize += size;
                    listing.put(file.getName(), size);
                } else {
                    folderSize = 0;
                    progress += increment;
                    //log.append("progress: " + progress);
                    setProgress(Math.min((int) Math.round(progress), 100));
                    getFolderSize(file);
                    totalSize += folderSize;
                    listing.put(file.getName(), folderSize);
                }
            }

            jProgressBar.setString("Sorting Listing...");
            ValueComparator vc = new ValueComparator(listing);
            Map<String, Long> sortedMap = new TreeMap<String, Long>(vc);
            sortedMap.putAll(listing);
            jProgressBar.setIndeterminate(false);
            PrettyPrint(dir, totalSize, sortedMap);

            return null;
        }

        private long getFolderSize(File file) {
            if (file.isFile()) {
                folderSize += file.length();
            } else {
                //log.append("\nProcessing size: " + file);
                String[] contents = file.list();
                if (contents != null) {  //take care of empty folders
                    for (File f : file.listFiles(new IgnoreFilter())) {
                        jProgressBar.setString(file.toString());
                        getFolderSize(f);
                    }
                }
            }
            return folderSize;
        }


        @Override
        public void done() {
            Toolkit.getDefaultToolkit().beep();
            progressMonitor.close();
        }
    }

    /**
     * The methods in this class allow the JTree component to traverse
     * the file system tree and display the files and directories.
     */
    class FileTreeModel implements TreeModel {
        // We specify the root directory when we create the model.
        protected File root;

        public FileTreeModel(File root) {
            this.root = root;
        }

        // The model knows how to return the root object of the tree
        public Object getRoot() {
            return root;
        }

        // Tell JTree whether an object in the tree is a leaf
        public boolean isLeaf(Object node) {
            return ((File) node).isFile();
        }

        // Tell JTree how many children a node has
        public int getChildCount(Object parent) {
            String[] children = ((File) parent).list();
            if (children == null) return 0;
            return children.length;
        }

        // Fetch any numbered child of a node for the JTree.
        // Our model returns File objects for all nodes in the tree.  The
        // JTree displays these by calling the File.toString() method.
        public Object getChild(Object parent, int index) {
            String[] children = ((File) parent).list();
            if ((children == null) || (index >= children.length)) return null;
            return new File((File) parent, children[index]);
        }

        // Figure out a child's position in its parent node.
        public int getIndexOfChild(Object parent, Object child) {
            String[] children = ((File) parent).list();
            if (children == null) return -1;
            String childname = ((File) child).getName();
            for (int i = 0; i < children.length; i++) {
                if (childname.equals(children[i])) return i;
            }
            return -1;
        }

        // This method is invoked by the JTree only for editable trees.
        // This TreeModel does not allow editing, so we do not implement
        // this method.  The JTree editable property is false by default.
        public void valueForPathChanged(TreePath path, Object newvalue) {
        }

        // Since this is not an editable tree model, we never fire any events,
        // so we don't actually have to keep track of interested listeners
        public void addTreeModelListener(TreeModelListener l) {
        }

        public void removeTreeModelListener(TreeModelListener l) {
        }
    }


    class DirectoryFilter implements FileFilter {
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;  //To change body of implemented methods use File | Settings | File Templates.
            }
            return false;
        }
    }

    class RightClickMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {

            JTree tree;

            if (SwingUtilities.isRightMouseButton(e)) {
                tree = (JTree) e.getSource();
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                Rectangle pathBounds = tree.getUI().getPathBounds(tree, path);
                if (pathBounds != null && pathBounds.contains(e.getX(), e.getY())) {
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem jMenuItem = new JMenuItem("check space");
                    jMenuItem.addActionListener(new MenuActionListener());
                    menu.add(jMenuItem);
                    menu.show(tree, pathBounds.x, pathBounds.y + pathBounds.height);


                }
            }
        }
    }

    ;

    class MenuActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object selection = tree.getLastSelectedPathComponent();
            if (selection.equals("listings:")) return;

            progressMonitor = new ProgressMonitor(DiskSpaceInformer.this,
                    "Getting Folder sizes",
                    "", 0, 100);
            progressMonitor.setProgress(0);
            task = new FindFileAndFolderSizes(new File(selection.toString()));
            task.addPropertyChangeListener(DiskSpaceInformer.this);
            task.execute();
            System.out.println("Selected: " + e.getActionCommand());

        }
    }

    ;

    public static final class OsUtils{
        private static String OS = null;
        public static String getOsName()
        {
            if(OS == null) { OS = System.getProperty("os.name"); }
            return OS;
        }
        public static boolean isWindows()
        {
            return getOsName().startsWith("Windows");
        }

        public static boolean isUnix(){
               return false;
        } // and so on
    }

    class DoubleClickMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                JTree tree = (JTree) e.getSource();
                Object selection = tree.getLastSelectedPathComponent();

                if (selection.equals("listings:")) return;

                progressMonitor = new ProgressMonitor(DiskSpaceInformer.this,
                        "Getting Folder sizes",
                        "", 0, 100);
                progressMonitor.setProgress(0);
                task = new FindFileAndFolderSizes(new File(selection.toString()));
                task.addPropertyChangeListener(DiskSpaceInformer.this);
                task.execute();
            }

            if (e.getClickCount() == 2) {
                JTree tree = (JTree) e.getSource();
                Object selection = tree.getLastSelectedPathComponent();

                if (selection.equals("listings:")) return;

                progressMonitor = new ProgressMonitor(DiskSpaceInformer.this,
                        "Getting Folder sizes",
                        "", 0, 100);
                progressMonitor.setProgress(0);
                task = new FindFileAndFolderSizes(new File(selection.toString()));
                task.addPropertyChangeListener(DiskSpaceInformer.this);
                task.execute();
            }
        }
    }

    ;

    public DiskSpaceInformer() {
        super(new BorderLayout());
        JFrame f = new JFrame();
        log = new JTextArea(30, 35);
        log.setMargin(new Insets(5, 5, 5, 5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);


//Create a file chooser
        String os = System.getProperty("os.name");
        fileChooser = new JFileChooser();

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        openButton = new JButton("Choose Folder...");
        openButton.addActionListener(this);

        checkButton = new JButton("Check Space...");
        checkButton.addActionListener(this);

        clearButton = new JButton("Clear Log...");
        clearButton.addActionListener(this);

//flow layout
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(openButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(checkButton);

        jProgressBar = new JProgressBar(0, 100);
        JPanel progressPanel = new JPanel();
        progressPanel.setSize(100, 100);
        progressPanel.setMinimumSize(new Dimension(200, 200));
        progressPanel.add(jProgressBar);


// Create a TreeModel object to represent our tree of files
//        File root = new File(System.getProperty("user.home"));
        File root;
        if (OsUtils.isWindows()){
            root = new File("c:\\");
        }else{
            root = new File("/");
        }
        FileTreeModel model = new FileTreeModel(root);

// Create a JTree and tell it to display our model

        tree = new JTree();
        tree.setModel(model);
        tree.addMouseListener(new RightClickMouseListener());

// The JTree can get big, so allow it to scroll
        JScrollPane treeScrollPane = new JScrollPane(tree);
        treeScrollPane.setSize(100, 100);

//Add bits to the panel.
        add(buttonPanel, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.EAST);
        add(treeScrollPane, BorderLayout.WEST);
        add(progressPanel, BorderLayout.SOUTH);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openButton) { //open
            int returnVal = fileChooser.showOpenDialog(DiskSpaceInformer.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File dir = fileChooser.getSelectedFile();

                progressMonitor = new ProgressMonitor(DiskSpaceInformer.this,
                        "Getting Folder sizes",
                        "", 0, 100);
                progressMonitor.setProgress(0);
                task = new FindFileAndFolderSizes(dir);
                task.addPropertyChangeListener(this);
                task.execute();
            } else {
                log.append("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());

        } else if (e.getSource() == clearButton) {
            log.setText("");  //clear
        } else if (e.getSource() == checkButton) {
            log.append(checkSpaceAvailable());  //check
            log.setCaretPosition(log.getDocument().getLength());
        }
    }

    private String checkSpaceAvailable() {
        StringBuffer sb = new StringBuffer();
        File[] roots = File.listRoots();
        for (File root : roots) {
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            String title = "Checking: [ " + root + " ]";
            String underline = String.format(String.format("%%0%dd", title.length()), 0).replace("0", "=");

            sb.append(underline + "\n" + title + "\n" + underline);
            sb.append(String.format("\nTotal Space is: [%s]\nUsed space is: [%s] \nFree space is: [%s] \n\n",
                    readableFileSize(totalSpace),
                    readableFileSize(usedSpace),
                    readableFileSize(freeSpace))
            );
        }
        return sb.toString();
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
                    jProgressBar.setString("Task cancelled");
                    log.append("Task canceled.\n");
                    task.cancel(true);
                } else {
                    jProgressBar.setString("Task completed");
                    log.append("Task completed.\n");
                }
            }
        }
    }

    private void PrettyPrint(File file, long total, Map<String, Long> sortedFileFolderSizes) {
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

    private static void setupAndShowUI() {
        JFrame frame = new JFrame(version);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//Add content to the window.
        frame.add(new DiskSpaceInformer());

//Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                setupAndShowUI();
            }
        });
    }
}
