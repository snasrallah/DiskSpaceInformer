import javax.swing.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Map;

public final class Utils {

    static private final String newline = "\n";
    static private final String tab = "\t";
    static private final String doubleSpace = "  ";
    static private final String space = " ";


    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String prettyPrint(String root, long totalSpace, long usedSpace, long freeSpace) {
        StringBuffer sb = new StringBuffer();
        long freeSpacePercent = Math.round(Float.valueOf(freeSpace) / Float.valueOf(totalSpace) * 100.0);
        String title = "Checked: [ " + root + " ]  " + "Free:" + " [ " + freeSpacePercent + "% ]";
        String underline = String.format(String.format("%%0%dd", title.length()), 0).replace("0", "=");

        sb.append(underline + newline + title + "\n" + underline);
        sb.append(String.format("\nTotal Space is: [%s]\nUsed space is: [%s] \nFree space is: [%s]\n\n",
                readableFileSize(totalSpace),
                readableFileSize(usedSpace),
                readableFileSize(freeSpace))
        );
        return sb.toString();
    }

    public static String prettyPrint(File file, long total, Map<String, Long> sortedFileFolderSizes, boolean debug, String extraInfo) {
        StringBuffer sb = new StringBuffer();
        String status = debug || extraInfo.length() == 0 ? "" : "[Error(s) turn on debug checkbox]";
        String title = "[" + readableFileSize(total) + "] ==> " +  file.getAbsolutePath() + space + status;
        String underline = String.format(String.format("%%0%dd", title.length()), 0).replace("0", "=");
        sb.append(underline + newline);
        sb.append(title + newline);
        sb.append(underline + newline);
        if(debug){
            sb.append(newline);
            sb.append(extraInfo);
            sb.append(newline);
        }
        for (Map.Entry<String, Long> entry : sortedFileFolderSizes.entrySet()) {
            sb.append("[ " + readableFileSize(entry.getValue()) + " ]");
            sb.append(" --> " + entry.getKey() + "\n");
        }
        sb.append(newline);
        return sb.toString();
    }

    public static String prettyPrint(File file, long total) {
        return String.format("%s: [%s]\n", file.getName(), readableFileSize(total));
    }

    public static String prettyPrint(File file) {
        return String.format("%s: [%s]\n", file.getName(), readableFileSize(file.length()));
    }

    public static String printInstructions() {
        StringBuilder builder = new StringBuilder();
        builder.append("- Space usage: right click tree item(s) & Check Space" + newline);
        builder.append("- Alternative: select tree item(s) & click [Check Space]" + newline + newline);
        return builder.toString();
    }


}

