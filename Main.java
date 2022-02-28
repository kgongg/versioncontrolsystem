package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        if (!firstArg.equals("init") && !Repository.GITLET_DIR.isDirectory()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        switch (firstArg) {
            case "init":
                Repository.initCommand();
                break;
            case "add":
                // checks if no file is inputted to add
                if (args.length == 1) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.addCommand(args[1]);
                break;
            case "commit":
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                    return;
                }
                Repository.commitCommand(args[1], null);
                break;
            case "log":
                Repository.logCommand();
                break;
            case "checkout":
                if (args.length == 1) {
                    System.out.println("Incorrect operands.");
                    return;
                } else if (args.length == 2) {
                    Repository.checkoutCommand3(args[1]);
                } else if (args[1].equals("--") && args[2] != null) {
                    Repository.checkoutCommand1(args[2]);
                } else if (args[1] != null && args[2].equals("--") && args[3] != null) {
                    Repository.checkoutCommand2(args[3], args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "rm":
                if (args.length == 1) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.removeCommand(args[1]);
                break;
            case "global-log":
                Repository.globalCommand();
                break;
            case "find":
                Repository.findCommand(args[1]);
                break;
            case "branch":
                Repository.branchCommand(args[1]);
                break;
            case "status":
                Repository.statusCommand();
                break;
            case "rm-branch":
                Repository.rmBranchCommand(args[1]);
                break;
            case "reset":
                Repository.resetCommand(args[1]);
                break;
            case "merge":
                Repository.mergeCommand(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }
}
