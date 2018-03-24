package de.stiffi.admin.foldersync;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class PathUtilsTest {

    @Test
    public void testGetParentDir() {
        String file = "/a/b/c/file.txt";
        String parentDir = PathUtils.getParentDir(file);
        Assert.assertEquals("/a/b/c", parentDir);


        file = "file.txt";
        parentDir = PathUtils.getParentDir(file);
        Assert.assertNull(parentDir);


        file = "/file.txt";
        parentDir = PathUtils.getParentDir(file);
        Assert.assertEquals("/", parentDir);


        file = "/";
        parentDir = PathUtils.getParentDir(file);
        Assert.assertNull(parentDir);
    }
}