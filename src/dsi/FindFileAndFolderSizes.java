package dsi;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

class FindFileAndFolderSizes extends SwingWorker<Void, Void> {

    private String[] pathsToIgnore;
    private JTable table;
    private JProgressBar progressBar;
    private File file;
    private static Logger log = Logger.getLogger(FindFileAndFolderSizes.class.getName());

    public static class Builder{
        //req params
        private File file;
        //opt params
        private String[] pathsToIgnore =  new String[0];
        private JTable table = new JTable();
        private JProgressBar progressBar = new JProgressBar();


        public Builder(File file){
              this.file = file;
        }

        public Builder pathstoIgnore(String[] val)
            {pathsToIgnore = val; return this;}
        public Builder table(JTable val)
            {table = val; return this;}
        public Builder progressBar(JProgressBar val)
            {progressBar = val; return this;}


        public FindFileAndFolderSizes build(){
            return new FindFileAndFolderSizes(this);
        }
    }

    private FindFileAndFolderSizes(Builder builder){
        file = builder.file;
        pathsToIgnore = builder.pathsToIgnore;
        table = builder.table;
        progressBar = builder.progressBar;
    }

    @Override
    public Void doInBackground() throws Exception {
        setProgress(0);
        Object[][] data = new Object[1][2];
        if (file.isFile()) {
            data[0][0] = file.getName();
            data[0][1]= HumanReadableFileSize.readableFileSize(file.length()) ;
            TableModel model = new TableModel(new String[]{"Name", "Size"}, data);
            table.setModel(model);
            model.fireTableDataChanged();
            return null;
        }

        progressBar.setString("Processing Selection...");
        long startTime = System.currentTimeMillis();
        Path root = Paths.get(String.valueOf(file.getPath()));

        List<Path> foldersToIgnore = new ArrayList<Path>();
        for (String path : pathsToIgnore) {
            foldersToIgnore.add(new File(path).toPath());
        }
            FileSystemVisitor visitor = new FileSystemVisitor(root, foldersToIgnore, progressBar);
        try {
            Files.walkFileTree(root, visitor);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, HumanReadableFileSize> foldersSizes = visitor.getFoldersSizes();
        if (visitor.getFoldersSizes().size() == 0){
            data[0][0] = file.getName();
            data[0][1]= HumanReadableFileSize.readableFileSize(0) ;
            TableModel model = new TableModel(new String[]{"Name", "Size"}, data);
            table.setModel(model);
            model.fireTableDataChanged();
            return null;
        }

        progressBar.setString("Sorting Listing...");
        data = new Object[foldersSizes.size()][2];
        int row = 0;
        for (Map.Entry<String, HumanReadableFileSize> entry : foldersSizes.entrySet()){
            data[row][0] = entry.getKey();
            data[row][1] = entry.getValue();
            row++;
        }
        TableModel model = new TableModel(new String[]{"Name", "Size (Total = "  +  HumanReadableFileSize.readableFileSize(visitor.getGrandTotal()) + ")"}, data);
        table.setModel(model);
        model.fireTableDataChanged();

        long endTime = System.currentTimeMillis();
        log.info("The scan for [" + file.getAbsolutePath() + "] took " + (endTime - startTime) + " milliseconds");
        return null;
    }

    public static String[][] checkSpaceAvailable() {
        String[][] info = new String[(File.listRoots().length*4)][2];
        File[] roots = File.listRoots();
        int row = 0;
        for (File root : roots) {
            long totalSpace =  root.getTotalSpace();
            long freeSpace =  root.getFreeSpace();
            info[row][0] = String.format("Total Space on [ %s ]",root.getAbsolutePath()) ;
            info[row][1] = HumanReadableFileSize.readableFileSize(totalSpace);
            row++;
            info[row][0] = String.format("Used Space on [ %s ]",root.getAbsolutePath()) ;
            info[row][1] = HumanReadableFileSize.readableFileSize(totalSpace - freeSpace);
            row++;
            info[row][0] = String.format("Free Space on [ %s ]", root.getAbsolutePath()) ;
            info[row][1] = HumanReadableFileSize.readableFileSize(freeSpace);
            row++;
            row++;
        }
        return info;
    }


    @Override
    public void done() {
        progressBar.setString("Task Complete...");
    }

}
