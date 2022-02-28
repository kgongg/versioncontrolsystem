package gitlet;

import java.io.File;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;

import static gitlet.Utils.*;


/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author Kevin Gong
 */
public class Repository {

    /**
     *
     * <p>
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    static String headCommit;
    static String activeBranch;
    static HashMap<String, byte[]> blobs = new HashMap<>();
    static StagingArea stagingArea;
    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /**
     * The staging area.
     */
    public static final File HEAD = join(GITLET_DIR, "headCommit");
    public static final File BLOBS = join(GITLET_DIR, "objects");

    public static void setupPersistence() {
        stagingArea = new StagingArea();
        blobs = new HashMap<>();
        writeObject(StagingArea.STAGING_AREA, stagingArea);
        writeObject(BLOBS, blobs);
        Commit.COMMIT_FOLDER.mkdir();
        Branch.BRANCHES_FOLDER.mkdir();
    }

    public static void initCommand() {
        if (GITLET_DIR.isDirectory()) {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
            return;
        }
        GITLET_DIR.mkdir();
        setupPersistence();
        Commit initialCommit = new Commit();
        String id = sha1(serialize(initialCommit));
        File initialCommitFile = join(Commit.COMMIT_FOLDER, id);
        File masterBranchFile = join(Branch.BRANCHES_FOLDER, "master");
        writeObject(initialCommitFile, initialCommit);
        activeBranch = "master";
        writeObject(join(GITLET_DIR, "activeBranch"), activeBranch);
        headCommit = id;
//            File headFile = join(HEAD, headCommit);
        writeObject(HEAD, headCommit);
        writeContents(masterBranchFile, headCommit);
    }

    public static void addCommand(String addedFileName) {
        stagingArea = readStagingArea();
        @SuppressWarnings("unchecked")
        HashMap<String, byte[]> blobMap = readObject(BLOBS, HashMap.class);
        File addedFile = join(CWD, addedFileName);
        if (!addedFile.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        Commit currCommit = getHead();
        byte[] blob = readContents(addedFile);
        String blobID = sha1(blob);
        if (currCommit.files != null && currCommit.files.containsKey(addedFileName)) {
            String headFileBlobID = currCommit.files.get(addedFileName);
            if (headFileBlobID.equals(blobID)) {
                if (stagingArea.staged.containsKey(addedFileName)
                        || stagingArea.removed.containsKey(addedFileName)) {
                    stagingArea.staged.remove(addedFileName);
                    stagingArea.removed.remove(addedFileName);
                }
                writeObject(StagingArea.STAGING_AREA, stagingArea);
                return;
            }
        }
        stagingArea.staged.put(addedFileName, blobID);
        blobMap.put(blobID, blob);
        writeObject(StagingArea.STAGING_AREA, stagingArea);
        writeObject(BLOBS, blobMap);
    }

    public static void commitCommand(String commitMessage, String parent2) {
        if (commitMessage.length() <= 0) {
            System.out.println("Please enter a commit message.");
            return;
        }
        activeBranch = readActive();
        headCommit = readHead();
        stagingArea = readStagingArea();
        if (stagingArea.staged.isEmpty() && stagingArea.removed.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        Commit newCommit;
        Commit lastCommit = readObject(join(Commit.COMMIT_FOLDER, headCommit), Commit.class);

        HashMap<String, String> trackedFiles = new HashMap<>();
        if (lastCommit.files == null || lastCommit.files.isEmpty()) {
            trackedFiles.putAll(stagingArea.staged);
            newCommit = new Commit(commitMessage, headCommit, trackedFiles);
        } else {
            trackedFiles.putAll(lastCommit.files);
            trackedFiles.putAll(stagingArea.staged);
            if (parent2 != null) {
                newCommit = new Commit(commitMessage, headCommit, parent2, trackedFiles);
            } else {
                newCommit = new Commit(commitMessage, headCommit, trackedFiles);
            }
        }
        for (String fileName: stagingArea.removed.keySet()) {
            newCommit.files.remove(fileName);
        }
        String id = sha1(serialize(newCommit));
        writeObject(join(Commit.COMMIT_FOLDER, id), newCommit);
        writeObject(HEAD, id);
        stagingArea.clear();
        writeObject(StagingArea.STAGING_AREA, stagingArea);
        writeContents(join(Branch.BRANCHES_FOLDER, activeBranch), id);
    }

    public static void logCommand() {
        String headCommitID = readHead();
        Commit currCommit = getHead();
        Commit pointer = currCommit;
        String parent;
        if (currCommit.parents != null) {
            parent = currCommit.parents.get(0);
        } else {
            parent = pointer.parent;
        }
        System.out.println("===");
        System.out.println("commit " + headCommitID);
        System.out.println("Date: " + pointer.timestamp);
        System.out.println(pointer.message);
        System.out.println();
        while (parent != null) {
            pointer = readObject(join(Commit.COMMIT_FOLDER, parent), Commit.class);
            System.out.println("===");
            System.out.println("commit " + parent);
            System.out.println("Date: " + pointer.timestamp);
            System.out.println(pointer.message);
            System.out.println();
            if (pointer.parents != null) {
                parent = currCommit.parents.get(0);
            } else {
                parent = pointer.parent;
            }
        }
    }

    private static StagingArea readStagingArea() {
        return readObject(StagingArea.STAGING_AREA, StagingArea.class);
    }
    private static String readHead() {
        return readObject(HEAD, String.class);
    }
    private static Commit getHead() {
        String headID = readHead();
        return readObject(join(Commit.COMMIT_FOLDER, headID), Commit.class);
    }
    private static String getLongID(String shortID) {
        for (String id: plainFilenamesIn(Commit.COMMIT_FOLDER)) {
            if (id.startsWith(shortID)) {
                return id;
            }
        }
        return null;
    }
    private static String readActive() {
        return readObject(join(GITLET_DIR, "activeBranch"), String.class);
    }
    public static void checkoutCommand1(String fileName) {
        String headCommitID = readHead();
        checkoutCommand2(fileName, headCommitID);
    }

    public static void checkoutCommand2(String fileName, String commitID) {
        if (commitID.length() < 40) {
            commitID = getLongID(commitID);
            if (commitID == null) {
                System.out.println("No commit with that id exists.");
                return;
            }
        }
        if (!join(Commit.COMMIT_FOLDER, commitID).exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit c = readObject(join(Commit.COMMIT_FOLDER, commitID), Commit.class);
        if (!c.files.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        @SuppressWarnings("unchecked")
        HashMap<String, byte[]> blobMap = readObject(BLOBS, HashMap.class);
        stagingArea = readStagingArea();
        File checkedFile = join(CWD, fileName);
        String id = c.files.get(fileName);
        byte[] original = blobMap.get(id);
        writeContents(checkedFile, original);
    }

    public static void checkoutCommand3(String branchName) {
        File checkedBranch = join(Branch.BRANCHES_FOLDER, branchName);
        Commit currCommit = getHead();
        if (!checkedBranch.exists()) {
            System.out.println("No such branch exists.");
            return;
        } else if (branchName.equals(readActive())) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        String branchCommitID = readContentsAsString(checkedBranch);
        Commit branchCommit = readObject(join(Commit.COMMIT_FOLDER, branchCommitID), Commit.class);
        for (String file : plainFilenamesIn(CWD)) {
            if (currCommit.files == null
                    || !currCommit.files.containsKey(file) && branchCommit.files != null) {
                String branchFileID = branchCommit.files.get(file);
                if (branchFileID != null
                        && !branchFileID.equals(sha1(serialize(readContents(join(CWD, file)))))) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return;
                }
            }
        }
        if (branchCommit.files != null) {
            for (String file : branchCommit.files.keySet()) {
                String fileID = branchCommit.files.get(file);
                @SuppressWarnings("unchecked")
                HashMap<String, byte[]> blobMap = readObject(BLOBS, HashMap.class);
                writeContents(join(CWD, file), blobMap.get(fileID));
            }
        }
        if (currCommit.files != null) {
            for (String file: currCommit.files.keySet()) {
                if (branchCommit.files == null || !branchCommit.files.containsKey(file)) {
                    restrictedDelete(file);
                }
            }
        }
        stagingArea = readStagingArea();
        stagingArea.clear();
        writeObject(join(GITLET_DIR, "activeBranch"), branchName);
        writeObject(HEAD, branchCommitID);
        writeObject(StagingArea.STAGING_AREA, stagingArea);
    }

    public static void removeCommand(String arg) {
        stagingArea = readStagingArea();
        Commit h = readObject(join(Commit.COMMIT_FOLDER, readHead()), Commit.class);
        if (h.files != null) {
            if (h.files.containsKey(arg)) {
                File removedFile = join(CWD, arg);
                stagingArea.removed.put(arg, h.files.get(arg));
                restrictedDelete(removedFile);
                stagingArea.staged.remove(arg);
            } else if (stagingArea.staged.containsKey(arg)) {
                stagingArea.staged.remove(arg);
            } else {
                System.out.println("No reason to remove the file.");
                return;
            }
        } else {
            if (stagingArea.staged.containsKey(arg)) {
                stagingArea.staged.remove(arg);
            } else {
                System.out.println("No reason to remove the file.");
                return;
            }
        }
        writeObject(StagingArea.STAGING_AREA, stagingArea);
    }

    public static void globalCommand() {
        for (String commit: plainFilenamesIn(Commit.COMMIT_FOLDER)) {
            Commit current = readObject(join(Commit.COMMIT_FOLDER, commit), Commit.class);
            System.out.println("===");
            System.out.println("commit " + commit);
            System.out.println("Date: " + current.timestamp);
            System.out.println(current.message);
            System.out.println();
        }
    }

    public static void findCommand(String message) {
        boolean found = false;
        for (String commit: plainFilenamesIn(Commit.COMMIT_FOLDER)) {
            Commit current = readObject(join(Commit.COMMIT_FOLDER, commit), Commit.class);
            if (current.message.equals(message)) {
                found = true;
                System.out.println(commit);
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void branchCommand(String branchName) {
        String headID = readHead();
        File newBranchFile = join(Branch.BRANCHES_FOLDER, branchName);
        if (newBranchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        writeContents(newBranchFile, headID);
    }

    public static void statusCommand() {
        stagingArea = readStagingArea();
        Commit currCommit = getHead();
        System.out.println("=== Branches ===");
        for (String branch : plainFilenamesIn(Branch.BRANCHES_FOLDER)) {
            if (branch.equals(readActive())) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String file: readStagingArea().staged.keySet()) {
            System.out.println(file);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String file: readStagingArea().removed.keySet()) {
            System.out.println(file);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        ArrayList<String> modifications = new ArrayList<>();
        if (currCommit.files != null) {
            for (String file : currCommit.files.keySet()) {
                if (!join(CWD, file).exists() && !stagingArea.removed.containsKey(file)) {
                    modifications.add(file + " (deleted)");
                    continue;
                }
                if (join(CWD, file).exists()) {
                    String cwdID = sha1(readContents(join(CWD, file)));
                    String headID = currCommit.files.get(file);
                    if (!cwdID.equals(headID) && !stagingArea.staged.containsKey(file)
                            && !stagingArea.removed.containsKey(file)) {
                        modifications.add(file + " (modified)");
                    }
                }
            }
        }
        for (String file : stagingArea.staged.keySet()) {
            if (!join(CWD, file).exists()) {
                modifications.add(file + " (deleted)");
                continue;
            } else if (!sha1(readContents(join(CWD, file))).equals(stagingArea.staged.get(file))) {
                modifications.add(file + "(modified)");
            }
        }
        Collections.sort(modifications);
        for (String file: modifications) {
            System.out.println(file);
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String file: plainFilenamesIn(CWD)) {
            if (currCommit.files == null && !stagingArea.staged.containsKey(file)
                    && !stagingArea.removed.containsKey(file)) {
                System.out.println(file);
            } else if (!stagingArea.staged.containsKey(file)
                    && !stagingArea.staged.containsKey(file)
                    && !stagingArea.removed.containsKey(file)
                    && currCommit.files != null && !currCommit.files.containsKey(file)) {
                System.out.println(file);
            }
        }
        System.out.println();
    }

    public static void rmBranchCommand(String branchName) {
        File branchFile = join(Branch.BRANCHES_FOLDER, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (readActive().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        } else {
            branchFile.delete();
        }
    }

    public static void resetCommand(String commitID) {
        Commit currCommit = getHead();
        String active = readActive();
        stagingArea = readStagingArea();
        if (commitID.length() < 40) {
            commitID = getLongID(commitID);
            if (commitID == null) {
                System.out.println("No commit with that id exists.");
                return;
            }
        }
        if (!join(Commit.COMMIT_FOLDER, commitID).exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit checkedCommit = readObject(join(Commit.COMMIT_FOLDER, commitID), Commit.class);
        for (String file: plainFilenamesIn(CWD)) {
            if (currCommit.files == null || !currCommit.files.containsKey(file)
                    && checkedCommit.files != null) {
                String checkedFileID = checkedCommit.files.get(file);
                if (checkedFileID != null && !checkedFileID.equals
                        (sha1(serialize(readContents(join(CWD, file)))))) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return;
                }
            }
        }
        for (String file: plainFilenamesIn(CWD)) {
            if (checkedCommit.files != null && currCommit.files.containsKey(file)
                    && !checkedCommit.files.containsKey(file)) {
                restrictedDelete(file);
            }
        }
        for (String file: checkedCommit.files.keySet()) {
            checkoutCommand2(file, commitID);
        }
        stagingArea.clear();
        writeObject(HEAD, commitID);
        writeContents(join(Branch.BRANCHES_FOLDER, active), commitID);
        writeObject(StagingArea.STAGING_AREA, stagingArea);
    }

    private static HashSet bfs(String commitID) {
        Queue<String> fringe = new PriorityQueue<String>();
        HashMap<String, Boolean> marked = new HashMap<>();
        HashSet<String> path = new HashSet<String>();
        fringe.add(commitID);
        marked.put(commitID, true);
        while (!fringe.isEmpty()) {
            String id = fringe.remove();
            path.add(id);
            Commit pointer = readObject(join(Commit.COMMIT_FOLDER, id), Commit.class);
            if (pointer.parents != null) {
                for (String parent : pointer.parents) {
                    if (!marked.containsKey(parent)) {
                        fringe.add(parent);
                        marked.put(parent, true);
                    }
                }
            } else {
                if (!marked.containsKey(pointer.parent) && pointer.parent != null) {
                    fringe.add(pointer.parent);
                    marked.put(pointer.parent, true);
                }
            }
        }
        return path;
    }

    private static String bfs2(String commitID, HashSet path) {
        Queue<String> fringe = new PriorityQueue<String>();
        HashMap<String, Boolean> marked = new HashMap<>();
        fringe.add(commitID);
        marked.put(commitID, true);
        while (!fringe.isEmpty()) {
            String id = fringe.remove();
            if (path.contains(id)) {
                return id;
            }
            Commit pointer = readObject(join(Commit.COMMIT_FOLDER, id), Commit.class);
            if (pointer.parents != null) {
                for (String parent : pointer.parents) {
                    if (!marked.containsKey(parent)) {
                        fringe.add(parent);
                        marked.put(parent, true);
                    }
                }
            } else {
                if (!marked.containsKey(pointer.parent) && pointer.parent != null) {
                    fringe.add(pointer.parent);
                    marked.put(pointer.parent, true);
                }
            }
        }
        return null;
    }
    public static String getSplit(String head, String branch) {
        return bfs2(branch, bfs(head));
    }

    public static void mergeCommand(String branchName) {
        stagingArea = readStagingArea();
        if (!stagingArea.staged.isEmpty() || !stagingArea.removed.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        File checkedBranch = join(Branch.BRANCHES_FOLDER, branchName);
        Commit currCommit = getHead();
        if (!checkedBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (branchName.equals(readActive())) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        String branchCommitID = readContentsAsString(checkedBranch);
        Commit branchCommit = readObject(join(Commit.COMMIT_FOLDER, branchCommitID), Commit.class);
        for (String file : plainFilenamesIn(CWD)) {
            if (currCommit.files == null
                    || !currCommit.files.containsKey(file) && branchCommit.files != null) {
                String branchFileID = branchCommit.files.get(file);
                if (branchFileID != null
                        && !branchFileID.equals(sha1(serialize(readContents(join(CWD, file)))))) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return;
                }
            }
        }
        headCommit = readHead();
        String splitID = getSplit(headCommit, branchCommitID);
        if (splitID.equals(branchCommitID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (splitID.equals(readHead())) {
            checkoutCommand3(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        Commit splitPoint = readObject(join(Commit.COMMIT_FOLDER, splitID), Commit.class);
        mergeHelper(splitPoint, currCommit, branchCommit, branchName, branchCommitID);
    }

    private static void mergeHelper(Commit splitPoint, Commit currCommit,
                                    Commit branchCommit, String branchName, String branchCommitID) {
        boolean conflict = false;
        @SuppressWarnings("unchecked")
        HashMap<String, byte[]> blobMap = readObject(BLOBS, HashMap.class);
        for (String file: splitPoint.files.keySet()) {
            String splitFileID = splitPoint.files.get(file);
            if (currCommit.files.containsKey(file) && currCommit.files.get(file).equals(splitFileID)
                    && branchCommit.files.containsKey(file)
                    && !branchCommit.files.get(file).equals(splitFileID)) {
                checkoutCommand2(file, branchCommitID);
                addCommand(file);
            } else if (currCommit.files.containsKey(file)
                    && currCommit.files.get(file).equals(splitFileID)
                    && !branchCommit.files.containsKey(file)) {
                removeCommand(file);
            } else if (currCommit.files.containsKey(file) && branchCommit.files.containsKey(file)
                    && !currCommit.files.get(file).equals(splitFileID)
                    && !branchCommit.files.get(file).equals(splitFileID)
                    && !currCommit.files.get(file).equals(branchCommit.files.get(file))) {
                byte[] headContents = blobMap.get(currCommit.files.get(file));
                byte[] branchContents = blobMap.get(branchCommit.files.get(file));
                writeContents(join(CWD, file),
                        "<<<<<<< HEAD\n", headContents, "=======\n", branchContents, ">>>>>>>\n");
                addCommand(file);
                conflict = true;
            } else if (!currCommit.files.containsKey(file)
                    && branchCommit.files.containsKey(file)
                    && !branchCommit.files.get(file).equals(splitFileID)
                    || currCommit.files.containsKey(file)
                    && !currCommit.files.get(file).equals(branchCommit.files.get(file))
                    && !branchCommit.files.containsKey(file)) {
                if (!currCommit.files.containsKey(file)) {
                    byte[] branchContents = blobMap.get(branchCommit.files.get(file));
                    writeContents(join(CWD, file),
                            "<<<<<<< HEAD\n", "=======\n", branchContents, ">>>>>>>\n");
                } else {
                    byte[] headContents = blobMap.get(currCommit.files.get(file));
                    writeContents(join(CWD, file),
                            "<<<<<<< HEAD\n", headContents, "=======\n", ">>>>>>>\n");
                }
                addCommand(file);
                conflict = true;
            }
        }
        for (String file : branchCommit.files.keySet()) {
            String branchFileID = branchCommit.files.get(file);
            if (!splitPoint.files.containsKey(file) && currCommit.files.containsKey(file)
                    && !currCommit.files.get(file).equals(branchFileID)) {
                byte[] headContents = blobMap.get(currCommit.files.get(file));
                byte[] branchContents = blobMap.get(branchCommit.files.get(file));
                writeContents(join(CWD, file),
                        "<<<<<<< HEAD\n", headContents, "=======\n", branchContents, ">>>>>>>\n");
                addCommand(file);
                conflict = true;
            } else if (!splitPoint.files.containsKey(file) && !currCommit.files.containsKey(file)) {
                checkoutCommand2(file, branchCommitID);
                addCommand(file);
            }
        }
        commitCommand("Merged " + branchName + " into " + readActive() + ".", branchCommitID);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }
}
