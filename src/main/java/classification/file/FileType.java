package classification.file;

/**
 * Created by anie on 3/4/2015.
 */
public enum FileType {
    TabFile("\t"), CSVFile(null);

    private String sep;

    private FileType(String sep) {
        this.sep = sep;
    }

    public String getSep() {
        return sep;
    }
}
