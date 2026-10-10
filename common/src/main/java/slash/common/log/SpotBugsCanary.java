package slash.common.log;

// TEMPORARY canary: proves the SpotBugs job still fails the build. Never merge.
public class SpotBugsCanary {
    public static int length() {
        String value = null;
        return value.length();
    }
}
