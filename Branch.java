package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.*;

public class Branch implements Serializable {
    static final File BRANCHES_FOLDER = join(".gitlet", "branches");

}
