package de.stiffi.admin.foldersync;

public class PathUtils {

    public static String getParentDir(String pathToFile) {
        pathToFile = pathToFile.replace("\\", "/");
        if (!pathToFile.contains("/")) {
            //No Parent Dir!
            return null;
        }else if (pathToFile.equals("/")) {
            return null;
        }
        String parentDir = pathToFile.substring(0, pathToFile.lastIndexOf("/"));

        if (parentDir.equals("")) {
            return "/";
        }

        return parentDir;
    }
}
