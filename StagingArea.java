package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;

import static gitlet.Utils.join;

public class StagingArea implements Serializable {
    TreeMap<String, String> staged;
    TreeMap<String, String> removed;
    static final File STAGING_AREA = join(".gitlet", "staged");

    public StagingArea() {
        // Hashmap stores files staged for addition (key: file name, value: blob sha1 id)
        staged = new TreeMap<>();
        // Stores files that are staged for removal
        removed = new TreeMap<>();
    }

    public void clear() {
        staged = new TreeMap<>();
        removed = new TreeMap<>();
    }
}
