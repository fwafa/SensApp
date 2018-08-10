package student.fh.sensorapplication.Model;

import java.io.File;

public class ModelFile {

    private File file;
    private String fileName;
    private boolean isSelected;

    public ModelFile(File file, String fileName, boolean isSelected) {
        this.file = file;
        this.fileName = fileName;
        this.isSelected = isSelected;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
